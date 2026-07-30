package com.wisecoders.jdbc.mongodb.wrappers

import com.mongodb.ConnectionString
import com.mongodb.client.ListDatabasesIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoIterable
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.ScanStrategy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.Properties
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.conversions.Bson
import org.slf4j.Logger

/**
 * Copyright Wise Coders GmbH. The MongoDB JDBC driver is build to be used with  [DbSchema Database Designer](https://dbschema.com)
 * Free to use by everyone, code modifications allowed only to the  [public repository](https://github.com/wise-coders/mongodb-jdbc-driver)
 */
class WrappedMongoClient(
    uri: String,
    prop: Properties?,
    databaseName: String?,
    scanStrategy: ScanStrategy,
    expandResultSet: Boolean,
    sortFields: Boolean,
) {
    private val mongoClient: MongoClient
    private val databaseName: String?
    val uRI: String
    private val scanStrategy: ScanStrategy

    @JvmField
    val expandResultSet: Boolean
    val sortFields: Boolean

    fun pingServer() {
        mongoClient.listDatabaseNames()
        val command: Bson = BsonDocument("ping", BsonInt64(1))
        try {
            mongoClient.getDatabase(if (!databaseName.isNullOrEmpty()) databaseName else "admin")
                .runCommand(command)
            LOGGER.atInfo().setMessage("Connected successfully to server.").log()
        } catch (e: Throwable) {
            LOGGER.atWarn().setMessage("Initial ping failed, retrying on admin: ${e.message}").setCause(e).log()
        }
        mongoClient.getDatabase("admin").runCommand(command)
        LOGGER.atInfo().setMessage("Connected successfully to server.").log()
    }

    fun close() {
        mongoClient.close()
    }

    fun listDatabaseNames(): MongoIterable<String> {
        return mongoClient.listDatabaseNames()
    }

    fun listDatabases(): ListDatabasesIterable<Document> {
        return mongoClient.listDatabases()
    }

    fun <T> listDatabases(clazz: Class<T>): ListDatabasesIterable<T> {
        return mongoClient.listDatabases(clazz)
    }

    val currentDatabaseName: String
        get() =// SEE THIS TO SEE HOW DATABASE NAME IS USED : http://api.mongodb.org/java/current/com/mongodb/MongoClientURI.html
            databaseName ?: "admin"

    val databaseNames: List<String>
        get() {
            val names: MutableList<String> = ArrayList()
            try {
                // THIS OFTEN THROWS EXCEPTION BECAUSE OF MISSING RIGHTS. IN THIS CASE WE ONLY ADD CURRENT KNOWN DB.
                for (dbName in listDatabaseNames()) {
                    names.add(dbName)
                }
            } catch (ex: Throwable) {
                names.add(currentDatabaseName)
            }
            for (str in createdDatabases) {
                if (!names.contains(str)) {
                    names.add(str)
                }
            }
            return names
        }

    private val cachedDatabases: MutableMap<String, WrappedMongoDatabase> = HashMap()

    init {
        val connectionString: ConnectionString = object : ConnectionString(injectCredentials(uri, prop)) {
            override fun getMaxConnectionIdleTime(): Int {
                return Int.MAX_VALUE
            }

            override fun getUuidRepresentation(): UuidRepresentation {
                return UuidRepresentation.STANDARD
            }
        }

        this.mongoClient = MongoClients.create(connectionString)
        this.databaseName = databaseName
        this.uRI = uri
        this.expandResultSet = expandResultSet
        this.scanStrategy = scanStrategy
        this.sortFields = sortFields
        databaseNames
    }

    fun getDatabase(dbName: String): WrappedMongoDatabase {
        if (cachedDatabases.containsKey(dbName)) {
            return cachedDatabases[dbName]!!
        }
        val db = WrappedMongoDatabase(mongoClient.getDatabase(dbName), scanStrategy, sortFields)
        cachedDatabases[dbName] = db
        return db
    }

    val databases: List<WrappedMongoDatabase>
        get() {
            val list: MutableList<WrappedMongoDatabase> =
                ArrayList()

            for (dbName in databaseNames) {
                list.add(getDatabase(dbName))
            }
            return list
        }


    val version: String
        get() = "1.1"


    @Throws(SQLException::class)
    fun getCollectionNames(databaseName: String): List<String> {
        val list: MutableList<String> = ArrayList()
        try {
            val db = getDatabase(databaseName)
            for (name in db.listCollectionNames()) {
                list.add(name)
            }
            list.remove("system.indexes")
            list.remove("system.users")
            list.remove("system.views")
            list.remove("system.version")
        } catch (ex: Throwable) {
            LOGGER.atError().setMessage(
                "Cannot list collection names for $databaseName. "
            ).setCause(ex).log()
            throw SQLException(ex)
        }
        return list
    }

    @Throws(SQLException::class)
    fun getViewNames(databaseName: String): List<String> {
        val list: MutableList<String> = ArrayList()
        try {
            val db = getDatabase(databaseName)
            for (doc in db.listCollections()) {
                if (doc.containsKey("type") && "view" == doc["type"]) {
                    list.add(doc["name"].toString())
                }
            }
        } catch (ex: Throwable) {
            LOGGER.atError().setMessage(
                "Cannot list collection names for $databaseName. "
            ).setCause(ex).log()
            throw SQLException(ex)
        }
        return list
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()

        // USE STATIC SO OPENING A NEW CONNECTION WILL REMEMBER THIS
        @JvmField
        val createdDatabases = mutableListOf<String>()

        /**
         * Embeds the JDBC `user`/`password` properties into the connection string, because
         * [ConnectionString] builds its credential while parsing: a URL with `authMechanism`
         * but no userinfo fails with "username can not be null" before an overridden
         * `getCredential()` could supply the username. Properties take precedence over
         * credentials embedded in the URL.
         */
        internal fun injectCredentials(
            uri: String,
            prop: Properties?,
        ): String {
            val username: String? = prop?.getProperty("user")
            if (username.isNullOrEmpty()) {
                return uri
            }
            val schemeEnd: Int = uri.indexOf("://")
            if (schemeEnd < 0) {
                return uri
            }
            val authorityStart: Int = schemeEnd + "://".length
            var authorityEnd: Int = uri.indexOfAny(charArrayOf('/', '?'), authorityStart)
            if (authorityEnd < 0) {
                authorityEnd = uri.length
            }
            val atIndex: Int = uri.lastIndexOf('@', authorityEnd - 1)
            val hostStart: Int = if (atIndex >= authorityStart) atIndex + 1 else authorityStart
            val password: String = prop.getProperty("password") ?: ""
            return uri.substring(0, authorityStart) + percentEncode(username) + ":" + percentEncode(password) + "@" + uri.substring(hostStart)
        }

        // URLEncoder produces form encoding; the MongoDB URI parser expects percent
        // encoding, where '+' stays literal and a space is %20.
        private fun percentEncode(
            value: String,
        ): String {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
        }
    }
}
