package com.pintomau.temporalspring.sales.features

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.pintomau.temporalspring.sales.clients.MarketingClient
import com.pintomau.temporalspring.sales.clients.MarketingRemoveLineItem
import com.pintomau.temporalspring.sales.clients.WarehouseClient
import com.pintomau.temporalspring.sales.clients.WarehouseRemoveLineItem
import com.pintomau.temporalspring.sales.core.*
import com.pintomau.temporalspring.sales.core.SalesCartRepository
import com.pintomau.temporalspring.sales.infra.objectMessageBuilder
import io.temporal.activity.ActivityInterface
import io.temporal.activity.LocalActivityOptions
import io.temporal.client.WorkflowClient
import io.temporal.client.newWorkflowStub
import io.temporal.common.RetryOptions
import io.temporal.spring.boot.ActivityImpl
import io.temporal.spring.boot.WorkflowImpl
import io.temporal.workflow.*
import java.time.Duration
import java.util.*
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.MessagePropertiesBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// region Port
@RestController
class AddLineItemController(
  private val addLineItemWorkflowStarter: AddLineItemWorkflowStarter,
  private val addLineItemHandler: AddLineItemHandler,
) {

  @PostMapping("{cartId}/add-line-item")
  fun addLineItem(
    @PathVariable("cartId") cartId: UUID,
    @Validated @RequestBody addLineItem: AddLineItem,
  ): WorkflowAndRun {
    return addLineItemWorkflowStarter.handle(
      AddLineItemWorkflowCommand(
        cartId = cartId,
        version = addLineItem.version,
        productId = addLineItem.productId,
        requestId = addLineItem.requestId,
      ),
    )
  }
}

data class AddLineItem(
  val version: Int,
  val productId: UUID,
  val requestId: String,
)
// endregion

// region Workflow

@WorkflowInterface
fun interface AddLineItemWorkflow {

  @WorkflowMethod fun addLineItem(command: AddLineItemWorkflowCommand)
}

@WorkflowImpl(taskQueues = [SALES_CART_PROCESSING_QUEUE])
class AddLineItemWorkflowImpl(
  private val addLineItemActivities: AddLineItemActivities =
    Workflow.newLocalActivityStub(
      AddLineItemActivities::class.java,
      LocalActivityOptions {
        setRetryOptions(
          RetryOptions {
            setInitialInterval(Duration.ofSeconds(1))
            setMaximumInterval(Duration.ofSeconds(100))
            setBackoffCoefficient(2.0)
            setMaximumAttempts(3)
          },
        )
        setStartToCloseTimeout(Duration.ofSeconds(2))
      },
    ),
) : AddLineItemWorkflow {

  override fun addLineItem(command: AddLineItemWorkflowCommand) {
    val saga = Saga(Saga.Options.Builder().setParallelCompensation(false).build())

    try {
      val lineItemId = Workflow.randomUUID()
      val addLineItemCommand = AddLineItemCommand.fromWorkflowCommand(command, lineItemId)

      addLineItemActivities.notifyStarted(addLineItemCommand)

      addLineItemActivities.addLineItemToSales(addLineItemCommand)
      saga.addCompensation(
        addLineItemActivities::removeLineItemFromSales,
        command.cartId,
        lineItemId,
      )

      val warehousePromise = Async.function(addLineItemActivities::addLineItemToWarehouse, command)
      saga.addCompensation(
        addLineItemActivities::removeLineItemFromWarehouse,
        command.cartId,
        lineItemId,
      )

      val marketingPromise = Async.function(addLineItemActivities::addLineItemToMarketing, command)
      saga.addCompensation(
        addLineItemActivities::removeLineItemFromMarketing,
        command.cartId,
        lineItemId,
      )

      Promise.allOf(warehousePromise, marketingPromise).get()

      addLineItemActivities.notifyFinished(addLineItemCommand)
    } catch (exception: Exception) {
      saga.compensate()
    }
  }
}

@ActivityInterface
interface AddLineItemActivities {

  fun notifyStarted(command: AddLineItemCommand)

  fun addLineItemToSales(command: AddLineItemCommand): SalesCart

  fun removeLineItemFromSales(cartId: UUID, lineItemId: UUID): SalesCart

  fun addLineItemToWarehouse(command: AddLineItemWorkflowCommand)

  fun removeLineItemFromWarehouse(cartId: UUID, lineItemId: UUID)

  fun addLineItemToMarketing(command: AddLineItemWorkflowCommand)

  fun removeLineItemFromMarketing(cartId: UUID, lineItemId: UUID)

  fun notifyFinished(command: AddLineItemCommand)
}

