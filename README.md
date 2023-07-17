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

    @Message(fullEventName = true)
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
