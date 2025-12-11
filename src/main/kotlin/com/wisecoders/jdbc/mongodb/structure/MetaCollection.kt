package com.wisecoders.jdbc.mongodb.structure

import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.ScanStrategy
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoCollection
import org.bson.types.ObjectId
import org.slf4j.Logger

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MetaCollection(
    val metaDatabase: MetaDatabase,
    name: String?,
    val isVirtual: Boolean,
) : MetaObject(null, name) {

    val metaIndexes: MutableList<MetaIndex> = ArrayList()

    fun createMetaIndex(
        name: String,
        pk: Boolean,
        unique: Boolean,
    ): MetaIndex {
        val index = MetaIndex(this, name, pk, unique)
        metaIndexes.add(index)
        return index
    }

    fun scanDocumentsAndIndexes(
        mongoCollection: WrappedMongoCollection<*>,
        strategy: ScanStrategy,
        sortFields: Boolean,
    ): MetaCollection {
        scanDocuments(mongoCollection, strategy, sortFields)
        scanIndexes(mongoCollection)
        return this
    }

    private fun scanDocuments(
        mongoCollection: WrappedMongoCollection<*>,
        strategy: ScanStrategy,
        sortFields: Boolean,
    ) {
        val scanStartTime = System.currentTimeMillis()
        var cnt = scan(mongoCollection, strategy, true, sortFields)
        if (fieldCount < 400 && cnt == strategy.SCAN_COUNT && strategy != ScanStrategy.full) {
            cnt += scan(mongoCollection, strategy, false, sortFields)
        }
        LOGGER.atInfo().setMessage(
            """Scanned $mongoCollection $cnt documents, $fieldCount fields in ${System.currentTimeMillis() - scanStartTime}ms"""
        ).log()

    }

    private fun scan(
        mongoCollection: WrappedMongoCollection<*>,
        strategy: ScanStrategy,
        directionUp: Boolean,
        sortFields: Boolean,
    ): Long {
        var cnt: Long = 0
        mongoCollection.find().sort("{_id:" + (if (directionUp) "1" else "-1") + "}").iterator().use { cursor ->
            while (cursor.hasNext() && cnt < strategy.SCAN_COUNT) {
                scanDocument(cursor.next(), sortFields, 0)
                cnt++
            }
        }
        return cnt
    }

    init {
        typeName = "object"
        javaType = TYPE_OBJECT
        val idField = MetaField(this, "_id")
        idField.isMandatory = true
        idField.setTypeClass(ObjectId::class.java)
        fields.add(idField)
        val pkId = createMetaIndex("_id_", true, false)
        pkId.addColumn(idField)
    }

    fun scanIndexes(mongoCollection: WrappedMongoCollection<*>) {
        try {
            val iterable = mongoCollection.listIndexes()
            for (indexObject in iterable) {
                if (indexObject is Map<*, *>) {
                    val indexMap = indexObject as Map<*, *>
                    val indexName = indexMap[KEY_NAME].toString()
                    val indexIsPk = "_id_".endsWith(indexName)
                    val indexIsUnique = java.lang.Boolean.TRUE == indexMap[KEY_UNIQUE]
                    val columnsObj = indexMap[KEY_KEY]
                    if (columnsObj is Map<*, *>) {
                        val metaIndex = createMetaIndex(indexName, indexIsPk, indexIsUnique)
                        for (fieldNameObj in columnsObj.keys) {
                            val metaField = findField(fieldNameObj as String)
                            if (metaField == null) {
                                LOGGER.atInfo().setMessage(
                                    "MongoJDBC discover index cannot find metaField '$fieldNameObj' for index $indexObject"
                                ).log()
                            } else {
                                metaIndex.addColumn(metaField)
                            }
                        }
                    }
                }
            }
        } catch (ex: Throwable) {
            LOGGER.atError().setMessage(
                "Error in discover indexes $nameWithPath. "
            ).setCause(ex).log()
        }
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()

        private const val KEY_NAME = "name"
        private const val KEY_UNIQUE = "unique"
        private const val KEY_KEY = "key"
    }
}
