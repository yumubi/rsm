package io.hika.rsm.service

import io.hika.rsm.config.ReplicationMetrics
import io.hika.rsm.exception.IncompatibleVersionException
import io.hika.rsm.exception.TerminateException
import io.hika.rsm.exception.UnsupportedEventException
import io.hika.rsm.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Service
class ReplicatedStateMachine(
    private val eventStore: EventStore,
    private val snapshotStore: SnapshotStore,
    private val metrics: ReplicationMetrics
) {
    private val state = AtomicReference<State>()
    private var currentVersion = Version(1, 0, 0)
    private val processingLock = Mutex()



    suspend fun process(event: Event) = coroutineScope {
        val startTime = System.nanoTime()

        processingLock.withLock {
            validateEventVersion(event.version)

            val currentState = state.get()
            val newState = when (event) {
                is OrderEvent -> currentState.apply(event)
                is DataUpdatedEvent -> handleDataUpdate(currentState, event)
                is ConfigurationChangedEvent -> handleConfigChange(currentState, event)
                else -> throw UnsupportedEventException("Unknown event type")
            }

            state.set(newState)
            eventStore.append(listOf(event))

            metrics.recordEventProcessing(Duration.ofNanos(System.nanoTime() - startTime))

            if (shouldTakeSnapshot()) {
                takeSnapshot()
            }
        }
    }


    fun validateEventVersion(version: Version) {
        when {
            version.major > currentVersion.major -> {
                if (!version.canBeIgnored) {
                    if (version.shouldTerminate) {
                        throw TerminateException("Major version incompatibility")
                    }
                    throw IncompatibleVersionException("Incompatible major version")
                }
            }
            version.major < currentVersion.major -> {
                // Handle backward compatibility scenarios
                throw IncompatibleVersionException("Downgrade not allowed")
            }
        }
    }

    private suspend fun takeSnapshot() {
        val currentState = state.get()
        val snapshot = Snapshot(
            id = UUID.randomUUID().toString(),
            sequence = eventStore.getCurrentSequence(),
            state = currentState,
            version = currentVersion,
            timestamp = Instant.now()
        )
        snapshotStore.save(snapshot)
        metrics.recordSnapshotCreation(Duration.ZERO) // Add actual duration measurement
    }



    private fun handleDataUpdate(currentState: State, event: DataUpdatedEvent): State {
        return currentState.copy(
            lastUpdated = event.timestamp
        )
    }

    private fun handleConfigChange(currentState: State, event: ConfigurationChangedEvent): State {
        return currentState.copy(
            version = event.version,
            lastUpdated = event.timestamp
        )
    }

    suspend fun getCurrentState(): State = state.get()

    suspend fun restoreFromSnapshot(snapshot: Snapshot) {
        state.set(snapshot.state)
        currentVersion = snapshot.version
    }

    private fun shouldTakeSnapshot(): Boolean {
        val currentState = state.get()
        return Duration.between(currentState.lastUpdated, Instant.now()).toHours() >= 1
    }

    private fun shouldTakeSnapshot(event: Event): Boolean {
        // Take snapshot every 1000 events or every hour
        return event.sequence % 1000 == 0L ||
                Duration.between(state.get().lastUpdated, Instant.now()).toHours() >= 1
    }
}
