package com.wisecoders.jdbc.mongodb.structure

import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoDatabase
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.Logger

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MetaDatabase(@JvmField val name: String) {
    private val metaCollections: MutableMap<String, MetaCollection> = HashMap()
    private var referencesDiscovered = false

    fun createMetaCollection(
        name: String,
        isVirtual: Boolean,
    ): MetaCollection {
        val metaCollection = MetaCollection(this, name, isVirtual)
        metaCollections[name] = metaCollection
        return metaCollection
    }

    fun getMetaCollection(name: String): MetaCollection? {
        return metaCollections[name]
    }

    fun getMetaCollections(): Collection<MetaCollection> {
        return metaCollections.values
    }

    fun dropMetaCollection(name: String) {
        metaCollections.remove(name)
    }

    private fun collectFieldsWithObjectId(metaFields: MutableList<MetaField>) {
        for (collection in metaCollections.values) {
            collection.collectFieldsWithObjectId(metaFields)
        }
    }

    fun discoverReferences(mongoDatabase: WrappedMongoDatabase) {
        if (!referencesDiscovered) {
            try {
                LOGGER.atInfo().setMessage("Discover relationships in database $name").log()
                referencesDiscovered = true
                val metaFields: MutableList<MetaField> = ArrayList()
                collectFieldsWithObjectId(metaFields)
                if (!metaFields.isEmpty()) {
                    for (_metaCollection in getMetaCollections()) {
                        val mongoCollection = mongoDatabase.getCollection(_metaCollection.name)
                        val objectIds = arrayOfNulls<ObjectId>(metaFields.size)

                        for (i in metaFields.indices) {
                            objectIds[i] = metaFields[i].getObjectId()
                        }
                        val inQuery = Document()
                        inQuery["\$in"] = objectIds
                        val query = Document() //new BasicDBObject();
                        query["_id"] = inQuery
                        for (obj in mongoCollection.find(query).projection("{_id:1}")) {
                            if (obj is Map<*, *>) {
                                val value = obj["_id"]
                                if (value != null) {
                                    for (metaField in metaFields) {
                                        if (value == metaField.getObjectId()) {
                                            metaField.createReferenceTo(_metaCollection)
                                            LOGGER.atInfo().setMessage(
                                                "Found relationship  " + metaField.parentObject?.name + " ( " + metaField.name + " ) ref " + _metaCollection.name
                                            ).log()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                LOGGER.atInfo().setMessage("Discover relationships done.").log()
            } catch (ex: Throwable) {
                LOGGER.atError().setMessage("Error discovering relationships.").setCause(ex).log()
            }
        }
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()
    }
}
