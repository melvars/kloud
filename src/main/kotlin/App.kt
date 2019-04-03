package space.anity

import com.fizzed.rocker.*
import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.*
import io.javalin.rendering.template.TemplateUtil.model
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*
import java.nio.file.*
import java.sql.*


fun main() {
    val app = Javalin.create().enableStaticFiles("../resources/").start(7000)
    val fileHome = "files"

    // TODO: Move to own database class
    val db: Database = Database.connect("jdbc:sqlite:main.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.createMissingTablesAndColumns(FileLocation, UserData, General)
    }

    JavalinRenderer.register(
        FileRenderer { filepath, model -> Rocker.template(filepath).bind(model).render().toString() }, ".rocker.html"
    )

    // TODO: Fix rocker templating
    app.get("/test") { ctx ->
        ctx.render("test.rocker.html", model("message", "Testy testy!"))
    }

    /**
     * Sends a json object of filenames in [fileHome]s
     * TODO: Fix possible security issue with "../"
     */
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

    /**
     * Redirects to corresponding html file
     */
    app.get("/upload") { ctx -> ctx.redirect("/views/upload.html") }

    /**
     * Receives and saves multipart media data
     * TODO: Fix possible security issue with "../"
     */
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

/**
 * Database table for the file location indexing
 */
object FileLocation : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val location = text("location")
}

/**
 * Database table to index the users with their regarding passwords
 */
object UserData : Table() {
    // only for multiple users: val id = integer("id").autoIncrement().primaryKey()
    val username = varchar("username", 24).primaryKey()  // remove if ID
    val password = varchar("password", 64)
}

/**
 * Database table storing general data/states
 */
object General : Table() {
    // redundant: val id = integer("id").autoIncrement().primaryKey()
    val initialUse = integer("initialUse").primaryKey()  // remove pKey if ID  // boolean -> 0:1
    // TODO: If not isSetup show other front page
}



