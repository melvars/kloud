package space.anity

import at.favre.lib.crypto.bcrypt.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
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
        val username = varchar("username", 24)
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
     * Database table storing general data/states
     */
    object General : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val initialUse = integer("initialUse").default(1).primaryKey()
        val isSetup = integer("isSetup").default(0).primaryKey()
    }

    init {
        // Create connection
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        // Add tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(FileLocation, UserData, UserRoles, RolesData, General)
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
     * Returns the corresponding username using [uuid]
     */
    fun getUsernameByUUID(uuid: String): String {
        return transaction {
            try {
                UserData.select { UserData.uuid eq uuid }.map { it[UserData.username] }[0]
            } catch (_: Exception) {
                ""
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
     * Returns the corresponding role using [usernameString]
     */
    fun getRole(usernameString: String): Roles {
        return transaction {
            try {
                val userId = UserData.select { UserData.username eq usernameString }.map { it[UserData.id] }[0]
                val userRoleId = UserRoles.select { UserRoles.userId eq userId }.map { it[UserRoles.roleId] }[0]
                val userRole = RolesData.select { RolesData.id eq userRoleId }.map { it[RolesData.role] }[0]
                if (userRole == "ADMIN") Roles.ADMIN else Roles.USER
            } catch (_: Exception) {
                Roles.GUEST
            }
        }
    }

    /**
     * Adds the uploaded file to the database
     */
    fun addFile(fileLocation: String, usernameString: String) {
        transaction {
            try {
                FileLocation.insert {
                    it[location] = fileLocation
                    it[username] = usernameString
                }
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                log.warning("File already exists!")
            }
        }
    }

    /**
     * Checks whether the site has been set up
     */
    fun isSetup(): Boolean {
        return transaction {
            try {
                General.selectAll().map { it[General.isSetup] }[0] == 1
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
            General.update({ General.initialUse eq 0 }) {
                it[General.isSetup] = 1
            }
        }
    }

    /**
     * Initializes the database
     */
    fun initDatabase() {
        val initialUseRow = transaction { General.selectAll().map { it[General.initialUse] } }
        if (initialUseRow.isEmpty() || initialUseRow[0] == 1) {
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
                    it[initialUse] = 0
                }
            }
        } else {
            log.info("Already initialized Database.")
        }
    }
}
