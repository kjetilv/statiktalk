package com.github.kjetilv.statiktalk.example.testapp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.db.MemorySessionDb
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

    //
    // Simple app which reacts to user logins and processes specially authorized users,  enriching them with
    // various additional data.
    //
    @DelicateCoroutinesApi
    @Test
    fun `should login and collect information`() {
        // A ticking clock, each time we look at it a day has passed
        val now = AtomicReference(Instant.EPOCH)
        val time = {
            now.getAndUpdate {
                it.plus(Duration.ofDays(1))
            }.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        // The following are the data we have on the users when the test starts
        //
        // These are the authorized users.  They have their own numeric key.
        val authorizedUsers = mapOf(
                "foo41" to "012",
                "foo42" to "123",
                "kv" to "234",
                "zot" to "345"
        )
        // These are the statuss for the various users
        val statusMap = mapOf(
                "foo42" to "elite",
                "kv" to "harmless"
        )
        // These are the returning customers we know of
        val frequentCustomers = setOf("foo42")

        // The following is mutable state that gets updated during the test
        //
        // A sessions database
        val sessionDb = MemorySessionDb()
        // Events recorded by the Db layer
        val eventsLog = mutableListOf<String>()
        // Logins we picked up from the elite squad
        val eliteLogins = mutableSetOf<String>()
        // ... and from the harmless types
        val harmlessLogins = mutableSetOf<String>()
        // ... and from unwashed masses
        val unauth = mutableMapOf<String, String>()
        // These are the channels we got messages from
        val channelsReceived = mutableSetOf<String>()

        // Stores sesssion information on authorized users
        val sessionsDao = SessionsDao(sessionDb, eventsLog::add)

        // Set up the app and fire some login attempts out on the bus
        withRapid { rapids ->
            waitForEvent("application_ready")
            waitForEvent("application_up")

            // Captures and broadcasts login attempts, and records which channels they came from
            val loginAttempted = object : LoginAttempt {
                override fun loginAttempted(userId: String, channel: String?, context: Context) {
                    channelsReceived += (channel ?: "")
                    context["loginTime"] = time()
                    rapids.authorization()
                            .userLoggedIn(userId, context)
                }
            }

            // Checks if logged-in user is authorized in the system
            val authorization = object : Authorization {
                override fun userLoggedIn(userId: String, context: Context) {
                    authorizedUsers[userId]?.let { key ->
                        rapids.sessions()
                                .loggedIn(userId, key, context = context)
                    } ?: rapids.unauthorized().unknownuser(userId, context = context)
                }
            }

            // Records unauthorized users
            val unauthorized = object : Unauthorized {
                override fun unknownuser(userId: String, loginTime: String?, context: Context) {
                    unauth[userId] = loginTime ?: "unknown"
                }
            }

            // Checks if users have a special status in the system
            val statusEnricher = object : AuthorizedUserEnricher {
                override fun authorized(userId: String, userKey: String) {
                    statusMap[userId]?.also {
                        rapids.sessions().userHasStatus(userId, userKey, it)
                    }
                }
            }

            // Checks if this is a returning customer
            val returningEnricher = object : AuthorizedUserEnricher {
                override fun authorized(userId: String, userKey: String) {
                    if (frequentCustomers.contains(userId)) {
                        rapids.sessions().userIsReturning(userId, userKey, true)
                    }
                }
            }

            // Records any "elite" status users authorized
            val eliteStatusReorder = object : StatusProcessor {
                override fun status(userKey: String, status: String) {
                    eliteLogins.add(userKey)
                }
            }

            // Records any "harmless" status users authorized
            val harmlessStatusProcessor = object : StatusProcessor {
                override fun status(userKey: String, status: String) {
                    harmlessLogins.add(userKey)
                }
            }

            // Handle logins, though only for the website
            rapids.handleLoginAttempt(
                    loginAttempted, reqs = LoginAttemptReqs(channel = "website"))

            rapids.handleAuthorization(authorization)
            rapids.handleUnauthorized(unauthorized)

            rapids.handleAuthorizedUserEnricher(statusEnricher)
            rapids.handleAuthorizedUserEnricher(returningEnricher)

            // Hook on to status=elite and store it
            rapids.handleStatusProcessor(
                    eliteStatusReorder,
                    reqs = StatusProcessorReqs(status = "elite"))

            // Hook on to status=harmless and store it
            rapids.handleStatusProcessor(
                    harmlessStatusProcessor,
                    reqs = StatusProcessorReqs(status = "harmless"))

            rapids.handleSessions(sessionsDao)

            // To start the ball rolling, we need a sender for registered login attempts
            val loginAttempt = rapids.loginAttempt()

            loginAttempt.apply {
                loginAttempted("foo42", "website") // Authorized, elite user
                loginAttempted("foo41", "channel0") // Authorized, but on different channel
                loginAttempted("unknown", "website") // Unauthorized user
                loginAttempted("kv", "website") // Authorized user, harmless
                loginAttempted("wrongchannel", "foobar") // Totally irrelevant user
                loginAttempted("stranger", "website") // Some unauthorized stranger
                loginAttempted("zot", "website") // Authorized user, with no particular data
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
                        } && sessionDb.sessions().any {
                            it == User(
                                    userId = "zot",
                                    userKey = "345"
                            )
                        } && unauth.size == 2
                    }
        }

        assertEquals(3, eventsLog.size)
        assertEquals("Login was registered at 1970-01-01", eventsLog[0])
        assertEquals("Login was registered at 1970-01-03", eventsLog[1])
        assertEquals("Login was registered at 1970-01-05", eventsLog[2])

        assertEquals(1, harmlessLogins.size)
        assertEquals("234", harmlessLogins.firstOrNull())

        assertEquals(1, eliteLogins.size)
        assertEquals("123", eliteLogins.firstOrNull())

        // Login times for unauthorized
        assertEquals("1970-01-02", unauth["unknown"])
        assertEquals("1970-01-04", unauth["stranger"])

        // Should only see "website" here, nothing else should reach us
        assertEquals(1, channelsReceived.size)
        assertEquals("website", channelsReceived.toList()[0])
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
