package space.anity

import at.favre.lib.crypto.bcrypt.*
import io.javalin.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.joda.time.*
import java.sql.*
import java.util.logging.*

class DatabaseController(dbFileLocation: String = "main.db") {
    val db: Database = Database.connect("jdbc:sqlite:$dbFileLocation", "org.sqlite.JDBC")
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Database table indexing the file locations
     */
    object FileLocation : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val path = text("path")
        val isDirectory = bool("isDirectory").default(false)
        val userId = integer("userId").references(UserData.id)
        val accessId = varchar("accessId", 64).uniqueIndex()
        val isShared = bool("isShared").default(false)
    }

    /**
     * Database table indexing the users with their regarding passwords
     */
    object UserData : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val username = varchar("username", 24).uniqueIndex()
        val password = varchar("password", 64)
        val verification = varchar("verification", 64).uniqueIndex()
    }

    /**
     * Database table indexing the users with their regarding role (multi line per user)
     */
    object UserRoles : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val userId = integer("userId").references(UserData.id)
        val roleId = integer("role").references(RolesData.id)
    }

    /**
     * Database table indexing the soon-to-be registered users by username
     */
    object UserRegistration : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val username = varchar("username", 24).uniqueIndex()
    }

    /**
     * Database table declaring available roles
     */
    object RolesData : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val role = varchar("roles", 16)
    }

    /**
     * Database table indexing the login attempts of an ip in combination with the timestamp
     */
    object LoginAttempts : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val ip = varchar("ip", 16)
        val timestamp = datetime("timestamp")
    }

    /**
     * Database table storing general data/states
     */
    object General : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val initialUse = bool("initialUse").default(true)
        val isSetup = bool("isSetup").default(false)
    }

    init {
        // Create connection
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        // Add tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                FileLocation,
                UserData,
                UserRoles,
                UserRegistration,
                RolesData,
                LoginAttempts,
                General
            )
        }
    }

    /**
     * Creates the user in the database using username, password and the role
     */
    fun createUser(usernameString: String, passwordString: String, roleString: String): Boolean {
        return transaction {
            try {
                val usersId = UserData.insert {
                    it[username] = usernameString
                    it[password] = BCrypt.withDefaults().hashToString(12, passwordString.toCharArray())
                    it[verification] = generateRandomString()
                }[UserData.id]

                UserRoles.insert { roles ->
                    roles[userId] = usersId!!
                    roles[roleId] = RolesData.select { RolesData.role eq roleString }.map { it[RolesData.id] }[0]
                }
                true
            } catch (_: Exception) {
                log.warning("User already exists!")
                false
            }
        }
    }

    /**
     * Checks whether the user is allowed to register
     * TODO: Verify registration via token
     */
    fun isUserRegistrationValid(usernameString: String): Boolean {
        return transaction {
            try {
                if (UserData.select { UserData.username eq usernameString }.empty()) {
                    val username = UserRegistration.select { UserRegistration.username eq usernameString }.map { it[UserRegistration.username] }[0]
                    username == usernameString
                } else false
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Adds a user to the registration table
     */
    fun indexUserRegistration(ctx: Context) {
        transaction {
            UserRegistration.insert {
                it[username] = ctx.queryParam("username", "").toString()
            }
        }
    }

    /**
     * Removes the registration index of [usernameString]
     */
    fun removeRegistrationIndex(usernameString: String) {
        transaction {
            UserRegistration.deleteWhere { UserRegistration.username eq usernameString }
        }
    }

    /**
     * Tests whether the password [passwordString] of the user [usernameString] is correct
     */
    fun checkUser(usernameString: String, passwordString: String): Boolean {
        return transaction {
            try {
                val passwordHash =
                    UserData.select { UserData.username eq usernameString }.map { it[UserData.password] }[0]
                BCrypt.verifyer().verify(passwordString.toCharArray(), passwordHash).verified
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Returns the corresponding username using [userId]
     */
    fun getUsername(userId: Int): String {
        return transaction {
            try {
                UserData.select { UserData.id eq userId }.map { it[UserData.username] }[0]
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Returns the corresponding username using [verificationId]
     */
    fun getUserIdByVerificationId(verificationId: String): Int {
        return transaction {
            try {
                UserData.select { UserData.verification eq verificationId }.map { it[UserData.id] }[0]
            } catch (_: Exception) {
                -1
            }
        }
    }

    /**
     * Returns the corresponding verification id using [usernameString]
     */
    fun getVerificationId(usernameString: String): String {
        return transaction {
            try {
                UserData.select { UserData.username eq usernameString }.map { it[UserData.verification] }[0]
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Returns the corresponding userId using [usernameString]
     */
    fun getUserId(usernameString: String): Int {
        return transaction {
            try {
                UserData.select { UserData.username eq usernameString }.map { it[UserData.id] }[0]
            } catch (_: Exception) {
                -1
            }
        }
    }

    /**
     * Returns the corresponding role using [userId]
     */
    fun getRoles(userId: Int): List<Roles> {
        return transaction {
            try {
                val userRoleId = UserRoles.select { UserRoles.userId eq userId }.map { it[UserRoles.roleId] }[0]

                val userRoles = mutableListOf<Roles>()
                RolesData.select { RolesData.id eq userRoleId }.map { it[RolesData.role] }.forEach {
                    when (Roles.valueOf(it)) {
                        Roles.GUEST -> {
                            userRoles.add(Roles.GUEST)
                        }
                        Roles.USER -> {
                            userRoles.add(Roles.GUEST)
                            userRoles.add(Roles.USER)
                        }
                        Roles.ADMIN -> {
                            userRoles.add(Roles.GUEST)
                            userRoles.add(Roles.USER)
                            userRoles.add(Roles.ADMIN)
                        }
                    }
                }
                userRoles
            } catch (_: Exception) {
                listOf(Roles.GUEST)
            }
        }
    }

    /**
     * Adds the uploaded file to the database
     */
    fun addFile(fileLocation: String, usersId: Int, isDirectoryBool: Boolean = false): Boolean {
        return transaction {
            try {
                if (FileLocation.select { (FileLocation.path eq fileLocation) and (FileLocation.userId eq usersId) }.empty()) {
                    FileLocation.insert {
                        it[path] = fileLocation
                        it[userId] = usersId
                        it[accessId] = generateRandomString()
                        it[isDirectory] = isDirectoryBool
                    }
                    true
                } else {
                    if (!isDirectoryBool) log.warning("File already exists!")
                    false
                }
            } catch (_: Exception) {
                if (!isDirectoryBool) log.warning("File already exists!")
                false
            }
        }
    }

    /**
     * Removes the file from the database
     */
    fun deleteFile(fileLocation: String, userId: Int) {
        transaction {
            try {
                // TODO: Think of new solution for directory deleting (instead of wildcards)
                FileLocation.deleteWhere { (FileLocation.path like "$fileLocation%") and (FileLocation.userId eq userId) }
            } catch (_: Exception) {
                log.warning("File does not exist!")
            }
        }
    }

    /**
     * Returns the accessId of the given file
     */
    fun getAccessId(fileLocation: String, userId: Int): String {
        return transaction {
            try {
                FileLocation.update({ (FileLocation.userId eq userId) and (FileLocation.path like "$fileLocation%") }) {
                    it[isShared] = true
                }
                FileLocation.select { (FileLocation.path eq fileLocation) and (FileLocation.userId eq userId) }.map { it[FileLocation.accessId] }[0]
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Returns accessId of file in directory
     */
    fun getAccessIdOfDirectory(filename: String, accessId: String): String {
        return transaction {
            try {
                val fileData =
                    FileLocation.select { FileLocation.accessId eq accessId }.map { it[FileLocation.path] to it[FileLocation.userId] to it[FileLocation.isShared] }[0]
                if (fileData.second)
                    FileLocation.select { (FileLocation.path eq "${fileData.first.first}${filename.substring(1)}") and (FileLocation.userId eq fileData.first.second) }.map { it[FileLocation.accessId] }[0]
                else ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Gets the shared file via [accessId]
     */
    fun getSharedFile(accessId: String): ReturnFileData {
        return transaction {
            try {
                if (FileLocation.select { FileLocation.accessId eq accessId }.map { it[FileLocation.isShared] }[0]) {
                    val userId =
                        FileLocation.select { FileLocation.accessId eq accessId }.map { it[FileLocation.userId] }[0]
                    val fileLocation =
                        FileLocation.select { FileLocation.accessId eq accessId }.map { it[FileLocation.path] }[0]
                    val isDir =
                        FileLocation.select { FileLocation.accessId eq accessId }.map { it[FileLocation.isDirectory] }[0]
                    ReturnFileData(userId, fileLocation, isDir)
                } else
                    ReturnFileData(-1, "", false)
            } catch (_: Exception) {
                log.warning("File does not exist!")
                ReturnFileData(-1, "", false)
            }
        }
    }

    /**
     * Checks whether the site has been set up
     */
    fun isSetup(): Boolean {
        return transaction {
            try {
                General.selectAll().map { it[General.isSetup] }[0]
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Toggles the setup state
     */
    fun toggleSetup() {
        transaction {
            General.update({ General.initialUse eq false }) {
                it[isSetup] = true
            }
        }
    }

    /**
     * Adds an login attempt to the database
     */
    fun loginAttempt(dateTime: DateTime, requestIp: String) {
        transaction {
            LoginAttempts.insert {
                it[timestamp] = dateTime
                it[ip] = requestIp
            }
        }
    }

    /**
     * Gets all login attempts of [requestIp]
     */
    fun getLoginAttempts(requestIp: String): List<Pair<DateTime, String>> {
        return transaction {
            LoginAttempts.select { LoginAttempts.ip eq requestIp }
                .map { it[LoginAttempts.timestamp] to it[LoginAttempts.ip] }
        }
    }

    /**
     * Initializes the database
     */
    fun initDatabase() {
        val initialUseRow = transaction { General.selectAll().map { it[General.initialUse] } }
        if (initialUseRow.isEmpty() || initialUseRow[0]) {
            transaction {
                RolesData.insert {
                    it[role] = "ADMIN"
                }
                RolesData.insert {
                    it[role] = "USER"
                }
                RolesData.insert {
                    it[role] = "GUEST"
                }

                UserRoles.insert {
                    it[userId] = 1
                    it[roleId] = 1
                }

                General.insert {
                    it[initialUse] = false
                }
            }
        } else {
            log.info("Already initialized Database.")
        }
    }

    /**
     * Generates a random string with [length] characters
     */
    private fun generateRandomString(length: Int = 64): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

data class ReturnFileData(
    val userId: Int,
    val fileLocation: String,
    val isDirectory: Boolean
)
