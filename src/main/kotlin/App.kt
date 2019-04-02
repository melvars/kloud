package space.anity

import io.javalin.Javalin
import io.javalin.NotFoundResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val app = Javalin.create().start(7000)
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
            throw NotFoundResponse("File or directory does not exist")
        }

        //File("test").writeText(ctx.splat(0)!!)
    }
}
