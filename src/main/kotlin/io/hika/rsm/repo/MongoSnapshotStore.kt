package io.hika.rsm.repo

import io.hika.rsm.model.Snapshot
import io.hika.rsm.model.SnapshotStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MongoSnapshotStore(
    private val mongoTemplate: ReactiveMongoTemplate
) : SnapshotStore {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun save(snapshot: Snapshot) {
        try {
            mongoTemplate.save(snapshot, "snapshots").awaitSingle()
        } catch (e: Exception) {
            logger.error("Failed to save snapshot: ${snapshot.id}", e)
            throw e
        }
    }

    override suspend fun getLatest(): Snapshot? =
        mongoTemplate.find(
            Query().with(Sort.by(Sort.Direction.DESC, "sequence")).limit(1),
            Snapshot::class.java,
            "snapshots"
        ).awaitFirstOrNull()

    override suspend fun getBySequence(sequence: Long): Snapshot? =
        mongoTemplate.findOne(
            Query(Criteria.where("sequence").`is`(sequence)),
            Snapshot::class.java,
            "snapshots"
        ).awaitFirstOrNull()

    override suspend fun deleteOlderThan(timestamp: Instant) {
        mongoTemplate.remove(
            Query(Criteria.where("timestamp").lt(timestamp)),
            Snapshot::class.java,
            "snapshots"
        ).awaitSingle()
    }
}
