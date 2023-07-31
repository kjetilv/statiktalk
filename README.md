# statiktalk

Static typing on top of rapids and rivers. A paddle for when you are up creek.

The things we want to type are the messages. We represent a message as
a Kotlin function and its argument names.

Functions can be grouped in interfaces. They can then be shared between
microservices to enable static typing, quick navigation and reliable
tracking of usages and implementations. Just like a statically typed
language!

Microservices can respond to different
messages by registering implementations of the corresponding
signatures. They can send messages by asking for an instance of a given interface to call.

The general idea is to take a function signature's argument names, and map them keys in the rapids world.
Non-nullable arguments are registered as required keys, nullable arguments are registered as interesting
keys.  

## Example

```kotlin
interface HelloWorld {
    @Message
    fun hello(name: String, greeting: String? = null)
}

val helloWorld = object : HelloWorld {
    override fun hello(name: String, greeting: String?) {
        // TODO
    }
}
```

The `@Message` annotation adds extension functions to `RapidsConnection` that will
setup listeners and send messages.

To receive hellos, we can register a listener which will be invoked with any message with the `name` key,
also passing along anything on the `greeting` key:

```kotlin
    rapids.handleHelloWorld(helloWorld)
```

To say hello:

```kotlin
    rapids.helloWorld().hello("Alan")
```

To make it more specific, we can also register on a specific event name:

```kotlin
    rapids.handleHelloWorld(helloWorld, eventName = "myEventName")
```

And send on an event name:

```kotlin
    rapids.helloWorld(eventName = "myEventName").apply {
    hello("Dan")
    hello("Adele")
}
```

We can also use a default, synthetic event name, which will be a concatenation of interface and method names:

```kotlin
interface HelloWorld {
    @Message(syntheticEventName = true)
    fun hello(name: String, greeting: String? = null)
}
```

Or a custom one:

```kotlin
interface HelloWorld {
    @Message(eventName = "socialStuff")
    fun hello(name: String, greeting: String? = null)
}
```

In this case, senders and implementors will agree on the common event name by default, while still
allowing explicit overrides at register/send time.

## Finer points

### Required values

We do required values, statically as well:

```kotlin
    rapids.handleHelloWorld(
        helloWorld,
        reqs = HelloWorldReqs(
                greeting = "howdy"
        )
)
```

### The Context

A `Context` object can be passed along when the receiver sends out messages of its own. It makes sure
all received values are included when forwarding the message, so the familiar pattern of enriching the
message can be used.

For functions that initialize a message flow from the outside, the `Context` does not
yet exist, so it can be made a nullable _last_ argument in case the receiver of the
message wants to pass along its context.

### The innards

[KSP](https://kotlinlang.org/docs/ksp-overview.html) is used to hook into the build, when my old
friend [StringTemplate](https://www.stringtemplate.org/) of
[ANTLR](https://www.antlr.org/) fame generates the code needed to navigate the rapids. 
