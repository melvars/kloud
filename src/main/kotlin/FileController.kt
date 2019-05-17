package space.anity

import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.template.TemplateUtil.model
import org.slf4j.*
import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.text.*

class FileController {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    /**
     * Crawls the requested file and either renders the directory view or the file view
     */
    fun crawl(ctx: Context) {
        try {
            val usersFileHome = "$fileHome/${userHandler.getVerifiedUserId(ctx)}"
            val firstParam = ctx.splat(0) ?: ""
            val fileLocation = "$usersFileHome/$firstParam"
            File(usersFileHome).mkdirs()
            when {
                ctx.queryParam("raw") != null -> ctx.result(FileInputStream(File(fileLocation)))
                File(fileLocation).isDirectory -> {
                    val files = ArrayList<Array<String>>()
                    Files.list(Paths.get("$usersFileHome/$firstParam/")).forEach {
                        val filename = it.toString()
                            .drop(usersFileHome.length + (if (firstParam.isNotEmpty()) firstParam.length + 2 else 1))
                        val filePath = "$usersFileHome${it.toString().drop(usersFileHome.length)}"
                        files.add(addToFileListing(filePath, filename))
                    }
                    files.sortWith(compareBy { it.first() })
                    ctx.render(
                        "files.rocker.html", model(
                            "files", files,
                            "path", (if (firstParam.firstOrNull() == '/') firstParam.drop(1) else firstParam),
                            "isShared", false,
                            "ctx", ctx
                        )
                    )
                }
                isHumanReadable(File(fileLocation)) -> handleHumanReadableFile(fileLocation, ctx)
                else -> {
                    ctx.contentType(Files.probeContentType(Paths.get(fileLocation)))
                    ctx.result(FileInputStream(File(fileLocation)))
                }
            }
        } catch (_: Exception) {
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
        ctx.uploadedFiles("file").forEach { (_, content, name, _) ->
            val fixedName = name.replace(":", "/") // "fix" for Firefox..
            val userId = userHandler.getVerifiedUserId(ctx)
            var addPath = ""

            fixedName.split("/").forEach {
                addPath += "$it/"
                if (!fixedName.endsWith(it)) databaseController.addFile(addPath, userId, true)
            }

            if (databaseController.addFile(fixedName, userId)) {
                FileUtil.streamToFile(
                    content,
                    "$fileHome/$userId/$fixedName"
                )
            }
        }

        ctx.json("success")
    }

    /**
     * Re-indexes every file in the users directory
     */
    fun indexAll(ctx: Context) {
        val userId = userHandler.getVerifiedUserId(ctx)

        fun recursiveIndex(filePath: String = "") {
            Files.list(Paths.get("$fileHome/$userId$filePath")).forEach {
                val filename = it.toString().drop("$fileHome/$userId".length + 1)

                if (it.toFile().isDirectory) {
                    databaseController.addFile("$filename/", userId, true)
                    recursiveIndex("/$filename")
                } else databaseController.addFile(filename, userId, false)
            }
        }

        recursiveIndex()
    }

    /**
     * Deletes the requested file
     */
    fun delete(ctx: Context) {
        val userId = userHandler.getVerifiedUserId(ctx)
        if (userId > 0) {
            val path = ctx.splat(0) ?: ""
            val file = File("$fileHome/$userId/$path")

            fun deleteDirectory(recursiveFile: File) {
                val fileList = recursiveFile.listFiles()
                if (fileList != null) {
                    for (subFile in fileList) {
                        deleteDirectory(subFile)
                    }
                }
                recursiveFile.delete()
            }

            if (file.isDirectory) {
                deleteDirectory(file)
            } else file.delete()
            databaseController.deleteFile(path, userId)
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
     * Renders a shared file
     */
    fun renderShared(ctx: Context) {
        val sharedFileData = databaseController.getSharedFile(ctx.queryParam("id").toString())
        val fileLocation = sharedFileData.fileLocation
        if (sharedFileData.userId > 0 && fileLocation.isNotEmpty()) {
            val sharedFileLocation = "$fileHome/${sharedFileData.userId}/$fileLocation"
            if (!sharedFileData.isDirectory) {
                if (isHumanReadable(File(sharedFileLocation))) handleHumanReadableFile(sharedFileLocation, ctx)
                else {
                    // TODO: Fix name of downloaded file ("shared")
                    ctx.contentType(Files.probeContentType(Paths.get(sharedFileLocation)))
                    ctx.result(FileInputStream(File(sharedFileLocation)))
                }
            } else {
                val files = ArrayList<Array<String>>()
                Files.list(Paths.get(sharedFileLocation)).forEach {
                    val filename = it.toString()
                        .drop(sharedFileLocation.length)
                    val filePath = "$sharedFileLocation$filename"
                    files.add(addToFileListing(filePath, filename))
                }
                files.sortWith(compareBy { it.first() })
                ctx.render(
                    "files.rocker.html", model(
                        "files", files,
                        "path", (if (fileLocation.firstOrNull() == '/') fileLocation.drop(1) else fileLocation),
                        "isShared", true,
                        "ctx", ctx
                    )
                )
            }
        } else {
            log.warn("Unknown file!")
            throw NotFoundResponse("Shared file couldn't be found.")
        }
    }

    /**
     * Adds a file to the file array used in the file listing view
     */
    private fun addToFileListing(filePath: String, filename: String): Array<String> {
        val file = File(filePath)
        val fileSize = if (file.isDirectory) getDirectorySize(file) else file.length()
        return arrayOf(
            // TODO: Clean up array responses
            if (file.isDirectory) "$filename/" else filename,
            humanReadableBytes(fileSize),
            SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(file.lastModified()).toString(),
            if (file.isDirectory) "true" else isHumanReadable(file).toString(),
            file.isDirectory.toString(),
            fileSize.toString(), // unformatted file size
            file.lastModified().toString() // unformatted last modified date
        )
    }

    /**
     * Handles the rendering of human readable files
     */
    private fun handleHumanReadableFile(filePath: String, ctx: Context) {
        ctx.render(
            "fileview.rocker.html", model(
                "content", Files.readAllLines(
                    Paths.get(filePath),
                    Charsets.UTF_8
                ).joinToString(separator = "\n"),
                "filename", File(filePath).name,
                "extension", File(filePath).extension,
                "ctx", ctx
            )
        )
    }

    /**
     * Returns the access id of the directory
     */
    fun handleSharedFile(ctx: Context) {
        val filename = ctx.formParam("filename").toString()
        val accessId = ctx.formParam("accessId").toString()
        ctx.result(databaseController.getAccessIdOfDirectory(filename, accessId))
    }
}
