package com.wisecoders.jdbc.mongodb

import com.mongodb.client.AggregateIterable
import com.mongodb.client.model.ReplaceOptions
import com.wisecoders.common_jdbc.jvm.result_set.ArrayResultSet
import com.wisecoders.common_jdbc.jvm.result_set.ObjectAsResultSet
import com.wisecoders.common_jdbc.jvm.result_set.OkResultSet
import com.wisecoders.common_jdbc.jvm.result_set.ResultSetIterator2
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.Util.readStringFromInputStream
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoClient
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoCollection
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoDatabase
import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.net.URL
import java.sql.Blob
import java.sql.Clob
import java.sql.Connection
import java.sql.Date
import java.sql.NClob
import java.sql.ParameterMetaData
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp
import java.util.Calendar
import java.util.regex.Pattern
import org.bson.Document
import org.slf4j.Logger

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MongoPreparedStatement : PreparedStatement {
    private val connection: MongoConnection
    private var lastResultSet: ResultSet? = null
    private var isClosed = false
    private var maxRows = -1
    private val query: String?

    internal constructor(connection: MongoConnection) {
        this.connection = connection
        this.query = null
    }

    internal constructor(
        connection: MongoConnection,
        query: String?,
    ) {
        this.connection = connection
        this.query = query
    }

    override fun <T> unwrap(iface: Class<T>): T? {
        return null
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }


    @Throws(SQLException::class)
    override fun executeQuery(query: String): ResultSet {
        var query = query
        checkClosed()
        LOGGER.atInfo().setMessage("Execute $query").log()
        if (lastResultSet != null) {
            lastResultSet!!.close()
        }
        var plainQuery = query.trim { it <= ' ' }
        if (plainQuery.endsWith(";")) {
            plainQuery = plainQuery.substring(0, plainQuery.length - 1)
        }
        val matcherSetDb = PATTERN_USE_DATABASE.matcher(plainQuery)
        if (matcherSetDb.matches()) {
            var db = matcherSetDb.group(1).trim { it <= ' ' }
            if ((db.startsWith("\"") && db.endsWith("\"")) || (db.startsWith("'") && db.endsWith("'"))) {
                db = db.substring(1, db.length - 1)
            }
            connection.catalog = db
            connection.getDatabase(db)
            WrappedMongoClient.createdDatabases.add(db)
            return OkResultSet()
        }
        val matcherCreateDatabase = PATTERN_CREATE_DATABASE.matcher(plainQuery)
        if (matcherCreateDatabase.matches()) {
            val dbName = matcherCreateDatabase.group(1)
            connection.getDatabase(dbName)
            WrappedMongoClient.createdDatabases.add(dbName)
            return OkResultSet()
        }
        if (query.lowercase().startsWith("show ")) {
            if (PATTERN_SHOW_DATABASES.matcher(query).matches() || PATTERN_SHOW_DBS.matcher(plainQuery).matches()) {
                val result = ArrayResultSet()
                result.setColumnNames(listOf("DATABASE_NAME"))
                for (str in connection.databaseNames) {
                    result.addRow(listOf<String?>(str))
                }
                return result.also { lastResultSet = it }
            } else if (PATTERN_SHOW_COLLECTIONS.matcher(plainQuery).matches()) {
                val result = ArrayResultSet()
                result.setColumnNames(listOf("COLLECTION_NAME"))
                for (str in connection.client.getCollectionNames(connection.catalog)) {
                    result.addRow(listOf<String?>(str))
                }
                return result.also { lastResultSet = it }
            } else if (PATTERN_SHOW_USERS.matcher(plainQuery).matches()) {
                query = "db.runCommand(\"{usersInfo:'" + connection.catalog + "'}\")"
            } else if (PATTERN_SHOW_PROFILES.matcher(plainQuery).matches() || PATTERN_SHOW_RULES.matcher(plainQuery)
                    .matches()
            ) {
                throw SQLException("Not yet implemented in this driver.")
            } else {
                throw SQLException("Invalid command : $plainQuery")
            }
        }
        try {
            val context = connection.createContext()
            var dbIsSet = false
            val bindings = context!!.getBindings("js")
            for (db in connection.databases) {
                bindings.putMember(db.name, db)
                if (connection.catalog == db.name) {
                    bindings.putMember("db", db)
                    dbIsSet = true
                }
            }
            if (!dbIsSet) {
                bindings.putMember("db", connection.getDatabase("admin"))
            }
            bindings.putMember("client", connection)
            val initScript = readStringFromInputStream(
                MongoPreparedStatement::class.java.getResourceAsStream("init.js")
            )
            context.eval("js", initScript)

            val value = context.eval("js", query)
            var obj: Any? = value
            if (value.isHostObject) {
                obj = value.asHostObject()
            }
            if (obj is AggregateIterable<*>) {
                lastResultSet = ResultSetIterator2(obj.allowDiskUse(true).iterator(), connection.client.expandResultSet)
            } else if (obj is Iterable<*>) {
                lastResultSet = ResultSetIterator2(obj.iterator(), connection.client.expandResultSet)
            } else if (obj is Iterator<*>) {
                lastResultSet = ResultSetIterator2(obj, connection.client.expandResultSet)
            } else if (obj is WrappedMongoCollection<*>) {
                lastResultSet = ResultSetIterator2(obj.find(), connection.client.expandResultSet)
            } else {
                lastResultSet = ObjectAsResultSet(obj)
            }
            return lastResultSet!!
        } catch (ex: Throwable) {
            LOGGER.atError().setMessage("Error executing: $query").setCause(ex).log()
            throw SQLException(ex.message, ex)
        }
    }

    fun debug(
        doc: Document,
        prefix: String,
        out: StringBuilder,
    ): StringBuilder {
        for (key in doc.keys) {
            val value = doc[key]
            out.append(prefix).append(key).append(" = ").append(getClassDetails(value)).append(" ").append(value)
                .append("\n")
            if (value is Document) {
                debug(value, "$prefix  ", out)
            }
        }
        return out
    }

    private fun getClassDetails(obj: Any?): String {
        val sb = StringBuilder()
        if (obj != null) {
            sb.append("[ Class:").append(obj.javaClass.name).append(" implements ")
            for (inf in obj.javaClass.interfaces) {
                sb.append(inf.name)
            }
            sb.append(" ] ").append(obj)
        }
        return sb.toString()
    }


    @Throws(SQLException::class)
    override fun execute(query: String): Boolean {
        executeQuery(query)
        return lastResultSet != null
    }

    private var documentParam: Document? = null

    @Throws(SQLException::class)
    override fun setObject(
        parameterIndex: Int,
        x: Any,
    ) {
        documentParam = when (x) {
            is Document -> {
                x
            }

            is Map<*, *> -> {
                Document(x as Map<String, *>)
            }

            null -> {
                throw SQLException("Map object expected. You currently did setObject( NULL ) ")
            }

            else -> {
                throw SQLException("Map object expected. You currently did setObject( " + x.javaClass.name + " ) ")
            }
        }
    }

    @Throws(SQLException::class)
    override fun executeUpdate(): Int {
        return executeUpdate(query)
    }

    private fun getDatabase(name: String): WrappedMongoDatabase? {
        for (scan in connection.databases) {
            if (scan.name.equals(name, ignoreCase = true)) {
                return scan
            }
        }
        if ("db" == name && connection.catalog != null) {
            for (scan in connection.databases) {
                if (scan.name.equals(connection.catalog, ignoreCase = true)) {
                    return scan
                }
            }
        }
        return null
    }

    @Throws(SQLException::class)
    override fun executeUpdate(sql: String?): Int {
        var sql = sql
        if (sql != null) {
            if (documentParam == null) {
                // IF HAS NO PARAMETERS, EXECUTE AS NORMAL SQL
                execute(sql)
                return 1
            } else {
                sql = sql.trim { it <= ' ' }
                var matcher = PATTERN_UPDATE.matcher(sql)
                val id = documentParam!!["_id"]
                if (matcher.matches()) {
                    val collection = getCollectionMandatory(matcher.group(1), true)
                    if (id == null) {
                        collection.insertOne(documentParam!!)
                    } else {
                        collection.replaceOne(Document("_id", id), documentParam, ReplaceOptions().upsert(true))
                    }
                    return 1
                }
                matcher = PATTERN_DELETE.matcher(sql)
                if (matcher.matches()) {
                    val collection = getCollectionMandatory(matcher.group(1), false)
                    collection.deleteOne((Document().append("_id", id)))
                    return 1
                }
            }
        }
        throw SQLException(ERROR_MESSAGE)
    }

    @Throws(SQLException::class)
    private fun getCollectionMandatory(
        collectionRef: String,
        createCollectionIfMissing: Boolean,
    ): WrappedMongoCollection<*> {
        var collectionRef = collectionRef
        var mongoDatabase: WrappedMongoDatabase? = null
        val matcherDbIdentifier = PATTERN_DB_IDENTIFIER.matcher(collectionRef)
        val matcherDbDot = PATTERN_DOT.matcher(collectionRef)
        if (matcherDbIdentifier.matches()) {
            mongoDatabase = getDatabase(matcherDbIdentifier.group(1))
            collectionRef = matcherDbIdentifier.group(2)
        } else if (matcherDbDot.matches()) {
            mongoDatabase = getDatabase(matcherDbDot.group(1))
            collectionRef = matcherDbDot.group(2)
        }
        if (mongoDatabase == null) throw SQLException("Cannot find database '$collectionRef'.")
        val matcherCollectionIdentifier = PATTERN_COLLECTION_IDENTIFIER.matcher(collectionRef)
        if (matcherCollectionIdentifier.matches()) {
            collectionRef = matcherDbIdentifier.group(1)
        }
        var collection: WrappedMongoCollection<*> = mongoDatabase.getCollection(collectionRef)
        if (collection == null && createCollectionIfMissing) {
            mongoDatabase.createCollection(collectionRef)
            collection = mongoDatabase.getCollection(collectionRef)
        }
        if (collection == null) throw SQLException("Cannot find collection '$collectionRef'.")
        return collection
    }

    @Throws(SQLException::class)
    override fun close() {
        if (lastResultSet != null) {
            lastResultSet!!.close()
        }
        this.isClosed = true
    }

    override fun getMaxFieldSize(): Int {
        return 0
    }

    override fun setMaxFieldSize(max: Int) {}

    override fun getMaxRows(): Int {
        return maxRows
    }

    override fun setMaxRows(max: Int) {
        this.maxRows = max
    }

    @Throws(SQLException::class)
    override fun setEscapeProcessing(enable: Boolean) {
    }

    @Throws(SQLException::class)
    override fun getQueryTimeout(): Int {
        checkClosed()
        // throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
        return Int.MAX_VALUE
    }

    @Throws(SQLException::class)
    override fun setQueryTimeout(seconds: Int) {
        checkClosed()
        // throw new SQLFeatureNotSupportedException("MongoDB provides no support for query timeouts.");
    }

    @Throws(SQLException::class)
    override fun cancel() {
        checkClosed()
        // throw new SQLFeatureNotSupportedException("MongoDB provides no support for interrupting an operation.");
    }

    @Throws(SQLException::class)
    override fun getWarnings(): SQLWarning? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun clearWarnings() {
        checkClosed()
    }

    @Throws(SQLException::class)
    override fun setCursorName(name: String) {
        checkClosed()
        // Driver doesn't support positioned updates for now, so no-op.
    }

    @Throws(SQLException::class)
    override fun getResultSet(): ResultSet {
        checkClosed()
        return lastResultSet!!
    }

    @Throws(SQLException::class)
    override fun getUpdateCount(): Int {
        checkClosed()
        return -1
    }

    @Throws(SQLException::class)
    override fun getMoreResults(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun setFetchDirection(direction: Int) {
    }

    @Throws(SQLException::class)
    override fun getFetchDirection(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun setFetchSize(rows: Int) {
    }

    @Throws(SQLException::class)
    override fun getFetchSize(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getResultSetConcurrency(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getResultSetType(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun addBatch(sql: String) {
        executeUpdate(sql)
    }

    @Throws(SQLException::class)
    override fun clearBatch() {
    }

    @Throws(SQLException::class)
    override fun executeBatch(): IntArray? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        checkClosed()
        return this.connection
    }

    @Throws(SQLException::class)
    override fun getMoreResults(current: Int): Boolean {
        checkClosed()
        return false
    }

    @Throws(SQLException::class)
    override fun getGeneratedKeys(): ResultSet? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String,
        autoGeneratedKeys: Int,
    ): Int {
        checkClosed()
        executeUpdate(sql)
        return 0
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String,
        columnIndexes: IntArray,
    ): Int {
        checkClosed()
        executeUpdate(sql)
        return 0
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String,
        columnNames: Array<String>,
    ): Int {
        checkClosed()
        executeUpdate(sql)
        return 0
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        autoGeneratedKeys: Int,
    ): Boolean {
        checkClosed()
        executeUpdate(sql)
        return false
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        columnIndexes: IntArray,
    ): Boolean {
        checkClosed()
        executeUpdate(sql)
        return false
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        columnNames: Array<String>,
    ): Boolean {
        checkClosed()
        executeUpdate(sql)
        return false
    }

    override fun getResultSetHoldability(): Int {
        return 0
    }

    override fun isClosed(): Boolean {
        return isClosed
    }

    override fun setPoolable(poolable: Boolean) {}

    override fun isPoolable(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    private fun checkClosed() {
        if (isClosed) {
            throw SQLException("Statement was previously closed.")
        }
    }

    @Throws(SQLException::class)
    override fun closeOnCompletion() {
    }

    @Throws(SQLException::class)
    override fun isCloseOnCompletion(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun executeQuery(): ResultSet {
        execute(query!!)
        return lastResultSet!!
    }

    @Throws(SQLException::class)
    override fun setNull(
        parameterIndex: Int,
        sqlType: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBoolean(
        parameterIndex: Int,
        x: Boolean,
    ) {
    }

    @Throws(SQLException::class)
    override fun setByte(
        parameterIndex: Int,
        x: Byte,
    ) {
    }

    @Throws(SQLException::class)
    override fun setShort(
        parameterIndex: Int,
        x: Short,
    ) {
    }

    @Throws(SQLException::class)
    override fun setInt(
        parameterIndex: Int,
        x: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setLong(
        parameterIndex: Int,
        x: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setFloat(
        parameterIndex: Int,
        x: Float,
    ) {
    }

    @Throws(SQLException::class)
    override fun setDouble(
        parameterIndex: Int,
        x: Double,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBigDecimal(
        parameterIndex: Int,
        x: BigDecimal,
    ) {
    }

    @Throws(SQLException::class)
    override fun setString(
        parameterIndex: Int,
        x: String,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBytes(
        parameterIndex: Int,
        x: ByteArray,
    ) {
    }

    @Throws(SQLException::class)
    override fun setDate(
        parameterIndex: Int,
        x: Date,
    ) {
    }

    @Throws(SQLException::class)
    override fun setTime(
        parameterIndex: Int,
        x: Time,
    ) {
    }

    @Throws(SQLException::class)
    override fun setTimestamp(
        parameterIndex: Int,
        x: Timestamp,
    ) {
    }

    @Throws(SQLException::class)
    override fun setAsciiStream(
        parameterIndex: Int,
        x: InputStream,
        length: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setUnicodeStream(
        parameterIndex: Int,
        x: InputStream,
        length: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBinaryStream(
        parameterIndex: Int,
        x: InputStream,
        length: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun clearParameters() {
    }

    @Throws(SQLException::class)
    override fun setObject(
        parameterIndex: Int,
        x: Any,
        targetSqlType: Int,
    ) {
    }


    @Throws(SQLException::class)
    override fun execute(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun addBatch() {
        executeUpdate()
    }

    @Throws(SQLException::class)
    override fun setCharacterStream(
        parameterIndex: Int,
        reader: Reader,
        length: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setRef(
        parameterIndex: Int,
        x: Ref,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBlob(
        parameterIndex: Int,
        x: Blob,
    ) {
    }

    @Throws(SQLException::class)
    override fun setClob(
        parameterIndex: Int,
        x: Clob,
    ) {
    }

    @Throws(SQLException::class)
    override fun setArray(
        parameterIndex: Int,
        x: java.sql.Array,
    ) {
    }

    @Throws(SQLException::class)
    override fun getMetaData(): ResultSetMetaData? {
        return null
    }

    @Throws(SQLException::class)
    override fun setDate(
        parameterIndex: Int,
        x: Date,
        cal: Calendar,
    ) {
    }

    @Throws(SQLException::class)
    override fun setTime(
        parameterIndex: Int,
        x: Time,
        cal: Calendar,
    ) {
    }

    @Throws(SQLException::class)
    override fun setTimestamp(
        parameterIndex: Int,
        x: Timestamp,
        cal: Calendar,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNull(
        parameterIndex: Int,
        sqlType: Int,
        typeName: String,
    ) {
    }

    @Throws(SQLException::class)
    override fun setURL(
        parameterIndex: Int,
        x: URL,
    ) {
    }

    @Throws(SQLException::class)
    override fun getParameterMetaData(): ParameterMetaData? {
        return null
    }

    @Throws(SQLException::class)
    override fun setRowId(
        parameterIndex: Int,
        x: RowId,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNString(
        parameterIndex: Int,
        value: String,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNCharacterStream(
        parameterIndex: Int,
        value: Reader,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNClob(
        parameterIndex: Int,
        value: NClob,
    ) {
    }

    @Throws(SQLException::class)
    override fun setClob(
        parameterIndex: Int,
        reader: Reader,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBlob(
        parameterIndex: Int,
        inputStream: InputStream,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNClob(
        parameterIndex: Int,
        reader: Reader,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setSQLXML(
        parameterIndex: Int,
        xmlObject: SQLXML,
    ) {
    }

    @Throws(SQLException::class)
    override fun setObject(
        parameterIndex: Int,
        x: Any,
        targetSqlType: Int,
        scaleOrLength: Int,
    ) {
    }

    @Throws(SQLException::class)
    override fun setAsciiStream(
        parameterIndex: Int,
        x: InputStream,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBinaryStream(
        parameterIndex: Int,
        x: InputStream,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setCharacterStream(
        parameterIndex: Int,
        reader: Reader,
        length: Long,
    ) {
    }

    @Throws(SQLException::class)
    override fun setAsciiStream(
        parameterIndex: Int,
        x: InputStream,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBinaryStream(
        parameterIndex: Int,
        x: InputStream,
    ) {
    }

    @Throws(SQLException::class)
    override fun setCharacterStream(
        parameterIndex: Int,
        reader: Reader,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNCharacterStream(
        parameterIndex: Int,
        value: Reader,
    ) {
    }

    @Throws(SQLException::class)
    override fun setClob(
        parameterIndex: Int,
        reader: Reader,
    ) {
    }

    @Throws(SQLException::class)
    override fun setBlob(
        parameterIndex: Int,
        inputStream: InputStream,
    ) {
    }

    @Throws(SQLException::class)
    override fun setNClob(
        parameterIndex: Int,
        reader: Reader,
    ) {
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()

        private val PATTERN_USE_DATABASE: Pattern = Pattern.compile("USE\\s+(.*)", Pattern.CASE_INSENSITIVE)
        private val PATTERN_CREATE_DATABASE: Pattern =
            Pattern.compile("CREATE\\s+DATABASE\\s*'(.*)'\\s*", Pattern.CASE_INSENSITIVE)

        private val PATTERN_SHOW_DATABASES: Pattern = Pattern.compile("SHOW\\s+DATABASES\\s*", Pattern.CASE_INSENSITIVE)
        private val PATTERN_SHOW_DBS: Pattern = Pattern.compile("SHOW\\s+DBS\\s*", Pattern.CASE_INSENSITIVE)
        private val PATTERN_SHOW_COLLECTIONS: Pattern =
            Pattern.compile("SHOW\\s+COLLECTIONS\\s*", Pattern.CASE_INSENSITIVE)
        private val PATTERN_SHOW_USERS: Pattern = Pattern.compile("SHOW\\s+USERS\\s*", Pattern.CASE_INSENSITIVE)
        private val PATTERN_SHOW_RULES: Pattern = Pattern.compile("SHOW\\s+RULES\\s*", Pattern.CASE_INSENSITIVE)
        private val PATTERN_SHOW_PROFILES: Pattern = Pattern.compile("SHOW\\s+PROFILES\\s*", Pattern.CASE_INSENSITIVE)


        private val PATTERN_UPDATE: Pattern = Pattern.compile("UPDATE\\s+(.*)", Pattern.CASE_INSENSITIVE)
        private val PATTERN_DELETE: Pattern = Pattern.compile("DELETE\\s+FROM\\s+(.*)", Pattern.CASE_INSENSITIVE)
        private const val ERROR_MESSAGE =
            "Allowed statements: update(<dbname>.<collectionName>) or delete(<dbname>.<collectionName>). Before calling this do setObject(0,<dbobject>)."

        private val PATTERN_DB_IDENTIFIER: Pattern =
            Pattern.compile("client\\.getDatabase\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        private val PATTERN_COLLECTION_IDENTIFIER: Pattern =
            Pattern.compile("getCollection\\('(.*)'\\).(.*)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        private val PATTERN_DOT: Pattern = Pattern.compile("(.*)\\.(.*)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
    }
}
