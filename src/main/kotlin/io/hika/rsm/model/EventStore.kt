package io.hika.rsm.model

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface EventStore {
    suspend fun append(events: List<Event>)
    suspend fun getEvents(afterSequence: Long, limit: Int): Flow<Event>
    suspend fun getSnapshot(id: String): Snapshot?
    suspend fun saveSnapshot(snapshot: Snapshot)
    suspend fun getLatestSnapshot(): Snapshot?
    suspend fun getNewEvents(): Flow<Event>
    suspend fun getCurrentSequence(): Long
}

interface SnapshotStore {
    suspend fun save(snapshot: Snapshot)
    suspend fun getLatest(): Snapshot?
    suspend fun getBySequence(sequence: Long): Snapshot?
    suspend fun deleteOlderThan(timestamp: Instant)
}
