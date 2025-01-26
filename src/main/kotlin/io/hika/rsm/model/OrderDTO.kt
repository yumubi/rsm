package io.hika.rsm.model

import java.time.Instant


data class OrderCreatedResponse(
    val orderId: String,
    val status: OrderStatus,
    val timestamp: Instant
)

data class OrderResponse(
    val order: Order,
    val _links: Map<String, String>
)

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)


data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItem>
)

data class UpdateOrderStatusRequest(
    val status: OrderStatus,
    val reason: String? = null
)
