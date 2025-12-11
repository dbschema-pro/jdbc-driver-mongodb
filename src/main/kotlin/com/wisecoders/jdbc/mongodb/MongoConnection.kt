package com.wisecoders.jdbc.mongodb

import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoClient
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoDatabase
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.SQLClientInfoException
import java.sql.SQLException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Savepoint
import java.sql.Statement
import java.sql.Struct
import java.util.Properties
import java.util.concurrent.Executor
import org.graalvm.polyglot.Context

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MongoConnection internal constructor(@JvmField val client: WrappedMongoClient) : Connection {
    private var catalog: String? = null
    private var isClosed = false
    private var isReadOnly = false


    override fun getCatalog(): String {
        return catalog!!
    }

    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        checkClosed()
        return false
    }

    /**
     * @see java.sql.Connection.createStatement
     */
    @Throws(SQLException::class)
    override fun createStatement(): Statement {
        checkClosed()
        return MongoPreparedStatement(this)
    }

    @Throws(SQLException::class)
    override fun createStatement(
        resultSetType: Int,
        resultSetConcurrency: Int
    ): Statement {
        checkClosed()
        return MongoPreparedStatement(this)
    }

    @Throws(SQLException::class)
    override fun createStatement(
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int
    ): Statement {
        checkClosed()
        return MongoPreparedStatement(this)
    }


    @Throws(SQLException::class)
    override fun prepareStatement(sql: String): PreparedStatement {
        checkClosed()
        return MongoPreparedStatement(this, sql)
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String): CallableStatement? {
        checkClosed()
        return null
    }

    /**
     * @see java.sql.Connection.nativeSQL
     */
    @Throws(SQLException::class)
    override fun nativeSQL(sql: String): String {
        checkClosed()
        throw UnsupportedOperationException("MongoDB does not support SQL natively.")
    }

    /**
     * @see java.sql.Connection.setAutoCommit
     */
    @Throws(SQLException::class)
    override fun setAutoCommit(autoCommit: Boolean) {
        checkClosed()
    }

    /**
     * @see java.sql.Connection.getAutoCommit
     */
    @Throws(SQLException::class)
    override fun getAutoCommit(): Boolean {
        checkClosed()
        return true
    }

    @Throws(SQLException::class)
    override fun commit() {
        checkClosed()
    }

    @Throws(SQLException::class)
    override fun rollback() {
        checkClosed()
    }

    override fun close() {
        client.close()
        isClosed = true
    }

    override fun isClosed(): Boolean {
        return isClosed
    }

    private val metaData = MongoDatabaseMetaData(this)

    @Throws(SQLException::class)
    override fun getMetaData(): DatabaseMetaData {
        checkClosed()
        return metaData
    }

    @Throws(SQLException::class)
    override fun setReadOnly(readOnly: Boolean) {
        checkClosed()
        isReadOnly = readOnly
    }

    /**
     * @see java.sql.Connection.isReadOnly
     */
    @Throws(SQLException::class)
    override fun isReadOnly(): Boolean {
        checkClosed()
        return isReadOnly
    }

    override fun setCatalog(catalog: String) {
        this.catalog = catalog
    }

    @Throws(SQLException::class)
    override fun setTransactionIsolation(level: Int) {
        checkClosed()
        // Since the only valid value for MongDB is Connection.TRANSACTION_NONE, and the javadoc for this method
        // indicates that this is not a valid value for level here, throw unsupported operation exception.
        // throw new UnsupportedOperationException("MongoDB provides no support for transactions.");
    }

    /**
     * @see java.sql.Connection.getTransactionIsolation
     */
    @Throws(SQLException::class)
    override fun getTransactionIsolation(): Int {
        checkClosed()
        return Connection.TRANSACTION_NONE
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
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int
    ): PreparedStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int
    ): CallableStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun getTypeMap(): Map<String, Class<*>>? {
        return null
    }

    @Throws(SQLException::class)
    override fun setTypeMap(map: Map<String?, Class<*>?>?) {
    }

    @Throws(SQLException::class)
    override fun setHoldability(holdability: Int) {
    }

    @Throws(SQLException::class)
    override fun getHoldability(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun setSavepoint(): Savepoint? {
        return null
    }

    @Throws(SQLException::class)
    override fun setSavepoint(name: String): Savepoint? {
        return null
    }

    @Throws(SQLException::class)
    override fun rollback(savepoint: Savepoint) {
    }

    @Throws(SQLException::class)
    override fun releaseSavepoint(savepoint: Savepoint) {
    }


    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int
    ): PreparedStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int
    ): CallableStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        autoGeneratedKeys: Int
    ): PreparedStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnIndexes: IntArray
    ): PreparedStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnNames: Array<String>
    ): PreparedStatement? {
        return null
    }

    @Throws(SQLException::class)
    override fun createClob(): Clob? {
        return null
    }

    @Throws(SQLException::class)
    override fun createBlob(): Blob? {
        return null
    }

    @Throws(SQLException::class)
    override fun createNClob(): NClob? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun createSQLXML(): SQLXML? {
        checkClosed()
        return null
    }

    /**
     * @see java.sql.Connection.isValid
     */
    @Throws(SQLException::class)
    override fun isValid(timeout: Int): Boolean {
        checkClosed()
        return true
    }

    /**
     * @see java.sql.Connection.setClientInfo
     */
    @Throws(SQLClientInfoException::class)
    override fun setClientInfo(
        name: String,
        value: String
    ) {
        /* MongoDB does not support setting client information in the database. */
    }

    /**
     * @see java.sql.Connection.setClientInfo
     */
    @Throws(SQLClientInfoException::class)
    override fun setClientInfo(properties: Properties) {
        /* MongoDB does not support setting client information in the database. */
    }

    /**
     * @see java.sql.Connection.getClientInfo
     */
    @Throws(SQLException::class)
    override fun getClientInfo(name: String): String? {
        checkClosed()
        return null
    }

    /**
     * @see java.sql.Connection.getClientInfo
     */
    @Throws(SQLException::class)
    override fun getClientInfo(): Properties? {
        checkClosed()
        return null
    }

    @Throws(SQLException::class)
    override fun createArrayOf(
        typeName: String,
        elements: Array<Any>
    ): java.sql.Array? {
        checkClosed()

        return null
    }

    /**
     * @see java.sql.Connection.createStruct
     */
    @Throws(SQLException::class)
    override fun createStruct(
        typeName: String,
        attributes: Array<Any>
    ): Struct? {
        checkClosed()

        return null
    }

    val url: String
        /**
         * @return the URI
         */
        get() = client.uRI

    val databases: List<WrappedMongoDatabase>
        get() = client.databases

    val databaseNames: List<String>
        get() = client.databaseNames

    fun getDatabase(name: String): WrappedMongoDatabase {
        return client.getDatabase(name)
    }


    @Throws(SQLException::class)
    private fun checkClosed() {
        if (isClosed) {
            throw SQLException("Statement was previously closed.")
        }
    }

    @Throws(SQLException::class)
    override fun setSchema(schema: String) {
    }

    @Throws(SQLException::class)
    override fun getSchema(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun abort(executor: Executor) {
    }

    @Throws(SQLException::class)
    override fun setNetworkTimeout(
        executor: Executor,
        milliseconds: Int
    ) {
    }

    @Throws(SQLException::class)
    override fun getNetworkTimeout(): Int {
        return 0
    }

    private var context: Context? = null

    init {
        setCatalog(client.currentDatabaseName)

        try {
            client.pingServer()
        } catch (ex: Throwable) {
            throw SQLException(ex.localizedMessage, ex)
        }
    }

    fun createContext(): Context? {
        // System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        // Without this it doesn't find the JS or Truffle
        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
        //https://github.com/oracle/graaljs/issues/214
        if (context == null) {
            context = Context.newBuilder("js").allowAllAccess(true).build()
        }
        return context
    }
}
