package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.Authorization
import com.github.kjetilv.statiktalk.example.testapp.shared.LoginAttempt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LoginAttemptService(
    private val authorization: Authorization,
    private val time: () -> Instant
    ) : LoginAttempt {

    override fun loginAttempted(userId: String, context: Context) {
        context["loginTime"] = time().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        authorization.userLoggedIn(userId, context)
    }
}
