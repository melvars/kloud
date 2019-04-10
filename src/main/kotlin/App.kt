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
import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.util.*
import java.util.logging.*

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
        get("/", { ctx -> ctx.render("index.rocker.html") }, roles(Roles.GUEST))

        /**
         * Renders the login page
         */
        get(
            "/login",
            { ctx -> ctx.render("login.rocker.html", model("message", "")) },
            roles(Roles.GUEST)
        )

        /**
         * Endpoint for user authentication
         */
        post("/login", { ctx -> login(ctx) }, roles(Roles.GUEST)) // TODO: brute-force protection

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
        post("/setup", { ctx -> setup(ctx) }, roles(Roles.GUEST))

        /**
         * Sends a json object of filenames in [fileHome]s
         * TODO: Fix possible security issue with "../"
         */
        get("/files/*", { ctx -> crawlFiles(ctx) }, roles(Roles.ADMIN))

        /**
         * Renders the upload rocker template
         */
        get("/upload", { ctx -> ctx.render("upload.rocker.html") }, roles(Roles.USER))

        /**
         * Receives and saves multipart media data
         * TODO: Fix possible security issue with "../"
         */
        post("/upload/*", { ctx -> upload(ctx) }, roles(Roles.ADMIN))
    }
}

/**
 * Sets up the roles with the database and declares the handling of roles
 */
fun roleManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
    val userRole = databaseController.getRole(getUsername(ctx))
    when {
        getUsername(ctx) == ctx.cookieStore("username") ?: "username" -> handler.handle(ctx)
        permittedRoles.contains(userRole) -> handler.handle(ctx)
        //ctx.host()!!.contains("localhost") -> handler.handle(ctx) // DEBUG
        else -> ctx.status(401).result("This site isn't available for you.")
    }
}

/**
 * Gets the username and verifies its identity
 */
fun getUsername(ctx: Context): String {
    return if (databaseController.getUsernameByUUID(ctx.cookieStore("uuid") ?: "uuid")
        == ctx.cookieStore("username") ?: "username"
    ) ctx.cookieStore("username")
    else ""
}

/**
 * Crawls the requested file and either renders the directory view or the file view
 */
fun crawlFiles(ctx: Context) {
    try {
        when {
            File("$fileHome/${ctx.splats()[0]}").isDirectory -> {
                val files = ArrayList<String>()
                Files.list(Paths.get("$fileHome/${ctx.splats()[0]}/")).forEach {
                    val fileName = it.toString()
                        .drop(fileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 2 else 1))
                    val filePath = "$fileHome${it.toString().drop(fileHome.length)}"
                    files.add(if (File(filePath).isDirectory) "$fileName/" else fileName)
                }
                files.sortWith(String.CASE_INSENSITIVE_ORDER)
                ctx.render(
                    "files.rocker.html", model(
                        "files", files,
                        "path", ctx.splats()[0]
                    )
                )
            }
            isHumanReadable("$fileHome/${ctx.splats()[0]}") ->
                ctx.render(
                    "fileview.rocker.html", model(
                        "content", Files.readAllLines(
                            Paths.get("$fileHome/${ctx.splats()[0]}"),
                            Charsets.UTF_8
                        ).joinToString(separator = "\n"),
                        "filename", File("$fileHome/${ctx.splats()[0]}").name,
                        "extension", File("$fileHome/${ctx.splats()[0]}").extension
                    )
                )
            else -> ctx.result(FileInputStream(File("$fileHome/${ctx.splats()[0]}")))
        }
    } catch (_: java.nio.file.NoSuchFileException) {
        throw NotFoundResponse("Error: File or directory does not exist.")
    }
}

/**
 * Saves multipart media data into requested directory
 */
fun upload(ctx: Context) {
    ctx.uploadedFiles("file").forEach { (contentType, content, name, extension) ->
        FileUtil.streamToFile(content, "$fileHome/${ctx.splats()[0]}/$name")
        // databaseController.addFile("$fileHome/${ctx.splats()[0]}/$name", USER???: get by Session)
        ctx.redirect("/upload")
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

/**
 * Checks and verifies users credentials and logs the user in
 */
fun login(ctx: Context) {
    val username = ctx.formParam("username").toString()
    val password = ctx.formParam("password").toString()

    if (databaseController.checkUser(username, password)) {
        ctx.cookieStore("uuid", databaseController.getUUID(username))
        ctx.cookieStore("username", username)
        ctx.render("login.rocker.html", model("message", "Login succeeded!"))
    } else
        ctx.render("login.rocker.html", model("message", "Login failed!"))
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
                ctx.render("setup.rocker.html", model("message", "Setup succeeded!"))
            } else ctx.status(400).render("setup.rocker.html", model("message", "User already exists!"))
        } else ctx.status(400).render("setup.rocker.html", model("message", "Passwords do not match!"))
    } catch (_: Exception) {
        ctx.status(400).render("setup.rocker.html", model("message", "An error occurred!"))
    }
}

/**
 * Declares the roles in which a user can be in
 */
enum class Roles : Role {
    ADMIN, USER, GUEST
}
