package space.anity

import at.favre.lib.crypto.bcrypt.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.joda.time.*
import java.sql.*
import java.util.*
import java.util.logging.*

class DatabaseController(dbFileLocation: String = "main.db") {
    val db: Database = Database.connect("jdbc:sqlite:$dbFileLocation", "org.sqlite.JDBC")
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Database table indexing the file locations
     */
    object FileLocation : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val location = text("location").uniqueIndex()
        val userId = integer("userId").references(UserData.id)
    }

    /**
     * Database table indexing the users with their regarding passwords
     */
    object UserData : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val username = varchar("username", 24).uniqueIndex()
        val password = varchar("password", 64)
        val uuid = varchar("uuid", 64)
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
        val initialUse = bool("initialUse").default(true).primaryKey()
        val isSetup = bool("isSetup").default(false).primaryKey()
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
                    it[uuid] = UUID.randomUUID().toString()
                }[UserData.id]

                UserRoles.insert { roles ->
                    roles[userId] = usersId!!
                    roles[roleId] = RolesData.select { RolesData.role eq roleString }.map { it[RolesData.id] }[0]
                }
                true
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                log.warning("User already exists!")
                false
            }
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
     * Returns the corresponding username using [uuid]
     */
    fun getUserIdByUUID(uuid: String): Int {
        return transaction {
            try {
                UserData.select { UserData.uuid eq uuid }.map { it[UserData.id] }[0]
            } catch (_: Exception) {
                -1
            }
        }
    }

    /**
     * Returns the corresponding uuid using [usernameString]
     */
    fun getUUID(usernameString: String): String {
        return transaction {
            try {
                UserData.select { UserData.username eq usernameString }.map { it[UserData.uuid] }[0]
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
                    when (it) {
                        "GUEST" -> {
                            userRoles.add(Roles.GUEST)
                        }
                        "USER" -> {
                            userRoles.add(Roles.GUEST)
                            userRoles.add(Roles.USER)
                        }
                        "ADMIN" -> {
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
    fun addFile(fileLocation: String, usersId: Int) {
        transaction {
            try {
                FileLocation.insert {
                    it[location] = fileLocation
                    it[userId] = usersId
                }
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                log.warning("File already exists!")
            }
        }
    }

    /**
     * Removes the file from the database
     */
    fun deleteFile(fileLocation: String, userId: Int) {
        transaction {
            try {
                FileLocation.deleteWhere { (FileLocation.location eq fileLocation) and (FileLocation.userId eq userId) }
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                log.warning("File does not exist!")
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
                it[General.isSetup] = true
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
}
