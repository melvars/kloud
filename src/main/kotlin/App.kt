package space.anity

import com.fizzed.rocker.*
import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import java.io.*
import java.nio.file.*

fun main(args: Array<String>) {
    val fileHome = "files"
    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )
    val app = Javalin.create().enableStaticFiles("../resources/").start(7000)

    // TODO: Fix rocker templating
    app.get("/test") { ctx ->
        ctx.render("test.rocker.html", model("message", "Testy testy!"))
    }

    // TODO: Fix possible security issue with "../"
    app.get("/api/files/*") { ctx ->
        val files = ArrayList<String>()
        try {
            Files.list(Paths.get("$fileHome/${ctx.splats()[0]}/")).forEach {
                val fileName = it.toString()
                    .drop(fileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 2 else 1))
                val filePath = "$fileHome${it.toString().drop(fileHome.length)}"
                files.add(if (File(filePath).isDirectory) "$fileName/" else fileName)
            }
            ctx.json(files)
        } catch (_: java.nio.file.NoSuchFileException) {
            throw NotFoundResponse("Error: File or directory does not exist.")
        }
    }

    app.get("/upload") { ctx -> ctx.redirect("/upload.html") }

    // TODO: Fix possible security issue with "../"
    app.post("/api/upload") { ctx ->
        ctx.uploadedFiles("files").forEach { (contentType, content, name, extension) ->
            if (ctx.queryParam("dir") !== null) {
                FileUtil.streamToFile(content, "files/${ctx.queryParam("dir")}/$name")
                ctx.redirect("/upload.html")
            } else
                throw BadRequestResponse("Error: Please enter a filename.")
        }
    }
}
