package space.anity

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.sql.*

class DatabaseController(dbFileLocation: String = "main.db") {
    val db: Database

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
        // only for multiple users:
        // val id = integer("id").autoIncrement().primaryKey()
        val username = varchar("username", 24).primaryKey()  // remove .primaryKey(), if id column is used
        val password = varchar("password", 64)
    }

    /**
     * Database table storing general data/states
     */
    object General : Table() {
        val initialUse = integer("initialUse").default(1).primaryKey() // boolean -> 0:1
    }

    init {
        // create connection
        this.db = Database.connect("jdbc:sqlite:$dbFileLocation", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        // add tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(FileLocation, UserData, General)
        }
    }

    // TODO add functions for database usage
}
