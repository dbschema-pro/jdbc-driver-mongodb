package com.wisecoders.jdbc.mongodb

import com.wisecoders.jdbc.mongodb.Util.getByPath
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.assertj.core.api.WithAssertions

@Disabled("disabled until we figure out how to run tests needing docker containers")
class LargeCollectionTest : WithAssertions {
    private var con: Connection? = null

    @BeforeEach
    @Throws(ClassNotFoundException::class, SQLException::class)
    fun setUp() {
        Class.forName("com.wisecoders.jdbc.mongodb.JdbcDriver")
        val _con = DriverManager.getConnection(URL, null, null)
        this.con = _con
        val stmt = _con.createStatement()
        stmt.execute("local.manyDocs.drop();")
        val documents = (0 until DOCUMENT_COUNT).joinToString(",") { "{idx:$it}" }
        stmt.execute("local.manyDocs.insertMany([$documents])")
        stmt.close()
    }

    @AfterEach
    @Throws(SQLException::class)
    fun tearDown() {
        con?.close()
    }

    @Test
    @Throws(Exception::class)
    fun testStatementMaxRowsLimitsReturnedRows() {
        val stmt = con!!.createStatement()
        stmt.maxRows = 3
        val resultSet = stmt.executeQuery("local.manyDocs.find()")
        assertThat(countRows(resultSet)).isEqualTo(3)
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testMaxRowsZeroReturnsAllRows() {
        val stmt = con!!.createStatement()
        val resultSet = stmt.executeQuery("local.manyDocs.find()")
        assertThat(countRows(resultSet)).isEqualTo(DOCUMENT_COUNT)
        stmt.close()
    }

    @Test
    fun testSetMaxRowsRejectsNegativeValues() {
        val stmt = con!!.createStatement()
        assertThatThrownBy { stmt.maxRows = -1 }.isInstanceOf(SQLException::class.java)
        stmt.close()
    }

    /**
     * cursor.count() must be computed server side and, like the legacy mongo shell, ignore limit():
     * counting client side would both transfer the whole collection and return the limited count.
     */
    @Test
    @Throws(Exception::class)
    fun testFindCountIsComputedServerSide() {
        val stmt = con!!.createStatement()
        val resultSet = stmt.executeQuery("local.manyDocs.find().limit(5).count()")
        assertThat(resultSet.next()).isTrue()
        assertThat(resultSet.getString(1)).isEqualTo("$DOCUMENT_COUNT")
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFindCountWithFilterCountsMatchingDocuments() {
        val stmt = con!!.createStatement()
        val resultSet = stmt.executeQuery("local.manyDocs.find({idx: {\$lt: 7}}).count()")
        assertThat(resultSet.next()).isTrue()
        assertThat(resultSet.getString(1)).isEqualTo("7")
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testClosingResultSetReleasesServerCursor() {
        val statusStmt = con!!.createStatement()
        val queryStmt = con!!.createStatement()
        val openCursorsBefore = countOpenServerCursors(statusStmt)

        val resultSet = queryStmt.executeQuery("local.manyDocs.find().batchSize(10)")
        assertThat(resultSet.next()).isTrue()
        assertThat(countOpenServerCursors(statusStmt)).isEqualTo(openCursorsBefore + 1)

        resultSet.close()
        assertThat(countOpenServerCursors(statusStmt)).isEqualTo(openCursorsBefore)

        queryStmt.close()
        statusStmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testCollectionScanForMetadataDiscoversFields() {
        val columnNames = mutableListOf<String>()
        con!!.metaData.getColumns("local", null, "manyDocs", null).use { resultSet: ResultSet ->
            while (resultSet.next()) {
                columnNames.add(resultSet.getString("COLUMN_NAME"))
            }
        }
        assertThat(columnNames).contains("_id", "idx")
    }

    private fun countRows(resultSet: ResultSet): Int {
        var rows = 0
        while (resultSet.next()) {
            rows++
        }
        return rows
    }

    private fun countOpenServerCursors(stmt: Statement): Long {
        val resultSet = stmt.executeQuery("db.runCommand({serverStatus: 1})")
        assertThat(resultSet.next()).isTrue()
        val status = resultSet.getObject(1) as Document
        return (getByPath(status, "metrics.cursor.open.total") as Number).toLong()
    }

    companion object {
        private const val URL = "mongodb://localhost:27017/local?scan=fast&connectTimeoutMS=1000"
        private const val DOCUMENT_COUNT = 500
    }
}
