package com.github.kjetilv.statiktalk.example.testapp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.microservices.*
import com.github.kjetilv.statiktalk.example.testapp.microservices.User
import com.github.kjetilv.statiktalk.example.testapp.shared.generated.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.*
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import java.net.ServerSocket
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StatikTalkTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


    private val testTopic = "a-test-topic"

    private val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.1"))

    private lateinit var appUrl: String

    private lateinit var testConsumer: Consumer<String, String>

    private lateinit var consumerJob: Job

    private val messages = mutableListOf<String>()

    @DelicateCoroutinesApi
    @BeforeAll
    internal fun setup() {
        kafkaContainer.start()
        testConsumer = KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer()).apply {
            subscribe(listOf(testTopic))
        }
        consumerJob = GlobalScope.launch {
            while (this.isActive) testConsumer.poll(Duration.ofSeconds(1)).forEach { messages.add(it.value()) }
        }
    }

    @AfterAll
    internal fun teardown() {
        runBlocking { consumerJob.cancelAndJoin() }
        testConsumer.close()
        kafkaContainer.stop()
    }

    @BeforeEach
    fun clearMessages() {
        messages.clear()
    }

    @DelicateCoroutinesApi
    @Test
    fun `should login and collect information`() {
        withRapid { rapids ->
            waitForEvent("application_ready")
            waitForEvent("application_up")

            val statusMap = mapOf("foo42" to "elite")
            val authorizedUsers = mapOf("foo42" to "123")
            val returningUsers = setOf("foo42")

            val memorySessions = MemorySessions()

            rapids.handleSessions(
                SessionsService(memorySessions)
            )
            rapids.handleLoginAttempt(
                LoginAttemptService(rapids.authorization()) { Instant.EPOCH }
            )
            rapids.handleAuthorization(
                AuthorizationService(rapids.sessions(), authorizedUsers)
            )
            rapids.handleStatusCustomer(
                StatusCustomerService(statusMap, rapids.sessions())
            )
            rapids.handleReturningCustomer(
                ReturningCustomerService(returningUsers, rapids.sessions())
            )

            rapids.loginAttempt().loginAttempted("foo42", Context.DUMMY)

            await("wait until settled")
                .atMost(10, SECONDS)
                .until {
                    memorySessions.sessions().firstOrNull()?.let {
                        it == User(
                            userId = "foo42",
                            userKey = "123",
                            status = "elite",
                            returning = true
                        )
                    } ?: false
                }
        }
    }

    private fun waitForEvent(event: String): JsonNode? {
        return await("wait until $event")
            .atMost(10, SECONDS)
            .until({
                messages.map { objectMapper.readTree(it) }
                    .firstOrNull { it.path("@event_name").asText() == event }
            }) { it != null }
    }

    private fun consumerProperties(): MutableMap<String, Any> {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    }

    private fun createConfig(): Map<String, String> {
        val randomPort = ServerSocket(0).use { it.localPort }
        appUrl = "http://localhost:$randomPort"
        return mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to kafkaContainer.bootstrapServers,
            "KAFKA_CONSUMER_GROUP_ID" to "component-test",
            "KAFKA_RAPID_TOPIC" to testTopic,
            "RAPID_APP_NAME" to "app-name",
            "HTTP_PORT" to "$randomPort"
        )
    }

    @DelicateCoroutinesApi
    private fun withRapid(
        builder: RapidApplication.Builder? = null,
        collectorRegistry: CollectorRegistry = CollectorRegistry(),
        block: (RapidsConnection) -> Unit
    ) {
        val rapidsConnection =
            (builder ?: RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(createConfig())))
                .withCollectorRegistry(collectorRegistry)
                .build()
        val job = GlobalScope.launch { rapidsConnection.start() }
        try {
            block(rapidsConnection)
        } finally {
            rapidsConnection.stop()
            runBlocking { job.cancelAndJoin() }
        }
    }
}
