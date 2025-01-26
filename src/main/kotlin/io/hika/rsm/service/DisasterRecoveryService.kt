package io.hika.rsm.service

import io.hika.rsm.model.Event
import io.hika.rsm.model.EventStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.forEach
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class DisasterRecoveryService(
    private val eventStore: EventStore,
    private val stateMachine: ReplicatedStateMachine,
    private val kafkaTemplate: KafkaTemplate<String, Event>
) {

    private val log = LogFactory.getLog(javaClass)

    @Value("\${dr.snapshot.interval:3600}") // 1 hour default
    private lateinit var snapshotInterval: Duration

    suspend fun initializeFromSnapshot() = coroutineScope {
        val latestSnapshot = eventStore.getLatestSnapshot()
        if (latestSnapshot != null) {
            stateMachine.restoreFromSnapshot(latestSnapshot)
            replayEventsAfterSnapshot(latestSnapshot.sequence)
        }
    }

    private suspend fun replayEventsAfterSnapshot(afterSequence: Long) {
        eventStore.getEvents(afterSequence, limit = Int.MAX_VALUE)
            .collect { event ->
                try {
                    stateMachine.process(event)
                } catch (e: Exception) {
                    log.error { "Failed to replay event: $event" }
                    throw e
                }
            }
    }

    // Warm standby implementation
    @Scheduled(fixedRate = 1000) // Check every second
    suspend fun streamToWarmStandby() {
        try {
            eventStore.getNewEvents()
                .collect { event ->
                    kafkaTemplate.send("dr-events", event)
                }
        } catch (e: Exception) {
            log.error("Failed to stream events to DR", e)
        }
    }
}
