package space.anity

import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.template.*
import io.javalin.rendering.template.TemplateUtil.model
import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.text.*
import java.util.logging.*

class FileController {
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Crawls the requested file and either renders the directory view or the file view
     */
    fun crawl(ctx: Context) {
        try {
            val usersFileHome = "$fileHome/${userHandler.getVerifiedUserId(ctx)}"
            val firstParam = ctx.splat(0) ?: ""
            File(usersFileHome).mkdirs()
            when {
                ctx.queryParam("raw") != null -> ctx.result(FileInputStream(File("$usersFileHome/$firstParam")))
                File("$usersFileHome/$firstParam").isDirectory -> {
                    val files = ArrayList<Array<String>>()
                    Files.list(Paths.get("$usersFileHome/$firstParam/")).forEach {
                        val filename = it.toString()
                            .drop(usersFileHome.length + (if (firstParam.isNotEmpty()) firstParam.length + 2 else 1))
                        val filePath = "$usersFileHome${it.toString().drop(usersFileHome.length)}"
                        val file = File(filePath)
                        val fileSize = if (file.isDirectory) getDirectorySize(file) else file.length()
                        files.add(
                            // TODO: Clean up file array responses
                            arrayOf(
                                if (file.isDirectory) "$filename/" else filename,
                                humanReadableBytes(fileSize),
                                SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(file.lastModified()).toString(),
                                if (file.isDirectory) "true" else isHumanReadable(file).toString(),
                                file.isDirectory.toString(),
                                fileSize.toString(), // unformatted file size
                                file.lastModified().toString() // unformatted last modified date
                            )
                        )
                    }
                    files.sortWith(compareBy { it.first() })
                    ctx.render(
                        "files.rocker.html", TemplateUtil.model(
                            "files", files,
                            "path", (if (firstParam.firstOrNull() == '/') firstParam.drop(1) else firstParam)
                        )
                    )
                }
                isHumanReadable(File("$usersFileHome/$firstParam")) ->
                    ctx.render(
                        "fileview.rocker.html", TemplateUtil.model(
                            "content", Files.readAllLines(
                                Paths.get("$usersFileHome/$firstParam"),
                                Charsets.UTF_8
                            ).joinToString(separator = "\n"),
                            "filename", File("$usersFileHome/$firstParam").name,
                            "extension", File("$usersFileHome/$firstParam").extension
                        )
                    )
                else -> {
                    ctx.contentType(Files.probeContentType(Paths.get("$usersFileHome/$firstParam")))
                    ctx.result(FileInputStream(File("$usersFileHome/$firstParam")))
                }
            }
        } catch (err: Exception) {
            log.warning(err.toString())
            throw NotFoundResponse("Error: File or directory does not exist.")
        }
    }

    /**
     * Gets directory size recursively
     */
    private fun getDirectorySize(directory: File): Long {
        var length: Long = 0
        for (file in directory.listFiles()!!) {
            length += if (file.isFile) file.length()
            else getDirectorySize(file)
        }
        return length
    }

    /**
     * Checks whether the file is binary or human-readable (text)
     */
    private fun isHumanReadable(file: File): Boolean {
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
     * Converts bytes to human-readable text like 100MiB
     */
    private fun humanReadableBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1] + "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    /**
     * Saves multipart media data into requested directory
     */
    fun upload(ctx: Context) {
        val firstParam = ctx.splat(0) ?: ""
        ctx.uploadedFiles("file").forEach { (_, content, name, _) ->
            val path = if (firstParam.isEmpty()) name else "$firstParam/$name"

            val userId = userHandler.getVerifiedUserId(ctx)
            var addPath = ""
            path.split("/").forEach {
                addPath += "$it/"
                if (!path.endsWith(it)) databaseController.addFile(addPath, userId, true)
            }
            if (databaseController.addFile(path, userId)) {
                FileUtil.streamToFile(
                    content,
                    "$fileHome/$userId/$path"
                )
            }
        }
    }

    /*
    fun indexAll(ctx: Context) {
        Files.list(Paths.get("$fileHome/${getVerifiedUserId(ctx)}").forEach {
            // TODO: Add file indexing function
        }
    }
    */

    /**
     * Deletes the requested file
     */
    fun delete(ctx: Context) { // TODO: Fix deleting of directories
        val userId = userHandler.getVerifiedUserId(ctx)
        if (userId > 0) {
            val path = ctx.splat(0) ?: ""
            File("$fileHome/$userId/$path").delete()  // File.deleteRecursively() kind of "crashes" server but deletes folder :'(
            databaseController.deleteFile(path, userId)  // kind of works for deleting directories
        }
    }

    /**
     * Shares the requested file via the accessId
     */
    fun share(ctx: Context) {
        val userId = userHandler.getVerifiedUserId(ctx)
        val shareType = ctx.queryParam("type").toString()
        val firstParam = ctx.splat(0) ?: ""
        if (userId > 0) {
            val path = "$firstParam${if (shareType == "dir") "/" else ""}"
            val accessId = databaseController.getAccessId(path, userId)
            ctx.result("${ctx.host()}/shared?id=$accessId")
        }
    }

    /**
     * Renders the shared file
     */
    fun renderShared(ctx: Context) {
        val accessId = ctx.queryParam("id").toString()
        val sharedFileData = databaseController.getSharedFile(accessId)
        if (sharedFileData.userId > 0 && sharedFileData.fileLocation.isNotEmpty()) {
            val sharedFileLocation = "$fileHome/${sharedFileData.userId}/${sharedFileData.fileLocation}"
            if (!sharedFileData.isDirectory) {
                if (isHumanReadable(File(sharedFileLocation))) {
                    ctx.render(
                        "fileview.rocker.html", model(
                            "content", Files.readAllLines(
                                Paths.get(sharedFileLocation),
                                Charsets.UTF_8
                            ).joinToString(separator = "\n"),
                            "filename", File(sharedFileLocation).name,
                            "extension", File(sharedFileLocation).extension
                        )
                    )
                } else {
                    val fileProbe = Files.probeContentType(Paths.get(sharedFileLocation)) // is null if file type is not recognized
                    ctx.contentType(fileProbe)
                    ctx.result(FileInputStream(File(sharedFileLocation)))
                }
            } else {
                // TODO: Add support for accessing files in shared directories
                // TODO: Combine the two file-crawling-render functions
                val files = ArrayList<Array<String>>()
                Files.list(Paths.get(sharedFileLocation)).forEach {
                    val filename = it.toString()
                        .drop(sharedFileLocation.length - 1)
                    val filePath = "$sharedFileLocation$filename"
                    val file = File(filePath)
                    val fileSize = if (file.isDirectory) getDirectorySize(file) else file.length()
                    files.add(
                        // TODO: Clean up file array responses
                        arrayOf(
                            if (file.isDirectory) "$filename/" else filename,
                            humanReadableBytes(fileSize),
                            SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(file.lastModified()).toString(),
                            if (file.isDirectory) "true" else isHumanReadable(file).toString(),
                            file.isDirectory.toString(),
                            fileSize.toString(), // unformatted file size
                            file.lastModified().toString() // unformatted last modified date
                        )
                    )
                }
                files.sortWith(compareBy { it.first() })
                ctx.render(
                    "files.rocker.html", TemplateUtil.model(
                        "files", files,
                        "path",
                        (if (sharedFileData.fileLocation.firstOrNull() == '/') sharedFileData.fileLocation.drop(1) else sharedFileData.fileLocation)
                    )
                )
            }
        } else {
            log.info("Unknown file!")
        }
    }
}
