package com.github.kjetilv.statiktalk.example.testapp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kjetilv.statiktalk.LocalKafkaConfig
import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Require.empty
import com.github.kjetilv.statiktalk.api.Require.notValue
import com.github.kjetilv.statiktalk.api.Require.value
import com.github.kjetilv.statiktalk.example.testapp.db.MemorySessionDb
import com.github.kjetilv.statiktalk.example.testapp.shared.*
import com.github.kjetilv.statiktalk.example.testapp.shared.generated.*
import io.ktor.server.engine.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.*
import no.nav.helse.rapids_rivers.ConsumerProducerFactory
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
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

    private val localConfig = LocalKafkaConfig(kafkaContainer)

    private val factory = ConsumerProducerFactory(localConfig)

    private lateinit var appUrl: String

    private lateinit var testConsumer: Consumer<String, String>

    private lateinit var consumerJob: Job

    private val messages = mutableListOf<String>()

    @DelicateCoroutinesApi
    @BeforeAll
    internal fun setup() {
        kafkaContainer.start()
        testConsumer = factory.createConsumer("test-consumer").apply {
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

    private fun waitForEvent(event: String): JsonNode? {
        return await("wait until $event")
            .atMost(60, SECONDS)
            .until({
                messages.map { objectMapper.readTree(it) }
                    .firstOrNull { it.path("@event_name").asText() == event }
            }) { it != null }
    }

    private fun response(path: String) =
        URL("$appUrl$path").openStream().use { it.bufferedReader().readText() }

    private fun isOkResponse(path: String): Boolean {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$appUrl$path").openConnection() as HttpURLConnection)
            return conn.responseCode in 200..299
        } catch (err: IOException) {
            System.err.println("$appUrl$path: ${err.message}")
            //err.printStackTrace(System.err)
        } finally {
            conn?.disconnect()
        }
        return false
    }

    @DelicateCoroutinesApi
    private fun withRapid(
        ktor: (port: Int) -> ApplicationEngine? = { null },
        collectorRegistry: CollectorRegistry = CollectorRegistry(),
        block: (RapidsConnection) -> Unit
    ) {
        val randomPort = ServerSocket(0).use { it.localPort }
        appUrl = "http://localhost:$randomPort"
        val rapidApplicationConfig = RapidApplication.RapidApplicationConfig(
            appName = "app-name",
            instanceId = "app-name-0",
            rapidTopic = testTopic,
            kafkaConfig = localConfig,
            consumerGroupId = "component-test",
            httpPort = randomPort,
            collectorRegistry = collectorRegistry
        )
        val builder = RapidApplication.Builder(rapidApplicationConfig)
        ktor(randomPort)?.let { builder.withKtor(it) }
        val rapidsConnection = builder.build()
        val job = GlobalScope.launch { rapidsConnection.start() }
        try {
            block(rapidsConnection)
        } finally {
            rapidsConnection.stop()
            runBlocking { job.cancelAndJoin() }
        }
    }

    //
    // Simple app which reacts to user logins and processes specially authorized users,  enriching them with
    // various additional data.
    //
    @DelicateCoroutinesApi
    @Test
    fun `should login and collect information`() {
        // An observer-driven quantum clock: Each time we look at it a day has passed
        val now = AtomicReference(Instant.EPOCH)
        val time = {
            now.getAndUpdate {
                it.plus(Duration.ofDays(1))
            }.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        // The following are the data we have on the users when the test starts
        //
        // Authorized users.  They have their own numeric key.
        val authorizedUsers = mapOf(
            "foo41" to "012",
            "foo42" to "123",
            "msuser" to "999",
            "kv" to "234",
            "zot" to "345"
        )
        // Status for the various users
        val statusMap = mapOf(
            "foo42" to "elite",
            "kv" to "harmless"
        )
        // Returning customers we know of
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
        // ... and from the unwashed masses
        val unauth = mutableMapOf<String, String>()
        // A collector recording the channels we've received messages from
        val channelsReceived = mutableSetOf<String>()

        // Set up the app and fire some login attempts out on the bus
        withRapid { rapids ->
            waitForEvent("application_ready")
            waitForEvent("application_up")

            // Captures and broadcasts login attempts, and records which channels they came from
            val loginAttempt = object : LoginAttempt {
                override fun loginAttempted(
                    userId: String,
                    channel: String?,
                    browser: String?,
                    externalId: String?,
                    context: Context
                ) {
                    channelsReceived += (channel ?: "")
                    context["loginTime"] = time()
                    rapids.authorization().userLoggedIn(
                        userId,
                        context
                    )
                }
            }

            // Checks if logged-in user is authorized in the system
            val authorization = object : Authorization {
                override fun userLoggedIn(userId: String, context: Context) {
                    authorizedUsers[userId]?.let { key ->
                        rapids.sessions().loggedIn(userId, key, context = context)
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

            // Checks if this is a returning customer

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

            // Handle logins, though only for the website channel
            rapids.handleLoginAttempt(
                loginAttempt,
                reqs = LoginAttemptReqs(
                    channel = value("website"),  // Only accept website logins (require value)
                    browser = notValue("msie"), // Reject dangeours browsers (reject value)
                    externalId = empty()          // Stay away from logins handled by others (reject key)
                )
            ) {
                rejectKey("foobar") // Drop down to R&R for custom requirements
            }

            // Handle login authorization
            rapids.handleAuthorization(authorization)

            // Handle authorizations
            rapids.handleUnauthorized(unauthorized)

            // Using this style isn't picked up by IDEA as an implementation it will find – yet
            rapids.handleAuthorizedUserEnricher({ userId, userKey ->
                statusMap[userId]?.also {
                    rapids.sessions().userHasStatus(userId, userKey, it)
                }
            })

            rapids.handleAuthorizedUserEnricher({ userId, userKey ->
                if (frequentCustomers.contains(userId)) {
                    rapids.sessions().userIsReturning(userId, userKey, true)
                }
            })

            // Hook on to status=elite and store it
            rapids.handleStatusProcessor(
                eliteStatusReorder,
                reqs = StatusProcessorReqs(status = value("elite"))
            )

            // Hook on to status=harmless and store it
            rapids.handleStatusProcessor(
                harmlessStatusProcessor,
                reqs = StatusProcessorReqs(
                    status = value("harmless")
                )
            )

            // Backend service, stores sesssion information on authorized users
            rapids.handleSessions(SessionsDao(sessionDb, eventsLog::add))

            // To start the ball rolling, we send out messages about some logins
            rapids.loginAttempt {
                loginAttempted("foo42", "website") // Authorized, elite user
                loginAttempted("msuser", "website", browser = "msie") // Authorized user, but using msie
                loginAttempted("foo41", "channel0") // Authorized, but on different channel
                loginAttempted("foo41", "website", externalId = "42") // Authorized, but not handled by us
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
}
