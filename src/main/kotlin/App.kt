package space.anity

import com.fizzed.rocker.*
import com.fizzed.rocker.runtime.*
import io.javalin.*
import io.javalin.Handler
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import io.javalin.security.*
import io.javalin.security.SecurityUtil.roles
import org.joda.time.*
import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.text.*
import java.util.*
import java.util.logging.*
import kotlin.math.*

const val fileHome = "files"
val databaseController = DatabaseController()
private val log = Logger.getLogger("App.kt")

fun main() {
    val app = Javalin.create()
        .enableStaticFiles("../resources/")
        .accessManager { handler, ctx, permittedRoles -> roleManager(handler, ctx, permittedRoles) }
        .start(7000)

    // Set up templating
    RockerRuntime.getInstance().isReloading = true
    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )

    databaseController.initDatabase()

    app.routes {
        /**
         * Main page
         * TODO: Create landing page
         */
        get(
            "/",
            { ctx ->
                ctx.render(
                    "index.rocker.html",
                    model("username", databaseController.getUsername(getVerifiedUserId(ctx)))
                )
            },
            roles(Roles.GUEST)
        )

        /**
         * Renders the login page
         */
        get("/login", { ctx ->
            if (getVerifiedUserId(ctx) > 0) ctx.redirect("/")
            else ctx.render(
                "login.rocker.html",
                model("message", "", "counter", 0)
            )
        }, roles(Roles.GUEST))

        /**
         * Endpoint for user authentication
         */
        post("/login", ::login, roles(Roles.GUEST))

        /**
         * Logs the user out
         */
        get("/logout", ::logout, roles(Roles.USER))

        /**
         * Renders the setup page (only on initial use)
         */
        get("/setup", { ctx ->
            if (databaseController.isSetup()) ctx.redirect("/")
            else ctx.render(
                "setup.rocker.html",
                model("message", "")
            )
        }, roles(Roles.GUEST))

        /**
         * Endpoint for setup (only on initial use)
         */
        post("/setup", ::setup, roles(Roles.GUEST))

        /**
         * Renders the file list view
         * TODO: Fix possible security issue with "../"
         */
        get("/files/*", ::crawlFiles, roles(Roles.USER))

        /**
         * Receives and saves multipart media data
         * TODO: Fix possible security issue with "../"
         */
        post("/upload/*", ::upload, roles(Roles.USER))
    }
}

/**
 * Sets up the roles with the database and declares the handling of roles
 */
fun roleManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
    when {
        getVerifiedUserId(ctx) == ctx.cookieStore("userId") ?: "userId" -> handler.handle(ctx)
        databaseController.getRoles(getVerifiedUserId(ctx)).any { it in permittedRoles } -> handler.handle(ctx)
        //ctx.host()!!.contains("localhost") -> handler.handle(ctx) // DEBUG
        else -> ctx.status(401).redirect("/login")
    }
}

/**
 * Gets the username and verifies its identity
 */
fun getVerifiedUserId(ctx: Context): Int {
    return if (databaseController.getUserIdByUUID(ctx.cookieStore("uuid") ?: "uuid")
        == ctx.cookieStore("userId") ?: "userId"
    ) ctx.cookieStore("userId")
    else -1
}

/**
 * Crawls the requested file and either renders the directory view or the file view
 */
fun crawlFiles(ctx: Context) {
    try {
        val usersFileHome = "$fileHome/${getVerifiedUserId(ctx)}"
        File(usersFileHome).mkdirs()
        when {
            File("$usersFileHome/${ctx.splats()[0]}").isDirectory -> {
                val files = ArrayList<Array<String>>()
                Files.list(Paths.get("$usersFileHome/${ctx.splats()[0]}/")).forEach {
                    val fileName = it.toString()
                        .drop(usersFileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 2 else 1))
                    val filePath = "$usersFileHome${it.toString().drop(usersFileHome.length)}"
                    files.add(
                        arrayOf(
                            if (File(filePath).isDirectory) "$fileName/" else fileName,
                            humanReadableBytes(File(filePath).length()),
                            SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(File(filePath).lastModified()).toString(),
                            if (File(filePath).isDirectory) "true" else isHumanReadable(filePath).toString()
                        )
                    )
                }
                //files.sortWith(String.CASE_INSENSITIVE_ORDER)
                ctx.render(
                    "files.rocker.html", model(
                        "files", files,
                        "path", ctx.splats()[0]
                    )
                )
            }
            isHumanReadable("$usersFileHome/${ctx.splats()[0]}") ->
                ctx.render(
                    "fileview.rocker.html", model(
                        "content", Files.readAllLines(
                            Paths.get("$usersFileHome/${ctx.splats()[0]}"),
                            Charsets.UTF_8
                        ).joinToString(separator = "\n"),
                        "filename", File("$usersFileHome/${ctx.splats()[0]}").name,
                        "extension", File("$usersFileHome/${ctx.splats()[0]}").extension
                    )
                )
            else -> ctx.result(FileInputStream(File("$usersFileHome/${ctx.splats()[0]}")))
        }
    } catch (_: Exception) {
        throw NotFoundResponse("Error: File or directory does not exist.")
    }
}

/**
 * Saves multipart media data into requested directory
 */
fun upload(ctx: Context) {
    ctx.uploadedFiles("file").forEach { (_, content, name, _) ->
        val path = "$fileHome/${getVerifiedUserId(ctx)}/${ctx.splats()[0]}/$name"
        FileUtil.streamToFile(content, path)
        databaseController.addFile(path, getVerifiedUserId(ctx))
    }
}

/**
 * Checks whether the file is binary or human-readable (text)
 */
private fun isHumanReadable(filePath: String): Boolean {
    val file = File(filePath)
    val input = FileInputStream(file)
    var size = input.available()
    if (size > 1000) size = 1000
    val data = ByteArray(size)
    input.read(data)
    input.close()
    val text = String(data, Charset.forName("ISO-8859-1"))
    val replacedText = text.replace(
        ("[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîôÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]").toRegex(),
        ""
    )
    val d = (text.length - replacedText.length).toDouble() / text.length.toDouble()
    return d > 0.95
}

fun humanReadableBytes(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

/**
 * Checks and verifies users credentials and logs the user in
 */
fun login(ctx: Context) {
    if (getVerifiedUserId(ctx) > 0) ctx.redirect("/")

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
            ctx.cookieStore("uuid", databaseController.getUUID(username))
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
    if (databaseController.isSetup()) ctx.render(
        "setup.rocker.html",
        model("message", "Setup process already finished!")
    ) else {
        try {
            val username = ctx.formParam("username").toString()
            val password = ctx.formParam("password").toString()
            val verifyPassword = ctx.formParam("verifyPassword").toString()
            if (password == verifyPassword) {
                if (databaseController.createUser(username, password, "ADMIN")) {
                    databaseController.toggleSetup()
                    ctx.render("setup.rocker.html", model("message", "Setup succeeded!"))
                } else ctx.status(400).render("setup.rocker.html", model("message", "User already exists!"))
            } else ctx.status(400).render("setup.rocker.html", model("message", "Passwords do not match!"))
        } catch (_: Exception) {
            ctx.status(400).render("setup.rocker.html", model("message", "An error occurred!"))
        }
    }
}

/**
 * Declares the roles in which a user can be in
 */
enum class Roles : Role {
    ADMIN, USER, GUEST
}
