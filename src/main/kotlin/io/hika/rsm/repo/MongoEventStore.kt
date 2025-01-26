package io.hika.rsm.repo
import io.hika.rsm.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.util.concurrent.atomic.AtomicLong


@Repository
class MongoEventStore(
    private val mongoTemplate: ReactiveMongoTemplate
) : EventStore {

    private var currentSequence = AtomicLong(0)

    override suspend fun append(events: List<Event>) = coroutineScope {
        events.forEach { event ->
            val nextSequence = currentSequence.incrementAndGet()
            val eventWithSequence = when(event) {
                is OrderCreatedEvent -> event.copy(sequence = nextSequence)
                is OrderStatusUpdatedEvent -> event.copy(sequence = nextSequence)
                is OrderCancelledEvent -> event.copy(sequence = nextSequence)
                is DataUpdatedEvent -> event.copy(sequence = nextSequence)
                is ConfigurationChangedEvent -> event.copy(sequence = nextSequence)
            }
            mongoTemplate.save(eventWithSequence, "events").awaitSingle()
        }
    }

    override suspend fun getLatestSnapshot(): Snapshot? =
        mongoTemplate.find(
            Query().with(Sort.by(Sort.Direction.DESC, "sequence")).limit(1),
            Snapshot::class.java,
            "snapshots"
        ).awaitFirstOrNull()

    override suspend fun getCurrentSequence(): Long = currentSequence.get()

    override suspend fun getNewEvents(): Flow<Event> =
        mongoTemplate.find(
            Query(Criteria.where("sequence").gt(getCurrentSequence())),
            Event::class.java,
            "events"
        ).asFlow()



    override suspend fun getEvents(afterSequence: Long, limit: Int): Flow<Event> =
        mongoTemplate.find(
            Query().apply {
                addCriteria(Criteria.where("sequence").gt(afterSequence))
                limit(limit)
                with(Sort.by(Sort.Direction.ASC, "sequence"))
            },
            Event::class.java,
            "events"
        ).asFlow()

    override suspend fun getSnapshot(id: String): Snapshot? =
        mongoTemplate.findById(id, Snapshot::class.java, "snapshots").awaitFirstOrNull()

    override suspend fun saveSnapshot(snapshot: Snapshot) {
        mongoTemplate.save(snapshot, "snapshots").awaitSingle()
    }

}

