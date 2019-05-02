package space.anity

import io.javalin.*
import io.javalin.rendering.template.TemplateUtil.model
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
                    model(
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
                model(
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
                    ctx.redirect("/user/login")
                } else ctx.status(400).render(
                    "setup.rocker.html",
                    model("message", "User already exists!")
                )
            } else ctx.status(400).render(
                "setup.rocker.html",
                model("message", "Passwords do not match!")
            )
        } catch (_: Exception) {
            ctx.status(400).render("setup.rocker.html", model("message", "An error occurred!"))
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

    /**
     * Renders the registration page
     */
    fun renderRegistration(ctx: Context) {
        val username = ctx.queryParam("username", "")
        if (username.isNullOrEmpty())
            ctx.status(403).result("Please provide a valid username!")
        else {
            if (databaseController.isUserRegistrationValid(username)) ctx.render(
                "register.rocker.html",
                model(
                    "username", username,
                    "message", ""
                )
            ) else ctx.redirect("/user/login")
        }
    }

    /**
     * Registers a new user
     */
    fun register(ctx: Context) {
        try {
            val username = ctx.formParam("username").toString()
            val password = ctx.formParam("password").toString()
            val verifyPassword = ctx.formParam("verifyPassword").toString()

            if (password == verifyPassword) {
                if (databaseController.isUserRegistrationValid(username)) {
                    databaseController.createUser(username, password, "USER")
                    databaseController.removeRegistrationIndex(username)
                    ctx.redirect("/login")
                } else ctx.status(401).result("This user is not authorized to register.")
            } else ctx.status(400).result("The passwords don't match!")
        } catch (_: Exception) {
            ctx.status(400).result("An exception occured.")
        }
    }
}
