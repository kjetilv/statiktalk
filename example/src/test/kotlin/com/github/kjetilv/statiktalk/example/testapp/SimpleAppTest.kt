package com.github.kjetilv.statiktalk.example.testapp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.db.MemorySessionDb
import com.github.kjetilv.statiktalk.example.testapp.shared.SessionsDao
import com.github.kjetilv.statiktalk.example.testapp.shared.*
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import java.net.ServerSocket
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SimpleAppTest {

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
        val now = AtomicReference(Instant.EPOCH)
        val time = {
            now.getAndUpdate {
                it.plus(Duration.ofDays(1))
            }.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val events = mutableListOf<String>()
        val unauth = mutableMapOf<String, String>()

        val authorizedUsers = mapOf(
            "foo42" to "123",
            "kv" to "234",
            "zot" to "345"
        )

        val statusMap = mapOf(
            "foo42" to "elite",
            "kv" to "harmless"
        )
        val frequentCustomers = setOf("foo42")

        val eliteLogins = mutableSetOf<String>()
        val harmlessLogins = mutableSetOf<String>()

        withRapid { rapids ->
            waitForEvent("application_ready")
            waitForEvent("application_up")

            rapids.handleLoginAttempt(
                object : LoginAttempt {
                    override fun loginAttempted(userId: String, context: Context) {
                        context["loginTime"] = time()
                        rapids.authorization()
                            .userLoggedIn(userId, context)
                    }
                })

            rapids.handleAuthorization(
                object : Authorization {
                    override fun userLoggedIn(userId: String, context: Context) {
                        authorizedUsers[userId]?.let { key ->
                            rapids.sessions()
                                .loggedIn(userId, key, context = context)
                        } ?: rapids.unauthorized().unknownuser(userId, context = context)
                    }
                }
            )

            rapids.handleUnauthorized(object : Unauthorized {
                override fun unknownuser(userId: String, loginTime: String?, context: Context) {
                    unauth[userId] = loginTime ?: "unknown"
                }
            })

            rapids.handleAuthorizedUserEnricher(
                object : AuthorizedUserEnricher {
                    override fun authorized(userId: String, userKey: String) {
                        statusMap[userId]?.also {
                            rapids.sessions().userHasStatus(userId, userKey, it)
                        }
                    }
                }
            )

            rapids.handleAuthorizedUserEnricher(
                object : AuthorizedUserEnricher {
                    override fun authorized(userId: String, userKey: String) {
                        if (frequentCustomers.contains(userId)) {
                            rapids.sessions().userIsReturning(userId, userKey, true)
                        }
                    }
                }
            )

            val sessionDb = MemorySessionDb()
            rapids.handleSessions(
                SessionsDao(sessionDb, events::add)
            )

            rapids.handleStatusProcessor(object : StatusProcessor {
                override fun status(userKey: String, status: String) {
                    eliteLogins.add(userKey)
                }
            }, reqs = StatusProcessorReqs(status = "elite"))

            rapids.handleStatusProcessor(object : StatusProcessor {
                override fun status(userKey: String, status: String) {
                    harmlessLogins.add(userKey)
                }
            }, reqs = StatusProcessorReqs(status = "harmless"))

            rapids.loginAttempt().apply {
                loginAttempted("foo42")
                loginAttempted("unknown")
                loginAttempted("kv")
                loginAttempted("stranger")
            }

            await("wait until settled")
                .atMost(10, SECONDS)
                .until {
                    sessionDb.sessions().any {
                        it == User(
                            userId = "foo42",
                            userKey = "123",
                            status = "elite",
                            returning = true
                        )
                    } && sessionDb.sessions().any {
                        it == User(
                            userId = "kv",
                            userKey = "234",
                            status = "harmless"
                        )
                    } && unauth.size == 2
                }
        }

        assertEquals(2, events.size)
        assertEquals("Login was registered at 1970-01-01", events[0])
        assertEquals("Login was registered at 1970-01-03", events[1])

        assertEquals(1, harmlessLogins.size)
        assertEquals("234", harmlessLogins.firstOrNull())
        assertEquals(1, eliteLogins.size)
        assertEquals("123", eliteLogins.firstOrNull())

        assertEquals("1970-01-02", unauth["unknown"])
        assertEquals("1970-01-04", unauth["stranger"])

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
