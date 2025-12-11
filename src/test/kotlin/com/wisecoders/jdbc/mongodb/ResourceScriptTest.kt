package com.wisecoders.jdbc.mongodb

import com.wisecoders.common_jdbc.jvm.sql.printResultSet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("disabled until we figure out how to run tests needing docker containers")
class ResourceScripts : WithAssertions {
    private var con: Connection? = null

    @BeforeEach
    @Throws(ClassNotFoundException::class, SQLException::class)
    fun setUp() {
        Class.forName("com.wisecoders.jdbc.mongodb.JdbcDriver")
        con = DriverManager.getConnection("jdbc:mongodb://localhost")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun script() {
        executeFile("script.txt")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun aggregate() {
        executeFile("testAggregate.txt")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun aggregate2() {
        executeFile("testAggregate2.txt")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun inventory() {
        executeFile("inventory.txt")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun mapReduce() {
        executeFile("mapReduce.txt")
    }

    @Test
    @Throws(IOException::class, SQLException::class)
    fun masterSlave() {
        executeFile("masterSlave.txt")
    }

    @Throws(IOException::class, SQLException::class)
    private fun executeFile(fileName: String) {
        val inputStream: InputStream = javaClass.getResourceAsStream(fileName)
            ?: throw IOException("classpath resource [$fileName] not found")

        inputStream.bufferedReader().use { reader: BufferedReader ->
            val statement = StringBuilder()

            reader.lineSequence()
                .filter { it.isNotBlank() }
                .forEach { line ->
                    statement.appendLine(line)
                    if (line.trimEnd().endsWith(";")) {
                        executeQuery(statement.toString())
                        statement.clear()
                    }
                }

            if (statement.isNotEmpty()) {
                executeQuery(statement.toString())
            }
        }
    }

    @Throws(SQLException::class)
    private fun executeQuery(query: String) {
        println("> $query")
        val stmt = con!!.createStatement()
        try {
            stmt.executeQuery(query).printResultSet()
        } catch (ex: Throwable) {
            println(ex.localizedMessage)
            throw ex
        }
        stmt.close()
    }
}
