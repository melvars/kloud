package space.anity

import com.fizzed.rocker.*
import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import java.io.*
import java.nio.file.*

const val fileHome = "files"
val db = DatabaseController()

fun main() {
    val app = Javalin.create().enableStaticFiles("../resources/").start(7000)

    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )

    /**
     * Sends a json object of filenames in [fileHome]s
     * TODO: Fix possible security issue with "../"
     */
    app.get("/files/*") { ctx -> crawlFiles(ctx) }

    /**
     * Redirects upload to corresponding html file
     */
    app.get("/upload") { ctx -> ctx.redirect("/views/upload.html") }

    /**
     * Receives and saves multipart media data
     * TODO: Fix possible security issue with "../"
     */
    app.post("/upload") { ctx -> upload(ctx) }
}

/**
 * Crawls the requested directory and returns filenames in array
 */
fun crawlFiles(ctx: Context) {
    val files = ArrayList<String>()
    try {
        if (File("$fileHome/${ctx.splats()[0]}").isDirectory) {
            Files.list(Paths.get("$fileHome/${ctx.splats()[0]}/")).forEach {
                val fileName = it.toString()
                    .drop(fileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 2 else 1))
                val filePath = "$fileHome${it.toString().drop(fileHome.length)}"
                files.add(if (File(filePath).isDirectory) "$fileName/" else fileName)
                ctx.render("files.rocker.html", model("files", files))
            }
        } else
        // TODO: Fix square brackets at fileview content
            ctx.render(
                "fileview.rocker.html", model(
                    "content", Files.readAllLines(
                        Paths.get("$fileHome/${ctx.splats()[0]}"),
                        Charsets.UTF_8
                    ).toString()
                )
            )
    } catch (_: java.nio.file.NoSuchFileException) {
        throw NotFoundResponse("Error: File or directory does not exist.")
    }
}

/**
 * Saves multipart media data into requested directory
 */
fun upload(ctx: Context) {
    ctx.uploadedFiles("files").forEach { (contentType, content, name, extension) ->
        if (ctx.queryParam("dir") !== null) {
            FileUtil.streamToFile(content, "files/${ctx.queryParam("dir")}/$name")
            ctx.redirect("/views/upload.html")
        } else
            throw BadRequestResponse("Error: Please enter a filename.")
    }
}





