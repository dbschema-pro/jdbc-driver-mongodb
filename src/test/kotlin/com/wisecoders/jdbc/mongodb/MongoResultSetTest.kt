package com.wisecoders.jdbc.mongodb

import com.wisecoders.common_jdbc.jvm.result_set.ResultSetIterator2
import java.sql.SQLException
import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MongoResultSetTest : WithAssertions {

    private class CloseTrackingIterator(
        documents: List<Map<String, Any>>,
    ) : Iterator<Map<String, Any>>, AutoCloseable {
        private val documentIterator: Iterator<Map<String, Any>> = documents.iterator()
        var closeCount = 0
            private set

        override fun hasNext(): Boolean {
            return documentIterator.hasNext()
        }

        override fun next(): Map<String, Any> {
            return documentIterator.next()
        }

        override fun close() {
            closeCount++
        }
    }

    private fun documents(count: Int): List<Map<String, Any>> {
        return (1..count).map { mapOf("idx" to it) }
    }

    private fun mongoResultSet(
        cursor: CloseTrackingIterator,
        maxRows: Int,
    ): MongoResultSet {
        return MongoResultSet(ResultSetIterator2(cursor, false), cursor, maxRows)
    }

    private fun countRows(resultSet: MongoResultSet): Int {
        var rows = 0
        while (resultSet.next()) {
            rows++
        }
        return rows
    }

    @Nested
    inner class MaxRows {

        @Test
        fun testMaxRowsLimitsReturnedRows() {
            val resultSet = mongoResultSet(CloseTrackingIterator(documents(5)), 2)
            assertThat(countRows(resultSet)).isEqualTo(2)
        }

        @Test
        fun testMaxRowsZeroReturnsAllRows() {
            val resultSet = mongoResultSet(CloseTrackingIterator(documents(3)), 0)
            assertThat(countRows(resultSet)).isEqualTo(3)
        }

        @Test
        fun testNextStaysFalseAfterLimitReached() {
            val resultSet = mongoResultSet(CloseTrackingIterator(documents(5)), 1)
            assertThat(resultSet.next()).isTrue()
            assertThat(resultSet.next()).isFalse()
            assertThat(resultSet.next()).isFalse()
        }
    }

    @Nested
    inner class Closing {

        @Test
        fun testCloseClosesTheCursor() {
            val cursor = CloseTrackingIterator(documents(3))
            val resultSet = mongoResultSet(cursor, 0)
            resultSet.close()
            assertThat(cursor.closeCount).isEqualTo(1)
        }

        @Test
        fun testCloseIsIdempotent() {
            val cursor = CloseTrackingIterator(documents(3))
            val resultSet = mongoResultSet(cursor, 0)
            resultSet.close()
            resultSet.close()
            assertThat(cursor.closeCount).isEqualTo(1)
        }

        @Test
        fun testIsClosedReflectsClose() {
            val resultSet = mongoResultSet(CloseTrackingIterator(documents(3)), 0)
            assertThat(resultSet.isClosed).isFalse()
            resultSet.close()
            assertThat(resultSet.isClosed).isTrue()
        }

        @Test
        fun testNextAfterCloseThrows() {
            val resultSet = mongoResultSet(CloseTrackingIterator(documents(3)), 0)
            resultSet.close()
            assertThatThrownBy { resultSet.next() }.isInstanceOf(SQLException::class.java)
        }

        @Test
        fun testCloseWithoutCursorSucceeds() {
            val resultSet = MongoResultSet(ResultSetIterator2(documents(1).iterator(), false), null, 0)
            resultSet.close()
            assertThat(resultSet.isClosed).isTrue()
        }
    }
}
