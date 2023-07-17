package com.github.kjetilv.statiktalk.example.testapp.shared

interface SessionDb {

    fun loggedIn(user: User)

    fun userChange(change: User)

    fun sessions(): List<User>
}
