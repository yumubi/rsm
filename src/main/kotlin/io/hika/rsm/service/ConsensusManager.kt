package io.hika.rsm.service

import io.hika.rsm.model.Event
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
//todo: to be implemented
class ConsensusManager(
    private val minimumNodes: Int = 2,
    private val timeout: Long = 5000
) {
    private val nodes = ConcurrentHashMap<String, NodeState>()
    private val consensusMutex = Mutex()

    data class NodeState(
        val nodeId: String,
        var lastProcessedSequence: Long = 0,
        var isHealthy: Boolean = true,
        var lastHeartbeat: Long = System.currentTimeMillis()
    )

    suspend fun registerNode(nodeId: String) = consensusMutex.withLock {
        nodes[nodeId] = NodeState(nodeId)
    }

    suspend fun validateConsensus(event: Event): Boolean = consensusMutex.withLock {
        // Check if enough nodes are available
        val healthyNodes = nodes.values.count { it.isHealthy }
        if (healthyNodes < minimumNodes) {
            throw ConsensusException("Insufficient healthy nodes")
        }

        // Validate event sequence across nodes
        val sequenceConsensus = nodes.values.all {
            it.lastProcessedSequence < event.sequence
        }

        if (!sequenceConsensus) {
            throw ConsensusException("Event sequence out of order")
        }

        true
    }

    fun updateNodeState(nodeId: String, sequence: Long) {
        nodes[nodeId]?.let { node ->
            node.lastProcessedSequence = sequence
            node.lastHeartbeat = System.currentTimeMillis()
        }
    }

    class ConsensusException(message: String) : RuntimeException(message)
}
