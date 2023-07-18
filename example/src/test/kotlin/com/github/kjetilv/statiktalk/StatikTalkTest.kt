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
import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.com.google.common.util.concurrent.AtomicDouble
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.math.BigInteger
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.set

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

    private val factoid = mutableMapOf<String, String>()

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

    @Disabled
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

            requireNotNull(await("wait for types")
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
        await("wait for $topic factoid")
            .atMost(10, SECONDS)
            .until({
                factoid[topic]
            }) { it != null }

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
