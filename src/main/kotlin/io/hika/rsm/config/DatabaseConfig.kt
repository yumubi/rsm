package io.hika.rsm.config

import com.mongodb.reactivestreams.client.MongoClients
import io.hika.rsm.model.Event
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class DatabaseConfig {
    @Bean
    fun reactiveMongoTemplate(
        @Value("\${spring.data.mongodb.uri}") mongoUri: String,
        @Value("\${spring.data.mongodb.database}") database: String
    ): ReactiveMongoTemplate {
        val mongoClient = MongoClients.create(mongoUri)
        return ReactiveMongoTemplate(mongoClient, database)
    }
}

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaTemplate(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String
    ): KafkaTemplate<String, Event> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to "3"
        )

        return KafkaTemplate(DefaultKafkaProducerFactory(props))
    }
}
