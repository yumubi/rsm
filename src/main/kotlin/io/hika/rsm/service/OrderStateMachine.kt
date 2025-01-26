package io.hika.rsm.service

import io.hika.rsm.config.ReplicationMetrics
import io.hika.rsm.exception.UnsupportedEventException
import io.hika.rsm.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Service
class OrderStateMachine(
    private val eventStore: EventStore,
    private val snapshotStore: SnapshotStore,
    private val metrics: ReplicationMetrics
) {
    private val state = AtomicReference(State(version = Version(1, 0, 0), lastUpdated = Instant.now()))
    private val processingLock = Mutex()

    private suspend fun validateEventVersion(eventVersion: Version) {
        val currentState = state.get()
        if (eventVersion < currentState.version) {
            throw UnsupportedEventException("Event version is older than current state")
        }
    }


    suspend fun processOrderEvent(event: OrderEvent) = coroutineScope {
        val startTime = System.nanoTime()

        processingLock.withLock {
            validateEventVersion(event.version)

            val currentState = state.get()
            val newState = currentState.apply(event)

            state.set(newState)
            eventStore.append(listOf(event))

            metrics.recordEventProcessing(Duration.ofNanos(System.nanoTime() - startTime))

            if (shouldTakeSnapshot(event)) {
                takeSnapshot(newState)
            }
        }
    }

    private fun shouldTakeSnapshot(event: Event): Boolean {
        // Take snapshot every 1000 events or every hour
        return event.sequence % 1000 == 0L ||
               Duration.between(state.get().lastUpdated, Instant.now()).toHours() >= 1
    }

    private suspend fun takeSnapshot(currentState: State) {
        val snapshot = Snapshot(
            id = UUID.randomUUID().toString(),
            sequence = eventStore.getCurrentSequence(),
            state = currentState,
            version = currentState.version,
            timestamp = Instant.now()
        )

        snapshotStore.save(snapshot)
        cleanupOldSnapshots()
    }

    private suspend fun cleanupOldSnapshots() {
        // Keep last 24 hours of snapshots
        val retentionTime = Instant.now().minus(24, ChronoUnit.HOURS)
        snapshotStore.deleteOlderThan(retentionTime)
    }
}

private operator fun Version.compareTo(version: Version): Int {
    return when {
        major > version.major -> 1
        major < version.major -> -1
        minor > version.minor -> 1
        minor < version.minor -> -1
        patch > version.patch -> 1
        patch < version.patch -> -1
        else -> 0
    }
}
