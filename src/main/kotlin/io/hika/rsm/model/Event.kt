package io.hika.rsm.model

import org.springframework.data.annotation.Id
import java.time.Instant


sealed class Event {
    abstract val id: String
    abstract val version: Version
    abstract val timestamp: Instant
    abstract val sequence: Long
}

data class DataUpdatedEvent(
    override val id: String,
    override val version: Version,
    override val timestamp: Instant,
    override val sequence: Long = 0,
    val data: Map<String, Any>
) : Event()

data class ConfigurationChangedEvent(
    override val id: String,
    override val version: Version,
    override val timestamp: Instant,
    override val sequence: Long = 0,
    val changes: Map<String, Any>
) : Event()

sealed class OrderEvent : Event() {
    abstract val orderId: String
}

data class OrderCreatedEvent(
    override val id: String,
    override val version: Version,
    override val timestamp: Instant,
    override val sequence: Long = 0,
    override val orderId: String,
    val order: Order
) : OrderEvent()

data class OrderStatusUpdatedEvent(
    override val id: String,
    override val version: Version,
    override val timestamp: Instant,
    override val sequence: Long = 0,
    override val orderId: String,
    val newStatus: OrderStatus,
    val previousStatus: OrderStatus
) : OrderEvent()

data class OrderCancelledEvent(
    override val id: String,
    override val version: Version,
    override val timestamp: Instant,
    override val sequence: Long = 0,
    override val orderId: String,
    val reason: String
) : OrderEvent()

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val canBeIgnored: Boolean = false,
    val shouldTerminate: Boolean = false
) {
    override fun toString() = "$major.$minor.$patch"
}


data class Snapshot(
    val id: String,
    val sequence: Long,
    val state: State,
    val version: Version,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
) {
    @Id
    private val _id: String = id
}



data class State(
    val orders: Map<String, Order> = mutableMapOf(),
    val version: Version,
    val lastUpdated: Instant
) {
    fun apply(event: Event): State = when (event) {
        is OrderCreatedEvent -> copy(
            orders = orders + (event.orderId to event.order),
            lastUpdated = event.timestamp
        )
        is OrderStatusUpdatedEvent -> {
            val updatedOrder = orders[event.orderId]?.copy(
                status = event.newStatus,
                updatedAt = event.timestamp
            )
            copy(
                orders = if (updatedOrder != null) orders + (event.orderId to updatedOrder) else orders,
                lastUpdated = event.timestamp
            )
        }
        is OrderCancelledEvent -> {
            val cancelledOrder = orders[event.orderId]?.copy(
                status = OrderStatus.CANCELLED,
                updatedAt = event.timestamp
            )
            copy(
                orders = if (cancelledOrder != null) orders + (event.orderId to cancelledOrder) else orders,
                lastUpdated = event.timestamp
            )
        }
        else -> this
    }

    fun getOrder(id: String): Order? = orders[id]
}
