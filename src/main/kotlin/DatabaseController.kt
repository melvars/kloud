package space.anity

import at.favre.lib.crypto.bcrypt.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.sql.*
import java.util.logging.*

class DatabaseController(dbFileLocation: String = "main.db") {
    val db: Database = Database.connect("jdbc:sqlite:$dbFileLocation", "org.sqlite.JDBC")
    private val log = Logger.getLogger(this.javaClass.name)

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
        val id = integer("id").autoIncrement().primaryKey()
        val username = varchar("username", 24).uniqueIndex()
        val password = varchar("password", 64)
        val role = varchar("role", 64).default("USER")
    }

    /**
     * Database table storing general data/states
     */
    object General : Table() {
        val initialUse = integer("initialUse").default(1).primaryKey()
    }

    init {
        // Create connection
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        // Add tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(FileLocation, UserData, General)
        }
    }

    /**
     * Creates the user in the database using username, password and the role
     */
    fun createUser(usernameString: String, passwordString: String, roleString: String) {
        transaction {
            try {
                UserData.insert {
                    it[username] = usernameString
                    it[password] = BCrypt.withDefaults().hashToString(12, passwordString.toCharArray())
                    it[role] = roleString
                }
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                log.warning("User already exists!")
            }
        }
    }

    /**
     * Tests whether the password [passwordString] of the user [usernameString] is correct
     */
    fun checkUser(usernameString: String, passwordString: String): Boolean {
        return transaction {
            val passwordHash = UserData.select { UserData.username eq usernameString }.map { it[UserData.password] }[0]
            BCrypt.verifyer().verify(passwordString.toCharArray(), passwordHash).verified
        }
    }

    /**
     * Returns the corresponding role using [usernameString]
     */
    fun getRole(usernameString: String): Roles {
        return transaction {
            val role = UserData.select { UserData.username eq usernameString }.map { it[UserData.role] }[0]
            if (role == "ADMIN") Roles.ADMIN else Roles.USER
        }
    }
}
