package space.anity

import io.javalin.*
import io.javalin.rendering.template.*
import org.joda.time.*
import java.util.logging.*
import kotlin.math.*

class UserHandler {
    private val log = Logger.getLogger(this.javaClass.name)
    /**
     * Checks and verifies users credentials and logs the user in
     */
    fun login(ctx: Context) {
        if (getVerifiedUserId(ctx) > 0 || !databaseController.isSetup()) ctx.redirect("/")

        val username = ctx.formParam("username").toString()
        val password = ctx.formParam("password").toString()
        val requestIp = ctx.ip()

        val loginAttempts = databaseController.getLoginAttempts(requestIp)
        val lastAttemptDifference =
            if (loginAttempts.isEmpty())
                -1
            else Interval(loginAttempts[loginAttempts.indexOfLast { true }].first.toInstant(), Instant()).toDuration()
                .standardSeconds.toInt()

        var lastHourAttempts = 0
        loginAttempts.forEach {
            val difference = Interval(it.first.toInstant(), Instant()).toDuration().standardMinutes.toInt()
            if (difference < 60) lastHourAttempts += 1
        }
        val nextThreshold = 4f.pow(lastHourAttempts + 1)

        if (lastAttemptDifference > 4f.pow(lastHourAttempts) || lastHourAttempts == 0) {
            if (databaseController.checkUser(username, password)) {
                ctx.cookieStore("verification", databaseController.getVerificationId(username))
                ctx.cookieStore("userId", databaseController.getUserId(username))
                ctx.redirect("/")
            } else {
                databaseController.loginAttempt(DateTime(), requestIp)
                ctx.render(
                    "login.rocker.html",
                    TemplateUtil.model(
                        "message",
                        "Login failed!",
                        "counter", if (nextThreshold / 60 > 60) 3600 else nextThreshold.toInt()
                    )
                )
            }
        } else {
            databaseController.loginAttempt(DateTime(), requestIp)
            ctx.render(
                "login.rocker.html",
                TemplateUtil.model(
                    "message",
                    "Too many request.",
                    "counter", if (nextThreshold / 60 > 60) 3600 else nextThreshold.toInt()
                )
            )
        }
    }

    /**
     * Logs the user out of the system
     */
    fun logout(ctx: Context) {
        ctx.clearCookieStore()
        ctx.redirect("/")
    }

    /**
     * Sets up the general settings and admin credentials
     */
    fun setup(ctx: Context) {
        try {
            val username = ctx.formParam("username").toString()
            val password = ctx.formParam("password").toString()
            val verifyPassword = ctx.formParam("verifyPassword").toString()
            if (password == verifyPassword) {
                if (databaseController.createUser(username, password, "ADMIN")) {
                    databaseController.toggleSetup()
                    ctx.redirect("/login")
                } else ctx.status(400).render(
                    "setup.rocker.html",
                    TemplateUtil.model("message", "User already exists!")
                )
            } else ctx.status(400).render(
                "setup.rocker.html",
                TemplateUtil.model("message", "Passwords do not match!")
            )
        } catch (_: Exception) {
            ctx.status(400).render("setup.rocker.html", TemplateUtil.model("message", "An error occurred!"))
        }
    }

    /**
     * Gets the username and verifies its identity
     */
    fun getVerifiedUserId(ctx: Context): Int {
        return if (databaseController.getUserIdByVerificationId(ctx.cookieStore("verification") ?: "verification")
            == ctx.cookieStore("userId") ?: "userId"
        ) ctx.cookieStore("userId")
        else -1
    }
}
