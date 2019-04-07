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
import java.util.logging.*

const val fileHome = "files"
val databaseController = DatabaseController()
private val log = Logger.getLogger("App.kt")

fun main() {
    val app = Javalin.create()
        .enableStaticFiles("../resources/")
        .accessManager { handler, ctx, permittedRoles -> setupRoles(handler, ctx, permittedRoles) }
        .start(7000)

    // Set up templating
    RockerRuntime.getInstance().isReloading = true
    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )

    // Only for testing purposes
    databaseController.createUser("melvin", "supersecure", "ADMIN")

    app.routes {
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
fun setupRoles(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
    val userRole = databaseController.getRole("melvin")
    when {
        permittedRoles.contains(userRole) -> handler.handle(ctx)
        ctx.host()!!.contains("localhost") -> handler.handle(ctx)
        else -> ctx.status(401).json("This site isn't available for you.")
    }
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
 * Declares the roles in which a user can be in
 */
enum class Roles : Role {
    ADMIN, USER, GUEST
}
