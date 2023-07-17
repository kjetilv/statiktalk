package com.github.kjetilv.statiktalk.example.testapp.microservices

interface SessionDb {
    fun loggedIn(user: User)
    fun userChange(change: User)
    fun sessions(): List<User>
}
