package com.wisecoders.jdbc.mongodb.wrappers

import com.google.gson.GsonBuilder
import com.mongodb.client.ListCollectionsIterable
import com.mongodb.client.MongoDatabase
import com.mongodb.client.MongoIterable
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.ValidationOptions
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.GraalConvertor.toBson
import com.wisecoders.jdbc.mongodb.GraalConvertor.toList
import com.wisecoders.jdbc.mongodb.ScanStrategy
import com.wisecoders.jdbc.mongodb.Util.getByPath
import com.wisecoders.jdbc.mongodb.structure.MetaCollection
import com.wisecoders.jdbc.mongodb.structure.MetaDatabase
import org.bson.Document
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.slf4j.Logger

/**
 * Wrapper class around MongoDatabase with direct access to collections as member variables.
 *
 * Copyright Wise Coders GmbH. The MongoDB JDBC driver is build to be used with  [DbSchema Database Designer](https://dbschema.com)
 * Free to use by everyone, code modifications allowed only to the  [public repository](https://github.com/wise-coders/mongodb-jdbc-driver)
 */
class WrappedMongoDatabase internal constructor(
    private val mongoDatabase: MongoDatabase,
    private val scanStrategy: ScanStrategy,
    private val sortFields: Boolean,
) :
    ProxyObject {
    val metaDatabase: MetaDatabase = MetaDatabase(mongoDatabase.name)

    init {
        try {
            if (("config" != mongoDatabase.name) && ("admin" != mongoDatabase.name) && ("local" != mongoDatabase.name)) {
                for (info in mongoDatabase.listCollections()) {
                    val definition = getByPath(info, "options.validator.\$jsonSchema") as Document?
                    if (definition != null) {
                        val name = info.getString("name")
                        val metaCollection = metaDatabase.createMetaCollection(name, false)
                        try {
                            metaCollection.visitValidatorNode(null, true, definition, sortFields)
                        } catch (ex: Throwable) {
                            LOGGER.atError().setMessage(
                                """
                                    Error parsing validation rule for $name
                                    
                                    ${GsonBuilder().setPrettyPrinting().create().toJson(definition)}
                                    
                                    """.trimIndent()
                            ).setCause(ex).log()
                            metaDatabase.dropMetaCollection(name)
                        }
                        metaCollection.scanIndexes(getCollection(name))
                    }
                }
            }
        } catch (ex: Throwable) {
            LOGGER.atError().setMessage(
                "Error listing database '" + mongoDatabase.name + "' collections\n\n"
            ).setCause(ex).log()
        }
    }

    fun getMetaCollectionIfAlreadyLoaded(collectionName: String?): MetaCollection? {
        if (collectionName == null || collectionName.isEmpty()) return null

        return metaDatabase.getMetaCollection(collectionName)
    }


    fun getMetaCollection(collectionName: String?): MetaCollection? {
        if (collectionName.isNullOrEmpty()) return null

        val metaCollection = metaDatabase.getMetaCollection(collectionName)
        if (metaCollection == null) {
            try {
                return metaDatabase.createMetaCollection(collectionName, true)
                    .scanDocumentsAndIndexes(getCollection(collectionName), scanStrategy, sortFields)
            } catch (ex: Throwable) {
                LOGGER.atError().setMessage(
                    "Error discovering collection " + mongoDatabase.name + "." + collectionName + ". "
                ).setCause(ex).log()
            }
        } else {
            return metaCollection
        }
        return null
    }

    override fun hasMember(key: String): Boolean {
        return true
    }

    override fun getMember(key: String): Any {
        return when (key) {
            "createView" -> CreateViewProxyExecutable()
            "getCollection" -> GetCollectionProxyExecutable()
            "createCollection" -> CreateCollectionProxyExecutable()
            "runCommand" -> RunCommandProxyExecutable()
            "drop" -> DropProxyExecutable()
            "listCollectionNames" -> ListCollectionNamesProxyExecutable()
            "listCollections" -> ListCollectionsProxyExecutable()
            "getViewSource" -> GetViewSourceProxyExecutable()
            "getName" -> GetNameProxyExecutable()
            else -> getCollection(key)
        }
    }

    fun getCollection(collectionName: String): WrappedMongoCollection<Document> {
        return WrappedMongoCollection(this, mongoDatabase.getCollection(collectionName))
    }

    override fun getMemberKeys(): Any {
        val keys: MutableSet<String> = LinkedHashSet()
        for (name in mongoDatabase.listCollectionNames()) {
            keys.add(name)
        }
        return keys.toTypedArray()
    }

    override fun putMember(
        key: String,
        value: Value,
    ) {
    }


    override fun toString(): String {
        return mongoDatabase.name
    }

    private inner class CreateViewProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.size == 3 && args[0].isString && args[1].isString && args[2].hasArrayElements()) {
                mongoDatabase.createView(
                    args[0].asString(), args[1].asString(), toList(args[2])
                )
            }
            return null
        }
    }

    private inner class GetCollectionProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.size == 1 && args[0].isString) {
                LOGGER.atInfo().setMessage(
                    "Get collection " + args[0].asString() + " " + getCollection(
                        args[0].asString()
                    )
                ).log()
                return getCollection(args[0].asString())
            }
            return null
        }
    }

    private inner class CreateCollectionProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.size == 1 && args[0].isString) {
                mongoDatabase.createCollection(args[0].asString())
            }
            if (args.size == 2 && args[0].isString) {
                if (args[1].isHostObject) {
                    mongoDatabase.createCollection(args[0].asString(), args[1].asHostObject())
                }
                val map: Map<*, *> = args[1].`as`(MutableMap::class.java)
                val options = CreateCollectionOptions()
                if (map.containsKey("validator")) {
                    options.validationOptions(ValidationOptions().validator(toBson(map["validator"])))
                }
                if (map.containsKey("storageEngine")) {
                    options.storageEngineOptions(toBson(map["storageEngine"]))
                }
                if (map.containsKey("capped")) {
                    options.capped(map["capped"].toString().toBoolean())
                }
                if (map.containsKey("max")) {
                    options.maxDocuments(map["max"].toString().toLong())
                }
                mongoDatabase.createCollection(args[0].asString(), options)
            }
            return null
        }
    }

    private inner class RunCommandProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.size == 1) {
                return mongoDatabase.runCommand(toBson(args[0].`as`(MutableMap::class.java)))
            }
            return null
        }
    }

    private inner class DropProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.isEmpty()) {
                mongoDatabase.drop()
            }
            return null
        }
    }

    private inner class ListCollectionNamesProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.isEmpty()) {
                return mongoDatabase.listCollectionNames()
            }
            return null
        }
    }

    private inner class ListCollectionsProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.isEmpty()) {
                return mongoDatabase.listCollections()
            }
            return null
        }
    }

    private inner class GetNameProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.isEmpty()) {
                return mongoDatabase.name
            }
            return null
        }
    }

    private inner class GetViewSourceProxyExecutable : ProxyExecutable {
        override fun execute(vararg args: Value): Any? {
            if (args.size == 1 && args[0].isString) {
                for (doc in mongoDatabase.listCollections()) {
                    if (args[0].asString() == doc["name"] && "view" == doc["type"]) {
                        val options = doc["options"] as Document?
                        val sb = StringBuilder()
                        sb.append(mongoDatabase.name).append(".createView(\n\t\"")
                        sb.append(doc["name"]).append("\",\n\t\"").append(options!!["viewOn"]).append("\",\n\t[\n\t\t")
                        var addComma = false
                        for (d in (options["pipeline"] as List<*>?)!!) {
                            if (addComma) sb.append(",\n\t\t")
                            if (d is Document) {
                                sb.append(d.toJson())
                            } else {
                                sb.append(d)
                            }
                            addComma = true
                        }
                        sb.append("\n\t]\n)")
                        val ret = Document()
                        ret["source"] = sb.toString()
                        return ret
                    }
                }
            }
            return null
        }
    }

    val name: String
        get() = mongoDatabase.name

    fun listCollectionNames(): MongoIterable<String> {
        return mongoDatabase.listCollectionNames()
    }

    fun listCollections(): ListCollectionsIterable<Document> {
        return mongoDatabase.listCollections()
    }

    fun createCollection(s: String) {
        mongoDatabase.createCollection(s)
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()
    }
}
