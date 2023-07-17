package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.AuthorizedUserLoggedIn
import com.github.kjetilv.statiktalk.example.testapp.shared.UserLoggedIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserLoggedInService(
    private val authorizedUserLoggedIn: AuthorizedUserLoggedIn,
    private val time: () -> Instant
    ) : UserLoggedIn {

    override fun loggedIn(userId: String, context: Context?) {
        context?.set("loginTime", time().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE))
        authorizedUserLoggedIn.authorized(
            userId,
            context!!
        )
    }
}
