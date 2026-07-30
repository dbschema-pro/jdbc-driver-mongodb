package com.wisecoders.jdbc.mongodb

import java.sql.ResultSet
import java.sql.SQLException

/**
 * The delegate result set does not close the MongoDB cursor it reads from, which leaks a server
 * cursor whenever a result is not read to the end — this wrapper closes it. It also enforces
 * `Statement.setMaxRows`, which the delegate does not know about.
 *
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MongoResultSet(
    private val resultSet: ResultSet,
    private val cursor: AutoCloseable?,
    private val maxRows: Int,
) : ResultSet by resultSet {

    private var returnedRows = 0
    private var isClosed = false

    override fun next(): Boolean {
        if (isClosed) {
            throw SQLException("ResultSet was previously closed.")
        }
        if (maxRows > 0 && returnedRows >= maxRows) {
            return false
        }
        val hasNext: Boolean = resultSet.next()
        if (hasNext) {
            returnedRows++
        }
        return hasNext
    }

    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
        try {
            cursor?.close()
        } catch (ex: Exception) {
            throw SQLException("Failed closing the database cursor.", ex)
        } finally {
            resultSet.close()
        }
    }

    override fun isClosed(): Boolean {
        return isClosed
    }
}
