package space.anity

import io.javalin.*
import io.javalin.core.util.*
import java.io.*
import java.nio.file.*

fun main(args: Array<String>) {
    val app = Javalin.create().enableStaticFiles("../resources/").start(7000)
    val fileHome = "files"

    app.get("/") { ctx ->
        ctx.result("Hello World")
    }

    app.get("/files/*") { ctx ->
        var files = ""
        try {
            Files.list(Paths.get("$fileHome/${ctx.splats()[0]}/")).forEach {
                val fileName = it.toString()
                    .drop(fileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 1 else 0))
                val filePath = "$fileHome${it.toString().drop(fileHome.length)}"
                files += if (File(filePath).isDirectory) "$fileName/\n" else "$fileName\n"
            }
            ctx.result(files)
        } catch (_: java.nio.file.NoSuchFileException) {
            throw NotFoundResponse("Error: File or directory does not exist.")
        }

        //File("test").writeText(ctx.splat(0)!!)
    }

    app.post("/upload") { ctx ->
        ctx.uploadedFiles("files").forEach { (contentType, content, name, extension) ->
            if (ctx.queryParam("dir") !== null)
                FileUtil.streamToFile(content, "files/${ctx.queryParam("dir")}/$name")
            else
                throw BadRequestResponse("Error: Please enter a filename.")
        }
    }
}
