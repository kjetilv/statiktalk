# statiktalk

Static typing on top of rapids&rivers.

The things we want to type are the messages. We represent a message as
a Kotlin function signature, consisting of:

* Function name
* Argument names

Functions can be grouped in interfaces. They can then be shared between
microservices to enable static typing, quick navigation and reliable 
tracking of usages and implementations. Just like a statically typed
language!

Microservices can respond to different
messages by registering implementations of the corresponding 
signatures. They can send messages by asking for
an instance of a given interface, and functions on it.

## Example

```kotlin
package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Factoids {

    @Message(eventName = "factoids'r'us")
    fun annoyWith(
        subjectMatter: String,
        interestingFact: String,
        aside: String? = null,
        context: Context? = null
    )
}

```

To receive factoids:

```kotlin
    rapids.handleFactoids(object : Factoids {
        override fun annoyWith(
            subjectMatter: String,
            interestingFact: String,
            aside: String?,
            context: Context?
        ) {
            myImplementation()
        }
    })

```

To send them:

```kotlin
    rapids.factoids().annoyWith(
      "Static typing", 
      "It's recommended by 9 out of 10 dentists", 
      aside = "Prevents gnashing of the teeth you see"
    )
```

The `@Message` annotation adds extension metohds to `RapidsConnection` that will
setup listeners and send messages.

The message sent was:

```json
{
  "@event_name": "factoids'r'us",
  "subjectMatter": "Static typing",
  "interestingFact": "It's recommended by 9 out of 10 dentists",
  "aside": "Prevents gnashing of the teeth you see",
  "@id": "ac8a2ab7-8568-4d94-a9d7-19f76b8e0db2",
  "@opprettet": "2023-07-17T21:35:23.722431",
  "system_read_count": 0,
  "system_participating_services": [
    {
      "id": "ac8a2ab7-8568-4d94-a9d7-19f76b8e0db2",
      "time": "2023-07-17T21:35:23.722431"
    }
  ]
}
```

## Finer points

### Nullable arguments

Nullable arguments are registered as interesting keys, non-nullables as required keys.

### The Context

The `Context` object can be passed along when the receiver sends out messages of its own.  It makes sure 
all received values are included when forwarding the message, so the familiar pattern of enriching the 
message can be used.

For functions that initialize a message flow from the outside, the `Context` does not 
yet exist, so it can be made a nullable _last_ argument in case the receiver of the
message wants to pass along its context. 

## Generated code:

Here is the sender:

```kotlin
@Suppress("unused")
fun RapidsConnection.factoids(): Factoids =
    FactoidsSendMediator(this)

private class FactoidsSendMediator(
    rapidsConnection: RapidsConnection
) : SendMediatorBase(rapidsConnection), Factoids {

    override fun annoyWith(
        subjectMatter: String,
        interestingFact: String,
        aside: String?,
        context: com.github.kjetilv.statiktalk.api.Context?
    ) {
        send0(
            context,
            "@event_name" to "factoids'r'us",
            "subjectMatter" to subjectMatter,
            "interestingFact" to interestingFact,
            "aside" to aside)
    }
}
```

Here is the receiver:

```kotlin
@Suppress("unused")
fun RapidsConnection.handleFactoids(
    factoids: Factoids,
    vararg additionalKeys: String
) {
    FactoidsReceiveMediatorAnnoyWith(factoids)
        .listenTo(
            this,
            additionalKeys.toList()
        )
}

private class FactoidsReceiveMediatorAnnoyWith(
    private val factoids: Factoids
) : ReceiveMediatorBase() {

    override fun listenTo(connection: RapidsConnection, additionalKeys: List<String>) {
        val requiredKeys = listOf(
            "subjectMatter",
            "interestingFact"
        )
        val interestingKeys = listOf(
            "aside"
        )
        listen(
            connection,
            "factoids'r'us",
            requiredKeys,
            interestingKeys,
            additionalKeys
        )
    }

    @Suppress("RedundantNullableReturnType", "RedundantSuppression")
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val subjectMatter: String = packet["subjectMatter"].textValue()
        val interestingFact: String = packet["interestingFact"].textValue()

        val aside: String? = packet["aside"].textValue()

        factoids.annoyWith(subjectMatter, interestingFact, aside, context(packet, context))
    }
}
```

[KSP](https://kotlinlang.org/docs/ksp-overview.html) is used to generate the code,
alongside my old friend [StringTemplate](https://www.stringtemplate.org/) of
[ANTLR](https://www.antlr.org/) fame. 
