package io.hika.rsm.model

import java.math.BigDecimal
import java.time.Instant


data class Order(
    val id: String,
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)


data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: BigDecimal
)

enum class OrderStatus {
    CREATED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}

