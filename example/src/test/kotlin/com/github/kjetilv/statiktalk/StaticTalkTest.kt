package com.github.kjetilv.statiktalk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.Typed
import com.github.kjetilv.statiktalk.example.generated.handleTyped
import com.github.kjetilv.statiktalk.example.generated.typed
import com.github.kjetilv.statiktalk.test.Factoids
import com.github.kjetilv.statiktalk.test.generated.factoids
import com.github.kjetilv.statiktalk.test.generated.handleFactoids
import io.ktor.server.engine.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.*
import no.nav.helse.rapids_rivers.ConsumerProducerFactory
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.com.google.common.util.concurrent.AtomicDouble
import org.testcontainers.shaded.org.awaitility.Awaitility
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StaticTalkTest {

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

    private val factoid = mutableMapOf<String, String>()

//    @Disabled
    @DelicateCoroutinesApi
    @Test
    fun `should annoy people with interesting factoids on random subject matter`() {
        val asideAtomic = AtomicReference<String>()

        withRapid { rapids ->
            waitForEvent("application_ready")

            rapids.factoids().annoyWith(
                "Static typing",
                "It's recommended by 9 out of 10 dentists",
                aside = "Less gnashing of the teeth you see"
            )

            rapids.handleFactoids(object : Factoids {

                override fun annoyWith(
                    subjectMatter: String,
                    interestingFact: String,
                    aside: String?,
                    context: Context?
                ) {
                    factoid[subjectMatter] = interestingFact
                    aside?.also(asideAtomic::set)
                }
            })

            requireNotNull(waitForFactoid("Static typing")) { "did not receive factoid before timeout" }
        }

        assertEquals("Less gnashing of the teeth you see", asideAtomic.get())
        assertNotNull(factoid["Static typing"])
    }

    @DelicateCoroutinesApi
    @Test
    fun `should support types`() {

        val nameA = AtomicReference<String>()
        val fortyTwoA = AtomicInteger()
        val fiftyFourA = AtomicLong()
        val wordA = AtomicBoolean()
        val twiceA = AtomicDouble()
        val halfA = AtomicDouble()
        val preciseA = AtomicReference<BigDecimal>()
        val bigA = AtomicReference<BigInteger>()

        withRapid { rapids ->
            waitForEvent("application_ready")

            rapids.typed().hello(
                "John",
                42,
                54L,
                true,
                84.0,
                42.0F,
                BigDecimal.TEN.setScale(10),
                BigInteger.TWO)

            rapids.handleTyped(object : Typed {
                override fun hello(
                    name: String,
                    fortyTwo: Int,
                    fiftyFour: Long,
                    word: Boolean,
                    twice: Double,
                    half: Float,
                    precise: BigDecimal,
                    big: BigInteger
                ) {
                    nameA.set(name)
                    fortyTwoA.set(fortyTwo)
                    fiftyFourA.set(fiftyFour)
                    wordA.set(word)
                    twiceA.set(twice)
                    halfA.set(half.toDouble())
                    preciseA.set(precise)
                    bigA.set(big)
                }
            })

            requireNotNull(
                Awaitility.await("wait for types")
                .atMost(10, SECONDS)
                .until({
                    bigA.get().intValueExact()
                }) { it == 2 }) { "did not receive types before timeout" }

        }

        assertEquals("John", nameA.get())
        assertEquals(42, fortyTwoA.get())
        assertEquals(54L, fiftyFourA.get())
        assertEquals(true, wordA.get())
        assertEquals(84.0, twiceA.get())
        assertEquals(42.0, halfA.get())
        assertEquals(10, preciseA.get().toInt())
        assertEquals(2, bigA.get().toInt())
    }

    private fun waitForFactoid(topic: String) =
        Awaitility.await("wait for $topic factoid")
            .atMost(10, SECONDS)
            .until({
                factoid[topic]
            }) { it != null }
}
