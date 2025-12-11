package com.wisecoders.jdbc.mongodb

import com.wisecoders.common_jdbc.jvm.result_set.ArrayResultSet
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.structure.MetaCollection
import com.wisecoders.jdbc.mongodb.structure.MetaField
import com.wisecoders.jdbc.mongodb.structure.MetaObject
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.RowIdLifetime
import java.sql.SQLException
import java.sql.Types
import org.slf4j.Logger

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MongoDatabaseMetaData internal constructor(private val con: MongoConnection) : DatabaseMetaData {
    /**
     * @see java.sql.DatabaseMetaData.getSchemas
     */
    override fun getSchemas(): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(listOf("TABLE_SCHEMA", "TABLE_CATALOG"))
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getCatalogs
     */
    override fun getCatalogs(): ResultSet {
        val mongoDbs = con.client.databaseNames
        val retVal = ArrayResultSet()
        retVal.setColumnNames(listOf("TABLE_CAT"))
        for (mongoDb in mongoDbs) {
            retVal.addRow(listOf<String?>(mongoDb))
        }
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getTables
     */
    @Throws(SQLException::class)
    override fun getTables(
        catalogName: String?,
        schemaPattern: String?,
        tableNamePattern: String?,
        types: Array<String>?,
    ): ResultSet {
        val resultSet = ArrayResultSet()
        resultSet.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
                "TABLE_TYPE", "REMARKS", "TYPE_CAT", "TYPE_SCHEMA", "TYPE_NAME", "SELF_REFERENCING_COL_NAME",
                "REF_GENERATION", "IS_VIRTUAL"
            )
        )
        if (catalogName == null) {
            for (cat in con.client.databaseNames) {
                getTablesByCatalogName(cat, resultSet)
            }
        } else {
            getTablesByCatalogName(catalogName, resultSet)
        }
        return resultSet
    }

    @Throws(SQLException::class)
    private fun getTablesByCatalogName(
        catalogName: String,
        resultSet: ArrayResultSet,
    ) {
        for (tableName in con.client.getCollectionNames(catalogName)) {
            resultSet.addRow(createTableRow(catalogName, tableName, "TABLE"))
        }
        for (tableName in con.client.getViewNames(catalogName)) {
            resultSet.addRow(createTableRow(catalogName, tableName, "VIEW"))
        }
    }

    private fun createTableRow(
        catalogName: String,
        tableName: String,
        type: String,
    ): List<String?> {
        val collection = con.client.getDatabase(catalogName).getMetaCollectionIfAlreadyLoaded(tableName)
        val data = mutableListOf(
            catalogName, // TABLE_CAT
            null, // TABLE_SCHEMA
            tableName, // TABLE_NAME
            type, // TABLE_TYPE
            collection?.description, // REMARKS
            "", // TYPE_CAT
            "", // TYPE_SCHEMA
            "", // TYPE_NAME
            "", // SELF_REFERENCING_COL_NAME
            "", // REF_GENERATION
            if (collection == null || collection.isVirtual) "true" else "false"
        )
        return data
    }

    /**
     * @see java.sql.DatabaseMetaData.getColumns
     */
    @Throws(SQLException::class)
    override fun getColumns(
        catalogName: String,
        schemaName: String?,
        tableNamePattern: String?,
        columnNamePattern: String?,
    ): ResultSet {
        val tableNames: MutableList<String> = ArrayList()

        if (tableNamePattern == null) {
            tableNames.addAll(con.client.getCollectionNames(catalogName))
        } else {
            tableNames.add(tableNamePattern)
        }

        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
                "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
                "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE",
                "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT"
            )
        )

        for (tableName in tableNames) {
            // As far as this driver implementation goes, every "table" in MongoDB is actually a collection, and
            // every collection "table" has two columns - "_id" column which is the primary key, and a "document"
            // column which is the JSON document corresponding to the "_id". An "_id" value can be specified on
            // insert, or it can be omitted, in which case MongoDB generates a unique value.
            val collection = con.client.getDatabase(catalogName).getMetaCollection(tableName)

            LOGGER.atInfo().setMessage("Export Collection '$tableName' fields").log()
            if (collection != null) {
                for (field in collection.fields) {
                    if (columnNamePattern == null || columnNamePattern == field.name) {
                        exportColumnsRecursive(collection, result, field)
                    }
                }
            }
        }

        return result
    }

    private fun exportColumnsRecursive(
        collection: MetaCollection,
        result: ArrayResultSet,
        field: MetaField,
    ) {
        LOGGER.atInfo().setMessage("Export Collection '" + collection.name + "' field '" + field.nameWithPath + "'").log()
        result.addRow(
            listOf(
                collection.metaDatabase.name,  // "TABLE_CAT",
                null,  // "TABLE_SCHEMA",
                collection.name,  // "TABLE_NAME", (i.e. MongoDB Collection Name)
                field.nameWithPath,  // "COLUMN_NAME",
                "" + field.javaType,  // "DATA_TYPE",
                field.typeName,  // "TYPE_NAME",
                "800",  // "COLUMN_SIZE",
                "0",  // "BUFFER_LENGTH", (not used)
                "0",  // "DECIMAL_DIGITS",
                "10",  // "NUM_PREC_RADIX",
                "" + (if (field.isMandatory) DatabaseMetaData.columnNoNulls else DatabaseMetaData.columnNullable),  // "NULLABLE",
                field.description,  // "REMARKS",
                field.options,  // "COLUMN_DEF",
                "0",  // "SQL_DATA_TYPE", (not used)
                "0",  // "SQL_DATETIME_SUB", (not used)
                "800",  // "CHAR_OCTET_LENGTH",
                "1",  // "ORDINAL_POSITION",
                "NO",  // "IS_NULLABLE",
                null,  // "SCOPE_CATLOG", (not a REF type)
                null,  // "SCOPE_SCHEMA", (not a REF type)
                null,  // "SCOPE_TABLE", (not a REF type)
                null,  // "SOURCE_DATA_TYPE", (not a DISTINCT or REF type)
                "NO" // "IS_AUTOINCREMENT" (can be auto-generated, but can also be specified)
            )
        )
        if (field is MetaObject) {
            for (children in field.fields) {
                exportColumnsRecursive(collection, result, children)
            }
        }
    }


    /**
     * @see java.sql.DatabaseMetaData.getPrimaryKeys
     */
    override fun getPrimaryKeys(
        catalogName: String,
        schemaName: String?,
        tableNamePattern: String?,
    ): ResultSet {
        /*
        * 	<LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
       *	<LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
       *	<LI><B>TABLE_NAME</B> String => table name
       *	<LI><B>COLUMN_NAME</B> String => column name
       *	<LI><B>KEY_SEQ</B> short => sequence number within primary key( a value
       *  of 1 represents the first column of the primary key, a value of 2 would
       *  represent the second column within the primary key).
       *	<LI><B>PK_NAME</B> Stri
        *
        */

        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
                "KEY_SEQ", "PK_NAME"
            )
        )

        val collection = con.client.getDatabase(catalogName).getMetaCollection(tableNamePattern)
        if (collection != null) {
            for (index in collection.metaIndexes) {
                if (index.pk) {
                    for (field in index.metaFields) {
                        result.addRow(
                            listOf(
                                collection.name,  // "TABLE_CAT",
                                null,  // "TABLE_SCHEMA",
                                collection.name,  // "TABLE_NAME", (i.e. MongoDB Collection Name)
                                field.nameWithPath,  // "COLUMN_NAME",
                                "" + index.metaFields.indexOf(field) + 1,  // "ORDINAL_POSITION"
                                index.name // "INDEX_NAME",
                            )
                        )
                    }
                }
            }
        }
        return result
    }


    /**
     * @see java.sql.DatabaseMetaData.getIndexInfo
     */
    override fun getIndexInfo(
        catalogName: String,
        schemaName: String?,
        tableNamePattern: String?,
        unique: Boolean,
        approximate: Boolean,
    ): ResultSet {
        /*
        *      *  <OL>
            *	<LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
            *	<LI><B>TABLE_SCHEMA</B> String => table schema (may be <code>null</code>)
            *	<LI><B>TABLE_NAME</B> String => table name
            *	<LI><B>NON_UNIQUE</B> boolean => Can index values be non-unique.
            *      false when TYPE is tableIndexStatistic
            *	<LI><B>INDEX_QUALIFIER</B> String => index catalog (may be <code>null</code>);
            *      <code>null</code> when TYPE is tableIndexStatistic
            *	<LI><B>INDEX_NAME</B> String => index name; <code>null</code> when TYPE is
            *      tableIndexStatistic
            *	<LI><B>TYPE</B> short => index type:
            *      <UL>
            *      <LI> tableIndexStatistic - this identifies table statistics that are
            *           returned in conjuction with a table's index descriptions
            *      <LI> tableIndexClustered - this is a clustered index
            *      <LI> tableIndexHashed - this is a hashed index
            *      <LI> tableIndexOther - this is some other style of index
            *      </UL>
            *	<LI><B>ORDINAL_POSITION</B> short => column sequence number
            *      within index; zero when TYPE is tableIndexStatistic
            *	<LI><B>COLUMN_NAME</B> String => column name; <code>null</code> when TYPE is
            *      tableIndexStatistic
            *	<LI><B>ASC_OR_DESC</B> String => column sort sequence, "A" => ascending,
            *      "D" => descending, may be <code>null</code> if sort sequence is not supported;
            *      <code>null</code> when TYPE is tableIndexStatistic
            *	<LI><B>CARDINALITY</B> int => When TYPE is tableIndexStatistic, then
            *      this is the number of rows in the table; otherwise, it is the
            *      number of unique values in the index.
            *	<LI><B>PAGES</B> int => When TYPE is  tableIndexStatisic then
            *      this is the number of pages used for the table, otherwise it
            *      is the number of pages used for the current index.
            *	<LI><B>FILTER_CONDITION</B> String => Filter condition, if any.
            *      (may be <code>null</code>)
            *  </OL>
        */
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
                "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC",
                "CARDINALITY", "PAGES", "FILTER_CONDITION"
            )
        )

        val collection = con.client.getDatabase(catalogName).getMetaCollection(tableNamePattern)

        if (collection != null) {
            for (index in collection.metaIndexes) {
                if (!index.pk) {
                    for (field in index.metaFields) {
                        result.addRow(
                            listOf(
                                collection.name,  // "TABLE_CAT",
                                null,  // "TABLE_SCHEMA",
                                collection.name,  // "TABLE_NAME", (i.e. MongoDB Collection Name)
                                if (index.unique) "false" else "true",  // "NON-UNIQUE",
                                collection.name,  // "INDEX QUALIFIER",
                                index.name,  // "INDEX_NAME",
                                "0",  // "TYPE",
                                "" + index.metaFields.indexOf(field) + 1,  // "ORDINAL_POSITION"
                                field.nameWithPath,  // "COLUMN_NAME",
                                "A",  // "ASC_OR_DESC",
                                "0",  // "CARDINALITY",
                                "0",  // "PAGES",
                                "" // "FILTER_CONDITION",
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    /**
     * @see java.sql.DatabaseMetaData.getTypeInfo
     */
    @Throws(SQLException::class)
    override fun getTypeInfo(): ResultSet {
        /*
            * <P>Each type description has the following columns:
            *  <OL>
            *	<LI><B>TYPE_NAME</B> String => Type name
            *	<LI><B>DATA_TYPE</B> int => SQL data type from java.sql.Types
            *	<LI><B>PRECISION</B> int => maximum precision
            *	<LI><B>LITERAL_PREFIX</B> String => prefix used to quote a literal
            *      (may be <code>null</code>)
            *	<LI><B>LITERAL_SUFFIX</B> String => suffix used to quote a literal
            (may be <code>null</code>)
            *	<LI><B>CREATE_PARAMS</B> String => parameters used in creating
            *      the type (may be <code>null</code>)
            *	<LI><B>NULLABLE</B> short => can you use NULL for this type.
            *      <UL>
            *      <LI> typeNoNulls - does not allow NULL values
            *      <LI> typeNullable - allows NULL values
            *      <LI> typeNullableUnknown - nullability unknown
            *      </UL>
            *	<LI><B>CASE_SENSITIVE</B> boolean=> is it case sensitive.
            *	<LI><B>SEARCHABLE</B> short => can you use "WHERE" based on this type:
            *      <UL>
            *      <LI> typePredNone - No support
            *      <LI> typePredChar - Only supported with WHERE .. LIKE
            *      <LI> typePredBasic - Supported except for WHERE .. LIKE
            *      <LI> typeSearchable - Supported for all WHERE ..
            *      </UL>
            *	<LI><B>UNSIGNED_ATTRIBUTE</B> boolean => is it unsigned.
            *	<LI><B>FIXED_PREC_SCALE</B> boolean => can it be a money value.
            *	<LI><B>AUTO_INCREMENT</B> boolean => can it be used for an
            *      auto-increment value.
            *	<LI><B>LOCAL_TYPE_NAME</B> String => localized version of type name
            *      (may be <code>null</code>)
            *	<LI><B>MINIMUM_SCALE</B> short => minimum scale supported
            *	<LI><B>MAXIMUM_SCALE</B> short => maximum scale supported
            *	<LI><B>SQL_DATA_TYPE</B> int => unused
            *	<LI><B>SQL_DATETIME_SUB</B> int => unused
            *	<LI><B>NUM_PREC_RADIX</B> int => usually 2 or 10
            *  </OL>
        */
        val retVal = ArrayResultSet()
        retVal.setColumnNames(
            listOf(
                "TYPE_NAME", "DATA_TYPE", "PRECISION", "LITERAL_PREFIX",
                "LITERAL_SUFFIX", "CREATE_PARAMS", "NULLABLE", "CASE_SENSITIVE", "SEARCHABLE",
                "UNSIGNED_ATTRIBUTE", "FIXED_PREC_SCALE", "AUTO_INCREMENT", "LOCAL_TYPE_NAME", "MINIMUM_SCALE",
                "MAXIMUM_SCALE", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "NUM_PREC_RADIX"
            )
        )

        retVal.addRow(
            listOf(
                OBJECT_ID_TYPE_NAME,  // "TYPE_NAME",
                "" + Types.VARCHAR,  // "DATA_TYPE",
                "800",  // "PRECISION",
                "'",  // "LITERAL_PREFIX",
                "'",  // "LITERAL_SUFFIX",
                null,  // "CREATE_PARAMS",
                "" + DatabaseMetaData.typeNullable,  // "NULLABLE",
                "true",  // "CASE_SENSITIVE",
                "" + DatabaseMetaData.typeSearchable,  // "SEARCHABLE",
                "false",  // "UNSIGNED_ATTRIBUTE",
                "false",  // "FIXED_PREC_SCALE",
                "false",  // "AUTO_INCREMENT",
                OBJECT_ID_TYPE_NAME,  // "LOCAL_TYPE_NAME",
                "0",  // "MINIMUM_SCALE",
                "0",  // "MAXIMUM_SCALE",
                null,  // "SQL_DATA_TYPE", (not used)
                null,  // "SQL_DATETIME_SUB", (not used)
                "10",  // "NUM_PREC_RADIX" (javadoc says usually 2 or 10)
            )
        )

        retVal.addRow(
            listOf(
                DOCUMENT_TYPE_NAME,  // "TYPE_NAME",
                "" + Types.CLOB,  // "DATA_TYPE",
                "16777216",  // "PRECISION",
                "'",  // "LITERAL_PREFIX",
                "'",  // "LITERAL_SUFFIX",
                null,  // "CREATE_PARAMS",
                "" + DatabaseMetaData.typeNullable,  // "NULLABLE",
                "true",  // "CASE_SENSITIVE",
                "" + DatabaseMetaData.typeSearchable,  // "SEARCHABLE",
                "false",  // "UNSIGNED_ATTRIBUTE",
                "false",  // "FIXED_PREC_SCALE",
                "false",  // "AUTO_INCREMENT",
                DOCUMENT_TYPE_NAME,  // "LOCAL_TYPE_NAME",
                "0",  // "MINIMUM_SCALE",
                "0",  // "MAXIMUM_SCALE",
                null,  // "SQL_DATA_TYPE", (not used)
                null,  // "SQL_DATETIME_SUB", (not used)
                "10",  // "NUM_PREC_RADIX" (javadoc says usually 2 or 10)
            )
        )
        return retVal
    }


    @Throws(SQLException::class)
    override fun <T> unwrap(iface: Class<T>): T? {
        return null
    }

    @Throws(SQLException::class)
    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun allProceduresAreCallable(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun allTablesAreSelectable(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getURL
     */
    @Throws(SQLException::class)
    override fun getURL(): String {
        return con.url
    }

    @Throws(SQLException::class)
    override fun getUserName(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun isReadOnly(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedHigh(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedLow(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedAtStart(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullsAreSortedAtEnd(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getDatabaseProductName
     */
    @Throws(SQLException::class)
    override fun getDatabaseProductName(): String {
        return "Mongo DB"
    }

    /**
     * @see java.sql.DatabaseMetaData.getDatabaseProductVersion
     */
    @Throws(SQLException::class)
    override fun getDatabaseProductVersion(): String {
        val version = con.client.version
        return version
    }

    /**
     * @see java.sql.DatabaseMetaData.getDriverName
     */
    @Throws(SQLException::class)
    override fun getDriverName(): String {
        return "MongoDB JDBC Driver"
    }

    /**
     * @see java.sql.DatabaseMetaData.getDriverVersion
     */
    @Throws(SQLException::class)
    override fun getDriverVersion(): String {
        return "1.0"
    }

    /**
     * @see java.sql.DatabaseMetaData.getDriverMajorVersion
     */
    override fun getDriverMajorVersion(): Int {
        return 1
    }

    /**
     * @see java.sql.DatabaseMetaData.getDriverMinorVersion
     */
    override fun getDriverMinorVersion(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun usesLocalFiles(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun usesLocalFilePerTable(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMixedCaseIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesUpperCaseIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesLowerCaseIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesMixedCaseIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsMixedCaseQuotedIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesUpperCaseQuotedIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesLowerCaseQuotedIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun storesMixedCaseQuotedIdentifiers(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getIdentifierQuoteString(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getSQLKeywords(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getNumericFunctions(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getStringFunctions(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getSystemFunctions(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getTimeDateFunctions(): String {
        return "date"
    }

    @Throws(SQLException::class)
    override fun getSearchStringEscape(): String? {
        return null
    }

    @Throws(SQLException::class)
    override fun getExtraNameCharacters(): String? {
        return null
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsAlterTableWithAddColumn
     */
    @Throws(SQLException::class)
    override fun supportsAlterTableWithAddColumn(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsAlterTableWithDropColumn
     */
    @Throws(SQLException::class)
    override fun supportsAlterTableWithDropColumn(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsColumnAliasing(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun nullPlusNonNullIsNull(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsConvert(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsConvert(
        fromType: Int,
        toType: Int,
    ): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsTableCorrelationNames(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsDifferentTableCorrelationNames(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsExpressionsInOrderBy(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOrderByUnrelated(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupBy(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupByUnrelated(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsGroupByBeyondSelect(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsLikeEscapeClause(): Boolean {
        return true
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsMultipleResultSets
     */
    @Throws(SQLException::class)
    override fun supportsMultipleResultSets(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsMultipleTransactions
     */
    @Throws(SQLException::class)
    override fun supportsMultipleTransactions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsNonNullableColumns
     */
    @Throws(SQLException::class)
    override fun supportsNonNullableColumns(): Boolean {
        return true
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsMinimumSQLGrammar
     */
    @Throws(SQLException::class)
    override fun supportsMinimumSQLGrammar(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsCoreSQLGrammar
     */
    @Throws(SQLException::class)
    override fun supportsCoreSQLGrammar(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsExtendedSQLGrammar
     */
    @Throws(SQLException::class)
    override fun supportsExtendedSQLGrammar(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsANSI92EntryLevelSQL
     */
    @Throws(SQLException::class)
    override fun supportsANSI92EntryLevelSQL(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsANSI92IntermediateSQL
     */
    @Throws(SQLException::class)
    override fun supportsANSI92IntermediateSQL(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsANSI92FullSQL
     */
    @Throws(SQLException::class)
    override fun supportsANSI92FullSQL(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsIntegrityEnhancementFacility
     */
    @Throws(SQLException::class)
    override fun supportsIntegrityEnhancementFacility(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsOuterJoins
     */
    @Throws(SQLException::class)
    override fun supportsOuterJoins(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsFullOuterJoins
     */
    @Throws(SQLException::class)
    override fun supportsFullOuterJoins(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsLimitedOuterJoins
     */
    @Throws(SQLException::class)
    override fun supportsLimitedOuterJoins(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getSchemaTerm
     */
    @Throws(SQLException::class)
    override fun getSchemaTerm(): String? {
        return null
    }

    /**
     * @see java.sql.DatabaseMetaData.getProcedureTerm
     */
    @Throws(SQLException::class)
    override fun getProcedureTerm(): String? {
        return null
    }

    /**
     * @see java.sql.DatabaseMetaData.getCatalogTerm
     */
    @Throws(SQLException::class)
    override fun getCatalogTerm(): String {
        return "database"
    }

    /**
     * @see java.sql.DatabaseMetaData.isCatalogAtStart
     */
    @Throws(SQLException::class)
    override fun isCatalogAtStart(): Boolean {
        return true
    }

    /**
     * @see java.sql.DatabaseMetaData.getCatalogSeparator
     */
    @Throws(SQLException::class)
    override fun getCatalogSeparator(): String {
        return "."
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSchemasInDataManipulation
     */
    @Throws(SQLException::class)
    override fun supportsSchemasInDataManipulation(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSchemasInProcedureCalls
     */
    @Throws(SQLException::class)
    override fun supportsSchemasInProcedureCalls(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSchemasInTableDefinitions
     */
    @Throws(SQLException::class)
    override fun supportsSchemasInTableDefinitions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSchemasInIndexDefinitions
     */
    @Throws(SQLException::class)
    override fun supportsSchemasInIndexDefinitions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSchemasInPrivilegeDefinitions
     */
    @Throws(SQLException::class)
    override fun supportsSchemasInPrivilegeDefinitions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsCatalogsInDataManipulation
     */
    @Throws(SQLException::class)
    override fun supportsCatalogsInDataManipulation(): Boolean {
        return true
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsCatalogsInProcedureCalls
     */
    @Throws(SQLException::class)
    override fun supportsCatalogsInProcedureCalls(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsCatalogsInTableDefinitions
     */
    @Throws(SQLException::class)
    override fun supportsCatalogsInTableDefinitions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCatalogsInIndexDefinitions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsCatalogsInPrivilegeDefinitions
     */
    @Throws(SQLException::class)
    override fun supportsCatalogsInPrivilegeDefinitions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsPositionedDelete
     */
    @Throws(SQLException::class)
    override fun supportsPositionedDelete(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsPositionedUpdate
     */
    @Throws(SQLException::class)
    override fun supportsPositionedUpdate(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSelectForUpdate
     */
    @Throws(SQLException::class)
    override fun supportsSelectForUpdate(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsStoredProcedures
     */
    @Throws(SQLException::class)
    override fun supportsStoredProcedures(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInComparisons(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInExists(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInIns(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsSubqueriesInQuantifieds(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsCorrelatedSubqueries(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsUnion(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsUnionAll(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenCursorsAcrossCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenCursorsAcrossRollback(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenStatementsAcrossCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsOpenStatementsAcrossRollback(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getMaxBinaryLiteralLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCharLiteralLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInGroupBy(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInIndex(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInOrderBy(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInSelect(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxColumnsInTable(): Int {
        return 0
    }

    /**
     * @see java.sql.DatabaseMetaData.getMaxConnections
     */
    @Throws(SQLException::class)
    override fun getMaxConnections(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCursorNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxIndexLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxSchemaNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxProcedureNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxCatalogNameLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxRowSize(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun doesMaxRowSizeIncludeBlobs(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getMaxStatementLength(): Int {
        return 0
    }

    @Throws(SQLException::class)
    override fun getMaxStatements(): Int {
        return 0
    }

    /**
     * @see java.sql.DatabaseMetaData.getMaxTableNameLength
     */
    @Throws(SQLException::class)
    override fun getMaxTableNameLength(): Int {
        /*
        * The maximum size of a collection name is 128 characters (including the name of the db and indexes).
        * It is probably best to keep it under 80/90 chars.
        */
        return 90
    }

    /**
     * @see java.sql.DatabaseMetaData.getMaxTablesInSelect
     */
    @Throws(SQLException::class)
    override fun getMaxTablesInSelect(): Int {
        // MongoDB collections are represented as SQL tables in this driver. Mongo doesn't support joins.
        return 1
    }

    @Throws(SQLException::class)
    override fun getMaxUserNameLength(): Int {
        return 0
    }

    /**
     * @see java.sql.DatabaseMetaData.getDefaultTransactionIsolation
     */
    @Throws(SQLException::class)
    override fun getDefaultTransactionIsolation(): Int {
        return Connection.TRANSACTION_NONE
    }

    /**
     * MongoDB doesn't support transactions, but document updates are atomic.
     *
     * @see java.sql.DatabaseMetaData.supportsTransactions
     */
    @Throws(SQLException::class)
    override fun supportsTransactions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsTransactionIsolationLevel
     */
    @Throws(SQLException::class)
    override fun supportsTransactionIsolationLevel(level: Int): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsDataDefinitionAndDataManipulationTransactions
     */
    @Throws(SQLException::class)
    override fun supportsDataDefinitionAndDataManipulationTransactions(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsDataManipulationTransactionsOnly(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun dataDefinitionCausesTransactionCommit(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun dataDefinitionIgnoredInTransactions(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getProcedures
     */
    @Throws(SQLException::class)
    override fun getProcedures(
        catalogName: String?,
        schemaPattern: String?,
        procedureNamePattern: String?,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(
            listOf(
                "PROCEDURE_CAT", "PROCEDURE_SCHEMA", "PROCEDURE_NAME", "REMARKS",
                "PROCEDURE_TYPE", "SPECIFIC_NAME"
            )
        )
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getProcedureColumns
     */
    @Throws(SQLException::class)
    override fun getProcedureColumns(
        catalogName: String?,
        schemaPattern: String?,
        procedureNamePattern: String?,
        columnNamePattern: String?,
    ): ResultSet {
        return EMPTY_RESULT_SET
    }

    private fun supportedTableTypes(): List<String?> {
        return if (con.client.expandResultSet) {
            listOf("TABLE")
        } else {
            listOf("COLLECTION")
        }
    }

    /**
     * @see java.sql.DatabaseMetaData.getTableTypes
     */
    @Throws(SQLException::class)
    override fun getTableTypes(): ResultSet {
        val result = ArrayResultSet()

        result.addRow(supportedTableTypes())

        return result
    }


    @Throws(SQLException::class)
    override fun getColumnPrivileges(
        catalogName: String?,
        schemaName: String?,
        table: String?,
        columnNamePattern: String?,
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getTablePrivileges(
        catalogName: String,
        schemaPattern: String,
        tableNamePattern: String,
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getBestRowIdentifier(
        catalogName: String,
        schemaName: String,
        table: String,
        scope: Int,
        nullable: Boolean,
    ): ResultSet? {
        return null
    }

    /**
     * @see java.sql.DatabaseMetaData.getVersionColumns
     */
    override fun getVersionColumns(
        catalogName: String,
        schemaName: String,
        table: String,
    ): ResultSet {
        return EMPTY_RESULT_SET
    }

    override fun getExportedKeys(
        catalogName: String,
        schemaName: String?,
        tableNamePattern: String?,
    ): ResultSet {
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "PKTABLE_CAT",
                "PKTABLE_SCHEMA",
                "PKTABLE_NAME",
                "PKCOLUMN_NAME",
                "FKTABLE_CAT",
                "FKTABLE_SCHEM",
                "FKTABLE_NAME",
                "FKCOLUMN_NAME",
                "KEY_SEQ",
                "UPDATE_RULE",
                "DELETE_RULE",
                "FK_NAME",
                "PK_NAME",
                "DEFERRABILITY"
            )
        )

        val db = con.client.getDatabase(catalogName)
        db.metaDatabase.discoverReferences(db)
        val pkCollection = db.getMetaCollection(tableNamePattern)
        if (pkCollection != null) {
            for (fromCollection in db.metaDatabase.getMetaCollections()) {
                for (fromFiled in fromCollection.fields) {
                    getExportedKeysRecursive(result, pkCollection, fromCollection, fromFiled)
                }
            }
        }
        return result
    }

    private fun getExportedKeysRecursive(
        result: ArrayResultSet,
        pkCollection: MetaCollection,
        fromCollection: MetaCollection,
        fromFiled: MetaField,
    ) {
        for (iReference in fromFiled.references) {
            if (iReference.pkCollection == pkCollection) {
                result.addRow(
                    listOf(
                        pkCollection.metaDatabase.name,  //PKTABLE_CAT
                        null,  //PKTABLE_SCHEM
                        pkCollection.name,  //PKTABLE_NAME
                        "_id",  //PKCOLUMN_NAME
                        fromCollection.metaDatabase.name,  //FKTABLE_CAT
                        null,  //FKTABLE_SCHEM
                        fromFiled.metaCollection.name,  //FKTABLE_NAME
                        iReference.fromField.nameWithPath,  //FKCOLUMN_NAME
                        "1",  //KEY_SEQ 1,2
                        "" + DatabaseMetaData.importedKeyNoAction,  //UPDATE_RULE
                        "" + DatabaseMetaData.importedKeyNoAction,  //DELETE_RULE
                        "Relationship",  //FK_NAME
                        null,  //PK_NAME
                        "" + DatabaseMetaData.importedKeyInitiallyImmediate //DEFERRABILITY
                    )
                )
            }
        }
        if (fromFiled is MetaObject) {
            for (field in fromFiled.fields) {
                getExportedKeysRecursive(result, pkCollection, fromCollection, field)
            }
        }
    }

    /**
     * @see java.sql.DatabaseMetaData.getExportedKeys
     */
    @Throws(SQLException::class)
    override fun getImportedKeys(
        catalogName: String,
        schemaName: String?,
        tableNamePattern: String?,
    ): ResultSet {
        val result = ArrayResultSet()
        result.setColumnNames(
            listOf(
                "PKTABLE_CAT",
                "PKTABLE_SCHEM",
                "PKTABLE_NAME",
                "PKCOLUMN_NAME",
                "FKTABLE_CAT",
                "FKTABLE_SCHEM",
                "FKTABLE_NAME",
                "FKCOLUMN_NAME",
                "KEY_SEQ",
                "UPDATE_RULE",
                "DELETE_RULE",
                "FK_NAME",
                "PK_NAME",
                "DEFERRABILITY"
            )
        )


        val db = con.client.getDatabase(catalogName)
        val fromCollection = db.getMetaCollection(tableNamePattern)
        db.metaDatabase.discoverReferences(db)
        if (fromCollection != null) {
            for (fromFiled in fromCollection.fields) {
                getImportedKeysRecursive(result, fromFiled)
            }
        }
        return result
    }

    private fun getImportedKeysRecursive(
        result: ArrayResultSet,
        fromFiled: MetaField,
    ) {
        for (reference in fromFiled.references) {
            result.addRow(
                listOf(
                    reference.pkCollection.metaDatabase.name,  //PKTABLE_CAT
                    null,  //PKTABLE_SCHEMA
                    reference.pkCollection.name,  //PKTABLE_NAME
                    "_id",  //PKCOLUMN_NAME
                    reference.fromField.metaCollection.metaDatabase.name,  //FKTABLE_CAT
                    null,  //FKTABLE_SCHEM
                    reference.fromField.metaCollection.name,  //FKTABLE_NAME
                    reference.fromField.nameWithPath,  //FKCOLUMN_NAME
                    "1",  //KEY_SEQ 1,2
                    "" + DatabaseMetaData.importedKeyNoAction,  //UPDATE_RULE
                    "" + DatabaseMetaData.importedKeyNoAction,  //DELETE_RULE
                    "Relationship",  //FK_NAME
                    null,  //PK_NAME
                    "" + DatabaseMetaData.importedKeyInitiallyImmediate //DEFERRABILITY
                )
            )
        }
        if (fromFiled is MetaObject) {
            for (field in fromFiled.fields) {
                getImportedKeysRecursive(result, field)
            }
        }
    }

    /**
     * @see java.sql.DatabaseMetaData.getCrossReference
     */
    @Throws(SQLException::class)
    override fun getCrossReference(
        parentCatalog: String,
        parentSchema: String,
        parentTable: String,
        foreignCatalog: String,
        foreignSchema: String,
        foreignTable: String,
    ): ResultSet {
        return EMPTY_RESULT_SET
    }


    /**
     * @see java.sql.DatabaseMetaData.supportsResultSetType
     */
    @Throws(SQLException::class)
    override fun supportsResultSetType(type: Int): Boolean {
        return type == ResultSet.TYPE_FORWARD_ONLY
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsResultSetConcurrency
     */
    @Throws(SQLException::class)
    override fun supportsResultSetConcurrency(
        type: Int,
        concurrency: Int,
    ): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownUpdatesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownDeletesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun ownInsertsAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersUpdatesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersDeletesAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun othersInsertsAreVisible(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun updatesAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun deletesAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun insertsAreDetected(type: Int): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsBatchUpdates(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getUDTs
     */
    @Throws(SQLException::class)
    override fun getUDTs(
        catalogName: String,
        schemaPattern: String?,
        typeNamePattern: String?,
        types: IntArray?,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(
            listOf(
                "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE",
                "REMARKS", "BASE_TYPE",
            )
        )
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getConnection
     */
    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        return con
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsSavepoints
     */
    @Throws(SQLException::class)
    override fun supportsSavepoints(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun supportsNamedParameters(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsMultipleOpenResults
     */
    @Throws(SQLException::class)
    override fun supportsMultipleOpenResults(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsGetGeneratedKeys
     */
    @Throws(SQLException::class)
    override fun supportsGetGeneratedKeys(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getSuperTypes
     */
    @Throws(SQLException::class)
    override fun getSuperTypes(
        catalogName: String,
        schemaPattern: String,
        typeNamePattern: String,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(
            listOf(
                "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT",
                "SUPERTYPE_SCHEM", "SUPERTYPE_NAME"
            )
        )
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getSuperTables
     */
    @Throws(SQLException::class)
    override fun getSuperTables(
        catalogName: String,
        schemaPattern: String,
        tableNamePattern: String,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(listOf("TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME"))
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.getAttributes
     */
    @Throws(SQLException::class)
    override fun getAttributes(
        catalogName: String,
        schemaPattern: String,
        typeNamePattern: String,
        attributeNamePattern: String,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(
            listOf(
                "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE",
                "ATTR_TYPE_NAME", "ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS",
                "ATTR_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
                "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE"
            )
        )
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsResultSetHoldability
     */
    @Throws(SQLException::class)
    override fun supportsResultSetHoldability(holdability: Int): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getResultSetHoldability
     */
    @Throws(SQLException::class)
    override fun getResultSetHoldability(): Int {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT
    }

    /**
     * @see java.sql.DatabaseMetaData.getDatabaseMajorVersion
     */
    @Throws(SQLException::class)
    override fun getDatabaseMajorVersion(): Int {
        return (databaseProductVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())[0].toInt()
    }

    /**
     * @see java.sql.DatabaseMetaData.getDatabaseMinorVersion
     */
    @Throws(SQLException::class)
    override fun getDatabaseMinorVersion(): Int {
        return (databaseProductVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())[1].toInt()
    }

    /**
     * @see java.sql.DatabaseMetaData.getJDBCMajorVersion
     */
    @Throws(SQLException::class)
    override fun getJDBCMajorVersion(): Int {
        return 1
    }

    /**
     * @see java.sql.DatabaseMetaData.getJDBCMinorVersion
     */
    @Throws(SQLException::class)
    override fun getJDBCMinorVersion(): Int {
        return 0
    }

    /**
     * @see java.sql.DatabaseMetaData.getSQLStateType
     */
    @Throws(SQLException::class)
    override fun getSQLStateType(): Int {
        return DatabaseMetaData.sqlStateXOpen
    }

    @Throws(SQLException::class)
    override fun locatorsUpdateCopy(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsStatementPooling
     */
    @Throws(SQLException::class)
    override fun supportsStatementPooling(): Boolean {
        return false
    }

    /**
     * @see java.sql.DatabaseMetaData.getRowIdLifetime
     */
    @Throws(SQLException::class)
    override fun getRowIdLifetime(): RowIdLifetime? {
        return null
    }

    /**
     * @see java.sql.DatabaseMetaData.getSchemas
     */
    @Throws(SQLException::class)
    override fun getSchemas(
        catalogName: String,
        schemaPattern: String,
    ): ResultSet {
        val retVal = ArrayResultSet()
        retVal.setColumnNames(listOf("TABLE_SCHEM", "TABLE_CATALOG"))
        return retVal
    }

    /**
     * @see java.sql.DatabaseMetaData.supportsStoredFunctionsUsingCallSyntax
     */
    @Throws(SQLException::class)
    override fun supportsStoredFunctionsUsingCallSyntax(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun autoCommitFailureClosesAllResultSets(): Boolean {
        return false
    }

    @Throws(SQLException::class)
    override fun getClientInfoProperties(): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getFunctions(
        catalogName: String,
        schemaPattern: String?,
        functionNamePattern: String?,
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getFunctionColumns(
        catalogName: String,
        schemaPattern: String?,
        functionNamePattern: String?,
        columnNamePattern: String?,
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun getPseudoColumns(
        catalogName: String,
        schemaPattern: String,
        tableNamePattern: String,
        columnNamePattern: String,
    ): ResultSet? {
        return null
    }

    @Throws(SQLException::class)
    override fun generatedKeyAlwaysReturned(): Boolean {
        return false
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()

        private val EMPTY_RESULT_SET = ArrayResultSet()
        private const val OBJECT_ID_TYPE_NAME = "OBJECT_ID"
        private const val DOCUMENT_TYPE_NAME = "DOCUMENT"
    }
}
