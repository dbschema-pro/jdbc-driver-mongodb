package com.wisecoders.jdbc.mongodb

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("disabled until we figure out how to run tests needing docker containers")
class ExpandedResultSetTest : WithAssertions {
    private var con: Connection? = null

    @BeforeEach
    @Throws(ClassNotFoundException::class, SQLException::class)
    fun setUp() {
        Class.forName("com.wisecoders.jdbc.mongodb.JdbcDriver")
        val _con = DriverManager.getConnection(urlWithoutAuth, null, null)
        this.con = _con
        val stmt = _con.createStatement()
        stmt.execute("local.words.drop();")
        stmt.execute("local.words.insertOne({word: 'sample1'});")
        stmt.execute("local.words.insertOne({word: 'sample2', qty:5});")
        stmt.close()
    }

    @Test
    @Throws(Exception::class)
    fun testFind() {
        val stmt = con!!.createStatement()
        val rs = stmt.executeQuery("local.words.find()")
        val metaData = rs.metaData
        for (i in 1..metaData.columnCount) {
            System.out.printf("%30s", metaData.getColumnName(i) + "(" + metaData.getColumnType(i) + ")")
        }
        println()
        while (rs.next()) {
            for (i in 1..metaData.columnCount) {
                System.out.printf("%30s", rs.getString(i))
            }
            println()
        }
        stmt.close()
    }


    companion object {
        private const val urlWithoutAuth = "mongodb://localhost?expand=true"
    }
}
