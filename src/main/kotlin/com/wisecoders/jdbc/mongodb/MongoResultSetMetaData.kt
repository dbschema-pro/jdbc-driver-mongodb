package com.wisecoders.jdbc.mongodb

import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Types

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MongoResultSetMetaData(
    private val tableName: String,
    private val columnNames: Array<String>,
    private val javaTypes: IntArray,
    private val displaySizes: IntArray
) :
    ResultSetMetaData {
    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T? {
        return null
    }

    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnCount
     */
    @Throws(SQLException::class)
    override fun getColumnCount(): Int {
        return columnNames.size
    }

    /**
     * @see java.sql.ResultSetMetaData.isAutoIncrement
     */
    @Throws(SQLException::class)
    override fun isAutoIncrement(column: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun isCaseSensitive(column: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun isSearchable(column: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun isCurrency(column: Int): Boolean {
        return false
    }

    /**
     * @see java.sql.ResultSetMetaData.isNullable
     */
    @Throws(SQLException::class)
    override fun isNullable(column: Int): Int {
        return ResultSetMetaData.columnNoNulls
    }

    /**
     * @see java.sql.ResultSetMetaData.isSigned
     */
    @Throws(SQLException::class)
    override fun isSigned(column: Int): Boolean {
        return false
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnDisplaySize
     */
    @Throws(SQLException::class)
    override fun getColumnDisplaySize(column: Int): Int {
        return displaySizes[column - 1]
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnLabel
     */
    @Throws(SQLException::class)
    override fun getColumnLabel(column: Int): String {
        return columnNames[column - 1]
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnName
     */
    @Throws(SQLException::class)
    override fun getColumnName(column: Int): String {
        return columnNames[column - 1]
    }

    /**
     * @see java.sql.ResultSetMetaData.getSchemaName
     */
    @Throws(SQLException::class)
    override fun getSchemaName(column: Int): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getPrecision(column: Int): Int {
        return 0
    }

    /**
     * @see java.sql.ResultSetMetaData.getScale
     */
    @Throws(SQLException::class)
    override fun getScale(column: Int): Int {
        return 0
    }

    /**
     * @see java.sql.ResultSetMetaData.getTableName
     */
    @Throws(SQLException::class)
    override fun getTableName(column: Int): String {
        return tableName
    }

    @Throws(SQLException::class)
    override fun getCatalogName(column: Int): String? {
        return null
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnType
     */
    @Throws(SQLException::class)
    override fun getColumnType(column: Int): Int {
        return javaTypes[column - 1]
    }

    @Throws(SQLException::class)
    override fun getColumnTypeName(column: Int): String {
        return when (javaTypes[column - 1]) {
            Types.JAVA_OBJECT -> "map"
            else -> "varchar"
        }
    }

    /**
     * @see java.sql.ResultSetMetaData.isReadOnly
     */
    @Throws(SQLException::class)
    override fun isReadOnly(column: Int): Boolean {
        return false
    }

    /**
     * @see java.sql.ResultSetMetaData.isWritable
     */
    @Throws(SQLException::class)
    override fun isWritable(column: Int): Boolean {
        return true
    }

    /**
     * @see java.sql.ResultSetMetaData.isDefinitelyWritable
     */
    @Throws(SQLException::class)
    override fun isDefinitelyWritable(column: Int): Boolean {
        return true
    }

    /**
     * @see java.sql.ResultSetMetaData.getColumnClassName
     */
    @Throws(SQLException::class)
    override fun getColumnClassName(column: Int): String {
        return "java.lang.String"
    }
}