@Component
@ActivityImpl(taskQueues = [SALES_CART_PROCESSING_QUEUE])
class AddLineItemActivitiesImpl(
  private val salesCartRepository: SalesCartRepository,
  private val addLineItemHandler: AddLineItemHandler,
  private val warehouseClient: WarehouseClient,
  private val marketingClient: MarketingClient,
  private val rabbitTemplate: RabbitTemplate,
  private val cartsFanout: FanoutExchange,
) : AddLineItemActivities {

  override fun notifyStarted(command: AddLineItemCommand) {
    val addLineItemStarted =
      Event.AddLineItemStarted(
        cartId = command.cartId,
        version = command.version,
        productId = command.productId,
        requestId = command.requestId,
        lineItemId = command.lineItemId,
      )

    val message =
      objectMessageBuilder(addLineItemStarted)
        .andProperties(
          MessagePropertiesBuilder.newInstance()
            .setMessageId(command.requestId)
            .setCorrelationId(command.requestId)
            .build(),
        )
        .build()

    rabbitTemplate.send(cartsFanout.name, "", message)
  }

  override fun addLineItemToSales(command: AddLineItemCommand): SalesCart {
    return addLineItemHandler.handle(command)
  }

  override fun removeLineItemFromSales(cartId: UUID, lineItemId: UUID): SalesCart {
    val cart = salesCartRepository.getById(cartId)
    cart.removeLineItem(lineItemId)
    return salesCartRepository.replace(cart)
  }

  override fun addLineItemToWarehouse(command: AddLineItemWorkflowCommand) {
    warehouseClient.addLineItem(command.cartId, command)
  }

  override fun removeLineItemFromWarehouse(cartId: UUID, lineItemId: UUID) {
    warehouseClient.removeLineItem(cartId, WarehouseRemoveLineItem(lineItemId))
  }

  override fun addLineItemToMarketing(command: AddLineItemWorkflowCommand) {
    marketingClient.addLineItem(command.cartId, command)
  }

  override fun removeLineItemFromMarketing(cartId: UUID, lineItemId: UUID) {
    marketingClient.removeLineItem(cartId, MarketingRemoveLineItem(lineItemId))
  }

  override fun notifyFinished(command: AddLineItemCommand) {
    val lineItemAdded =
      Event.LineItemAdded(
        cartId = command.cartId,
        version = command.version,
        productId = command.productId,
        requestId = command.requestId,
        lineItemId = command.lineItemId,
      )

    val message =
      objectMessageBuilder(lineItemAdded)
        .andProperties(
          MessagePropertiesBuilder.newInstance()
            .setMessageId(command.requestId)
            .setCorrelationId(command.requestId)
            .build(),
        )
        .build()

    rabbitTemplate.send(cartsFanout.name, "", message)
  }
}

// endregion

@Service
class AddLineItemHandler(
  private val salesCartRepository: SalesCartRepository,
) {

  // Optimistic concurrency also guarantees idempotency.
  // However, if you want to return a value in case the command has already been processed instead
  // of a conflict, we need additional work and a transaction.
  // For learning purposes, we're exploring both a request registrar and optimistic concurrency.
  @Transactional
  fun handle(command: AddLineItemCommand): SalesCart {
    val cart = salesCartRepository.getByIdForUpdate(command.cartId)

    if (cart.requestIds.contains(command.requestId)) {
      return cart
    }

    if (command.version != cart.version) {
      throw SalesCartVersionMismatchException(command.version, cart.version)
    }

    val lineItem = LineItem(command.lineItemId, command.productId)
    cart.addLineItem(lineItem, command.requestId)

    return salesCartRepository.replace(cart)
  }
}

@Service
class AddLineItemWorkflowStarter(
  private val workflowClient: WorkflowClient,
) {

  fun handle(command: AddLineItemWorkflowCommand): WorkflowAndRun {
    val workflowStub =
      workflowClient.newWorkflowStub<AddLineItemWorkflow> {
        setTaskQueue(SALES_CART_PROCESSING_QUEUE)
        setWorkflowId(command.requestId)
      }

    val execution = WorkflowClient.start(workflowStub::addLineItem, command)
    return WorkflowAndRun(execution.workflowId, execution.runId)
  }
}

data class WorkflowAndRun(
  val workflowId: String,
  val runId: String,
)

data class AddLineItemWorkflowCommand
@JsonCreator
constructor(
  @JsonProperty("cartId") val cartId: UUID,
  @JsonProperty("version") val version: Int,
  @JsonProperty("productId") val productId: UUID,
  @JsonProperty("requestId") val requestId: String,
)

data class AddLineItemCommand
@JsonCreator
constructor(
  @JsonProperty("cartId") val cartId: UUID,
  @JsonProperty("version") val version: Int,
  @JsonProperty("productId") val productId: UUID,
  @JsonProperty("requestId") val requestId: String,
  @JsonProperty("lineItemId") val lineItemId: UUID,
) {
  companion object {
    fun fromWorkflowCommand(
      command: AddLineItemWorkflowCommand,
      lineItemId: UUID = UUID.randomUUID(),
    ) =
      AddLineItemCommand(
        cartId = command.cartId,
        version = command.version,
        productId = command.productId,
        requestId = command.requestId,
        lineItemId = lineItemId,
      )
  }
}
