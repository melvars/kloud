package space.anity

import com.fizzed.rocker.*
import com.fizzed.rocker.runtime.*
import io.javalin.*
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import io.javalin.security.*
import io.javalin.security.SecurityUtil.roles
import org.slf4j.*
import java.net.*
import kotlin.system.*

const val debug = false
var silent = true
var port = 7000
// TODO: Add abstract and secure file home support for windows/BSD/macOS
val fileHome = if (System.getProperty("os.name") != "Linux" || debug) "files" else "/usr/share/kloud/files"
val databaseController = DatabaseController()
val userHandler = UserHandler()
val fileController = FileController()
private val log = LoggerFactory.getLogger("App.kt")

fun main(args: Array<String>) {
    val app = startServer(args)
    log.info("Successfully started server on port $port")

    // Set up templating
    RockerRuntime.getInstance().isReloading = false
    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )

    databaseController.initDatabase()

    app.routes {
        /**
         * Normalizes and cleans the requested url
         */
        before("/*") { ctx ->
            if (URI(ctx.url()).normalize().toString() != ctx.url()) ctx.redirect(URI(ctx.url()).normalize().toString())
        }

        /**
         * Renders the static resources (important for deployed jar files)
         */
        get(
            "/css/*", { ctx ->
                ctx.contentType("text/css")
                try {
                    ctx.result(Thread.currentThread().contextClassLoader.getResourceAsStream("css/" + ctx.splat(0)))
                } catch (_: Exception) {
                    throw NotFoundResponse()
                }
            },
            roles(Roles.GUEST, Roles.USER)
        )
        get(
            "/js/*", { ctx ->
                ctx.contentType("text/javascript")
                try {
                    ctx.result(Thread.currentThread().contextClassLoader.getResourceAsStream("js/" + ctx.splat(0)))
                } catch (_: Exception) {
                    throw NotFoundResponse()
                }
            },
            roles(Roles.GUEST, Roles.USER)
        )
        get(
            "/fonts/*", { ctx ->
                try {
                    ctx.result(Thread.currentThread().contextClassLoader.getResourceAsStream("fonts/" + ctx.splat(0)))
                } catch (_: Exception) {
                    throw NotFoundResponse()
                }
            },
            roles(Roles.GUEST, Roles.USER)
        )

        /**
         * Main page
         */
        get("/", { ctx ->
            ctx.render(
                "index.rocker.html",
                model("username", databaseController.getUsername(userHandler.getVerifiedUserId(ctx)), "ctx", ctx)
            )
        }, roles(Roles.GUEST, Roles.USER))

        /**
         * Renders the login page
         */
        get("/user/login", userHandler::renderLogin, roles(Roles.GUEST, Roles.USER))

        /**
         * Endpoint for user authentication
         */
        post("/user/login", userHandler::login, roles(Roles.GUEST))

        /**
         * Logs the user out
         */
        get("/user/logout", userHandler::logout, roles(Roles.USER))

        /**
         * Toggles the users theme
         */
        post("/user/theme", userHandler::toggleTheme, roles(Roles.USER))

        /**
         * Renders the registration page
         */
        get("/user/register", userHandler::renderRegistration, roles(Roles.GUEST))

        /**
         * Registers new user
         */
        post("/user/register", userHandler::register, roles(Roles.GUEST))

        /**
         * Adds part of a new user (username) to database
         */
        post("/user/add", databaseController::indexUserRegistration, roles(Roles.ADMIN))

        /**
         * Renders the admin interface
         */
        get("/admin", userHandler::renderAdmin, roles(Roles.ADMIN))

        /**
         * Renders the setup page (only on initial use)
         */
        get("/setup", userHandler::renderSetup, roles(Roles.GUEST))

        /**
         * Endpoint for setup (only on initial use)
         */
        post("/setup", userHandler::setup, roles(Roles.GUEST))

        /**
         * Renders the file list view
         */
        get("/files/*", fileController::crawl, roles(Roles.USER))

        /**
         * Receives and saves multipart media data
         */
        post("/upload/*", fileController::upload, roles(Roles.USER))

        /**
         * Indexes every file of the user into the database
         */
        get("/index", fileController::indexAll, roles(Roles.USER))

        /**
         * Deletes file
         */
        post("/delete/*", fileController::delete, roles(Roles.USER))

        /**
         * Shares file
         */
        post("/share/*", fileController::share, roles(Roles.USER))

        /**
         * Shares file in directory
         */
        post("/share", fileController::handleSharedFile, roles(Roles.USER))

        /**
         * Shows the shared file
         */
        get("/shared", fileController::renderShared, roles(Roles.GUEST, Roles.USER))
    }
}

/**
 * Sets up the roles with the database and declares the handling of roles
 */
fun roleManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
    if (userHandler.getVerifiedUserId(ctx) == ctx.cookieStore("userId") ?: "userId"
        && databaseController.getRoles(userHandler.getVerifiedUserId(ctx)).any { it in permittedRoles }
    ) handler.handle(ctx)
    else if (userHandler.getVerifiedUserId(ctx) != ctx.cookieStore("userId") ?: "userId"
        && databaseController.getRoles(userHandler.getVerifiedUserId(ctx)).any { it in permittedRoles }
    ) handler.handle(ctx)
    else ctx.status(401).redirect("/user/login")
}

/**
 * Starts the server and parses the command line arguments
 */
fun startServer(args: Array<String>): Javalin {
    var runServer = true

    args.forEachIndexed { index, element ->
        run {
            val wantsDebug = element.startsWith("-d") || element.startsWith("--debug")
            val wantsPort = element.startsWith("-p") || element.startsWith("--port")
            val wantsHelp = element.startsWith("-h") || element.startsWith("--help")

            if (wantsPort) {
                val portArgument = args[index + 1].toInt()
                if (portArgument in 1..65535) port = portArgument
            }
            if (wantsDebug) {
                silent = false
            }
            if (wantsHelp) {
                runServer = false
                log.info(
                    "Help: \n" +
                            "Use -p or --port to specify a port\n" +
                            "Use -d or --debug to enable debug mode\n" +
                            "Use -h or --help to show this help message"
                )
            }
        }
    }

    return if (runServer) {
        try {
            Javalin.create().apply {
                port(port)
                accessManager { handler, ctx, permittedRoles -> roleManager(handler, ctx, permittedRoles) }
                disableStartupBanner()
            }.start()
        } catch (_: Exception) {
            log.info("Port $port already in use!")
            exitProcess(1)
        }
    } else exitProcess(1)
}

/**
 * Declares the roles in which a user can be in
 */
enum class Roles : Role {
    ADMIN, USER, GUEST
}
