package io.hika.rsm.service
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.hika.rsm.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class OrderService(
    private val orderStateMachine: OrderStateMachine,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderService")

    suspend fun createOrder(createOrderRequest: CreateOrderRequest): OrderCreatedResponse {
        val order = Order(
            id = UUID.randomUUID().toString(),
            customerId = createOrderRequest.customerId,
            items = createOrderRequest.items,
            totalAmount = calculateTotalAmount(createOrderRequest.items),
            status = OrderStatus.CREATED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val event = OrderCreatedEvent(
            id = UUID.randomUUID().toString(),
            version = Version(1, 0, 0),
            timestamp = Instant.now(),
            orderId = order.id,
            order = order
        )

        return withContext(Dispatchers.IO) {
            mono {
                orderStateMachine.processOrderEvent(event)
                OrderCreatedResponse(
                    orderId = order.id,
                    status = order.status,
                    timestamp = order.createdAt
                )
            }.transform(CircuitBreakerOperator.of(circuitBreaker))
                .block()
        } ?: throw IllegalStateException("Order creation failed")
    }


    private fun calculateTotalAmount(items: List<OrderItem>): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.price * BigDecimal(item.quantity))
        }
}
