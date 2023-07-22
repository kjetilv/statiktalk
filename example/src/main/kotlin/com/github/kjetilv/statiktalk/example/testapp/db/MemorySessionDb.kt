package com.github.kjetilv.statiktalk.example.testapp.db

import com.github.kjetilv.statiktalk.example.testapp.shared.SessionDb
import com.github.kjetilv.statiktalk.example.testapp.shared.User

class MemorySessionDb : SessionDb {

    private val logins: MutableMap<User.Id, User> = mutableMapOf()

    private val changes: MutableMap<User.Id, List<User>> = mutableMapOf()

    override fun loggedIn(user: User) {
        logins.compute(user.id) { _, ex ->
            change(ex, user)
        }
    }

    override fun userChange(change: User) {
        changes.compute(change.id) { _, list ->
            list?.plus(change) ?: listOf(change)
        }
    }

    override fun sessions() =
        logins.values.toList().sortedBy(User::userId)
            .map { user ->
                changes[user.id]?.fold(user, this::change) ?: user
            }

    private fun change(
        user: User?,
        update: User
    ) =
        user?.copy(
            returning = update.returning ?: user.returning,
            status = update.status ?: user.status,
            metadata = merge(user.metadata, update.metadata)
        ) ?: update

    private fun merge(
        base: Map<String, String>?,
        change: Map<String, String>?
    ): Map<String, String>? {
        return when {
            base == null -> change
            change == null -> base
            else -> base + change
        }
    }
}
