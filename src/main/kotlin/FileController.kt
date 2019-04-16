package space.anity

import io.javalin.*
import io.javalin.core.util.*
import io.javalin.rendering.template.*
import java.io.*
import java.nio.charset.*
import java.nio.file.*
import java.text.*
import java.util.*

class FileController {
    /**
     * Crawls the requested file and either renders the directory view or the file view
     */
    fun crawl(ctx: Context) {
        try {
            val usersFileHome = "$fileHome/${userHandler.getVerifiedUserId(ctx)}"
            File(usersFileHome).mkdirs()
            when {
                File("$usersFileHome/${ctx.splats()[0]}").isDirectory -> {
                    val files = ArrayList<Array<String>>()
                    Files.list(Paths.get("$usersFileHome/${ctx.splats()[0]}/")).forEach {
                        val fileName = it.toString()
                            .drop(usersFileHome.length + (if (ctx.splats()[0].isNotEmpty()) ctx.splats()[0].length + 2 else 1))
                        val filePath = "$usersFileHome${it.toString().drop(usersFileHome.length)}"
                        val file = File(filePath)
                        val fileSize = if (file.isDirectory) getDirectorySize(file) else file.length()
                        files.add(
                            // TODO: Clean up file array responses
                            arrayOf(
                                if (file.isDirectory) "$fileName/" else fileName,
                                humanReadableBytes(fileSize),
                                SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(file.lastModified()).toString(),
                                if (file.isDirectory) "true" else isHumanReadable(file).toString(),
                                fileSize.toString(), // unformatted file size
                                file.lastModified().toString() // unformatted last modified date
                            )
                        )
                    }
                    //files.sortWith(String.CASE_INSENSITIVE_ORDER) // TODO: Reimplement file array sorting in backend
                    ctx.render(
                        "files.rocker.html", TemplateUtil.model(
                            "files", files,
                            "path", ctx.splats()[0]
                        )
                    )
                }
                isHumanReadable(File("$usersFileHome/${ctx.splats()[0]}")) ->
                    ctx.render(
                        "fileview.rocker.html", TemplateUtil.model(
                            "content", Files.readAllLines(
                                Paths.get("$usersFileHome/${ctx.splats()[0]}"),
                                Charsets.UTF_8
                            ).joinToString(separator = "\n"),
                            "filename", File("$usersFileHome/${ctx.splats()[0]}").name,
                            "extension", File("$usersFileHome/${ctx.splats()[0]}").extension
                        )
                    )
                else -> ctx.result(FileInputStream(File("$usersFileHome/${ctx.splats()[0]}")))
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
            val path = "${ctx.splats()[0]}/$name"
            FileUtil.streamToFile(content, "$fileHome/${userHandler.getVerifiedUserId(ctx)}/$path")
            databaseController.addFile(path, userHandler.getVerifiedUserId(ctx))
        }
    }

    /**
     * Deletes the requested file
     */
    fun delete(ctx: Context) {
        val userId = userHandler.getVerifiedUserId(ctx)
        if (userId > 0) {
            val path = ctx.splats()[0]
            File("$fileHome/$userId/$path").delete()
            databaseController.deleteFile(path, userId)
        }
    }

    /**
     * Shares the requested file via the accessId
     */
    fun share(ctx: Context) {
        val userId = userHandler.getVerifiedUserId(ctx)
        if (userId > 0) {
            val accessId = databaseController.getAccessId(ctx.splats()[0], userId)
            ctx.result("${ctx.host()}/shared?id=$accessId")
        }
    }

    /**
     * Renders the shared file
     */
    fun renderShared(ctx: Context) {
        val accessId = ctx.queryParam("id").toString()
        val sharedFileData = databaseController.getSharedFile(accessId)
        if (sharedFileData.first > 0 && sharedFileData.second.isNotEmpty()) {
            val sharedFileLocation = "$fileHome/${sharedFileData.first}/${sharedFileData.second}"
            if (isHumanReadable(File(sharedFileLocation))) {
                ctx.render(
                    "fileview.rocker.html", TemplateUtil.model(
                        "content", Files.readAllLines(
                            Paths.get(sharedFileLocation),
                            Charsets.UTF_8
                        ).joinToString(separator = "\n"),
                        "filename", File(sharedFileLocation).name,
                        "extension", File(sharedFileLocation).extension
                    )
                )
            } else ctx.result(FileInputStream(File(sharedFileLocation)))  // TODO: other file types
        } else {
            log.info("Unknown file!")
        }
    }

}
