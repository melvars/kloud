package space.anity

import com.fizzed.rocker.Rocker
import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.*
import java.nio.file.*
import java.sql.Connection


fun main(args: Array<String>) {

    // TODO outsource to class
    val db : Database = Database.connect("jdbc:sqlite:main.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.createMissingTablesAndColumns(FileLocation, UserData, General)
    }


    val fileHome = "files"
    /*JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )*/

    val app = Javalin.create().enableStaticFiles("../resources/").start(7000)


    // TODO: Fix rocker templating
    app.get("/test") { ctx ->
        val templateTest = Rocker.template("test.rocker.html").bind("message", "Testy testy!").render().toString()
        println(templateTest)
        // ctx.render("views/test.rocker.html", model("message", "Testy testy!"))
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

    app.get("/upload") { ctx -> ctx.redirect("/views/upload.html") }

    // TODO: Fix possible security issue with "../"
    app.post("/api/upload") { ctx ->
        ctx.uploadedFiles("files").forEach { (contentType, content, name, extension) ->
            if (ctx.queryParam("dir") !== null) {
                FileUtil.streamToFile(content, "files/${ctx.queryParam("dir")}/$name")
                ctx.redirect("/views/upload.html")
            } else
                throw BadRequestResponse("Error: Please enter a filename.")
        }
    }
}

// DB tables
object FileLocation : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val location = text("location")
}

object UserData : Table() {
    // only for multiple users: val id = integer("id").autoIncrement().primaryKey()
    val uname = varchar("uname", 24).primaryKey()  // remove if ID
    val pwd = varchar("pwd", 64)
}

object General : Table() {
    // redundant: val id = integer("id").autoIncrement().primaryKey()
    val isSetup = integer("isSetup").primaryKey()  // remove pKey if ID  // boolean -> 0:1
    // TODO if not isSetup show other front page
}



