package io.hika.rsm.controller

import io.hika.rsm.exception.IncompatibleVersionException
import io.hika.rsm.model.*
import io.hika.rsm.service.ReplicatedStateMachine
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val stateMachine: ReplicatedStateMachine,
    private val eventStore: EventStore
) {
    @PostMapping
    suspend fun createOrder(@RequestBody order: Order): ResponseEntity<OrderCreatedResponse> {
        val event = OrderCreatedEvent(
            id = UUID.randomUUID().toString(),
            order = order,
            version = Version(major = 1, minor = 0, patch = 0),
            timestamp = Instant.now(),
            orderId = order.id
        )

        return try {
            stateMachine.process(event)
            ResponseEntity.ok(OrderCreatedResponse(
                orderId = order.id,
                status = order.status,
                timestamp = event.timestamp
            ))
        } catch (e: IncompatibleVersionException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{id}")
    suspend fun getOrder(@PathVariable id: String): ResponseEntity<Order> {
        val order = stateMachine.getCurrentState().getOrder(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
