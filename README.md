# statiktalk

Static typing on top of rapids and rivers. A paddle for when you are up **** creek.

We want to type the message. So we represent a message as a Kotlin function and its argument names. A function corresponds to a river.

In Kotlin, functions can be grouped in interfaces, which can then be shared between
microservices to enable static typing, quick navigation and reliable
tracking of usages and implementations. Just like a statically typed
language!

Microservices can respond to different messages by offering implementations of the corresponding
interfaces. They can send messages by asking for an instance that performs a messages send.

The general idea is to take a function signature's argument names, and map them to keys in the rapids world.  
The function's name optionally maps to the event name. Non-nullable arguments are registered as required keys, nullable arguments are registered as interesting keys.

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
Or:

```kotlin
rapids.helloWorld { 
    hello("Alan")
}
```

To make it more specific, we can also register on a specific event name:

```kotlin
rapids.handleHelloWorld(helloWorld, eventName = "myEventName")
```

And send on an event name:

```kotlin
rapids.helloWorld(eventName = "myEventName") {
    hello("Adele")
    hello("Dan")
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

In this case, senders and implementors will agree on the common event name by default.  Explicit overrides at register/send time will still take precedence.

## Finer points

### Required/rejected values

Also supported:

```kotlin
rapids.handleHelloWorld(
    helloWorld,
    reqs = HelloWorldReqs(
        name = notValue("Bjarne"),
        greeting = value("Davs")
    )
)
```

### Encriching Messages: The Context

Out-of-band fields on the message (i.e. fields that are in the message but not in the signature) can
be preserved by accepting a `Context` as the last parameter.  This can be passed along to a function, provided it also supports it. 

It makes sure all received values are included when forwarding the message, so the familiar pattern of enriching the message is possible.

> For functions that initialize a message flow from the outside, the `Context` does not
>  yet exist, so it can be made a nullable _last_ argument in case the receiver of the
>  message wants to pass along its context.

### Unsupported

Disallowing fields/values have no natural static counterpart, yet. Maybe a hook for adding custom 
message validations is the best approach for fine-tuned message flow.

### The innards

[KSP](https://kotlinlang.org/docs/ksp-overview.html) is used to hook into the build, when my old
friend [StringTemplate](https://www.stringtemplate.org/) of
[ANTLR](https://www.antlr.org/) fame generates the code needed to navigate the rapids. 
