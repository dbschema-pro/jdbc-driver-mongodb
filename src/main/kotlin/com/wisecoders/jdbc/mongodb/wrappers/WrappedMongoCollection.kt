package com.wisecoders.jdbc.mongodb.wrappers

import com.mongodb.MongoNamespace
import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.AggregateIterable
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.ClientSession
import com.mongodb.client.DistinctIterable
import com.mongodb.client.ListIndexesIterable
import com.mongodb.client.MapReduceIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.CreateIndexOptions
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.DropIndexOptions
import com.mongodb.client.model.EstimatedDocumentCountOptions
import com.mongodb.client.model.FindOneAndDeleteOptions
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.InsertOneOptions
import com.mongodb.client.model.RenameCollectionOptions
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.wisecoders.jdbc.mongodb.GraalConvertor.convertMap
import com.wisecoders.jdbc.mongodb.GraalConvertor.toBson
import com.wisecoders.jdbc.mongodb.GraalConvertor.toList
import java.util.concurrent.TimeUnit
import org.bson.BsonString
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson

/**
 * Copyright Wise Coders GmbH. The MongoDB JDBC driver is build to be used with  [DbSchema Database Designer](https://dbschema.com)
 * Free to use by everyone, code modifications allowed only to the  [public repository](https://github.com/wise-coders/mongodb-jdbc-driver)
 */
class WrappedMongoCollection<TDocument : Any> internal constructor(
    val wrappedMongoDatabase: WrappedMongoDatabase,
    private val mongoCollection: MongoCollection<TDocument>
) {


    private fun toDocument(map: Map<*, *>): TDocument {
        return (Document(convertMap(map))) as TDocument
    }

    override fun toString(): String {
        return mongoCollection.namespace.fullName
    }

    fun explain(): Any {
        // Support for explain()
        return find().explain()
    }

    val namespace: MongoNamespace
        get() = mongoCollection.namespace


    val documentClass: Class<*>
        get() = mongoCollection.documentClass


    val codecRegistry: CodecRegistry
        get() = mongoCollection.codecRegistry


    val readPreference: ReadPreference
        get() = mongoCollection.readPreference


    val writeConcern: WriteConcern
        get() = mongoCollection.writeConcern


    val readConcern: ReadConcern
        get() = mongoCollection.readConcern


    fun withDocumentClass(clazz: Class<Any>): WrappedMongoCollection<*> {
        mongoCollection.withDocumentClass<Any>(clazz)
        return this
    }


    fun withCodecRegistry(codecRegistry: CodecRegistry): WrappedMongoCollection<*> {
        mongoCollection.withCodecRegistry(codecRegistry)
        return this
    }


    fun withReadPreference(readPreference: ReadPreference): WrappedMongoCollection<*> {
        mongoCollection.withReadPreference(readPreference)
        return this
    }


    fun withWriteConcern(writeConcern: WriteConcern): WrappedMongoCollection<*> {
        mongoCollection.withWriteConcern(writeConcern)
        return this
    }


    fun withReadConcern(readConcern: ReadConcern): WrappedMongoCollection<*> {
        mongoCollection.withReadConcern(readConcern)
        return this
    }

    fun count(): Long {
        return mongoCollection.countDocuments()
    }

    fun count(filter: Map<*, *>?): Long {
        return mongoCollection.countDocuments(toBson(filter))
    }

    fun count(
        filter: Map<*, *>?,
        options: CountOptions
    ): Long {
        return mongoCollection.countDocuments(toBson(filter), options)
    }


    fun count(clientSession: ClientSession): Long {
        return mongoCollection.countDocuments(clientSession)
    }


    fun count(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): Long {
        return mongoCollection.countDocuments(clientSession, toBson(filter))
    }


    fun count(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        options: CountOptions
    ): Long {
        return mongoCollection.countDocuments(clientSession, toBson(filter), options)
    }


    fun countDocuments(): Long {
        return mongoCollection.countDocuments()
    }


    fun countDocuments(filter: Map<*, *>?): Long {
        return mongoCollection.countDocuments(toBson(filter))
    }


    fun countDocuments(
        filter: Map<*, *>?,
        options: CountOptions
    ): Long {
        return mongoCollection.countDocuments(toBson(filter), options)
    }


    fun countDocuments(clientSession: ClientSession): Long {
        return mongoCollection.countDocuments(clientSession)
    }


    fun countDocuments(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): Long {
        return mongoCollection.countDocuments(clientSession, toBson(filter))
    }


    fun countDocuments(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        options: CountOptions
    ): Long {
        return mongoCollection.countDocuments(clientSession, toBson(filter), options)
    }


    fun estimatedDocumentCount(): Long {
        return mongoCollection.estimatedDocumentCount()
    }


    fun estimatedDocumentCount(options: EstimatedDocumentCountOptions): Long {
        return mongoCollection.estimatedDocumentCount(options)
    }


    fun distinct(
        fieldName: String,
        aClass: Class<Any>
    ): DistinctIterable<*> {
        return mongoCollection.distinct<Any>(fieldName, aClass)
    }

    fun distinct(fieldName: String): DistinctIterable<*> {
        return mongoCollection.distinct(fieldName, BsonString::class.java)
    }


    fun distinct(
        fieldName: String,
        filter: Map<*, *>?,
        aClass: Class<Any>
    ): DistinctIterable<*> {
        return mongoCollection.distinct(fieldName, toBson(filter), aClass)
    }


    fun distinct(
        clientSession: ClientSession,
        fieldName: String,
        aClass: Class<Any>
    ): DistinctIterable<*> {
        return mongoCollection.distinct(clientSession, fieldName, aClass)
    }


    fun distinct(
        clientSession: ClientSession,
        fieldName: String,
        filter: Map<*, *>?,
        aClass: Class<Any>
    ): DistinctIterable<*> {
        return mongoCollection.distinct<Any>(clientSession, fieldName, toBson(filter), aClass)
    }


    fun find(): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find())
    }


    fun find(aClass: Class<Any>): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(aClass))
    }


    fun find(filter: Map<*, *>?): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(toBson(filter)))
    }

    fun find(
        filter: Map<*, *>?,
        projection: Map<*, *>?
    ): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(toBson(filter)).projection(toBson(projection)))
    }


    fun find(
        filter: Map<*, *>?,
        aClass: Class<Any>
    ): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find<Any>(toBson(filter), aClass))
    }


    fun find(clientSession: ClientSession): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(clientSession))
    }


    fun find(
        clientSession: ClientSession,
        aClass: Class<Any>
    ): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(clientSession, aClass))
    }


    fun find(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find(clientSession, toBson(filter)))
    }


    fun find(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        aClass: Class<Any>
    ): WrappedFindIterable<*> {
        return WrappedFindIterable(mongoCollection.find<Any?>(clientSession, toBson(filter), aClass))
    }

    //
    fun findOne(): TDocument? {
        return WrappedFindIterable(mongoCollection.find()).first()
    }


    fun findOne(aClass: Class<TDocument>): TDocument? {
        return WrappedFindIterable<TDocument>(mongoCollection.find(aClass)).first()
    }


    fun findOne(filter: Map<*, *>?): TDocument? {
        return WrappedFindIterable(mongoCollection.find(toBson(filter))).first()
    }

    fun findOne(
        filter: Map<*, *>?,
        projection: Map<*, *>?
    ): TDocument? {
        return WrappedFindIterable(mongoCollection.find(toBson(filter)).projection(toBson(projection))).first()
    }


    fun findOne(
        filter: Map<*, *>?,
        aClass: Class<TDocument>
    ): TDocument? {
        return WrappedFindIterable(mongoCollection.find(toBson(filter), aClass)).first()
    }


    fun findOne(clientSession: ClientSession): TDocument? {
        return WrappedFindIterable(mongoCollection.find(clientSession)).first()
    }


    fun findOne(
        clientSession: ClientSession,
        aClass: Class<TDocument>
    ): TDocument? {
        return WrappedFindIterable(mongoCollection.find(clientSession, aClass)).first()
    }


    fun findOne(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): TDocument? {
        return WrappedFindIterable(mongoCollection.find(clientSession, toBson(filter))).first()
    }


    fun findOne(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        aClass: Class<TDocument>
    ): TDocument? {
        return WrappedFindIterable<TDocument>(mongoCollection.find(clientSession, toBson(filter), aClass)).first()
    }

    fun aggregate(pipeline: List<*>?): AggregateIterable<*> {
        return mongoCollection.aggregate(toList(pipeline))
    }

    fun aggregate(obj: Any?): AggregateIterable<*> {
        val list: MutableList<Bson> = ArrayList<Bson>()
        list.add(toBson(obj))
        return mongoCollection.aggregate(list)
    }

    fun aggregate(
        obj1: Any?,
        obj2: Any?
    ): AggregateIterable<*> {
        val list: MutableList<Bson> = ArrayList<Bson>()
        list.add(toBson(obj1))
        list.add(toBson(obj2))
        return mongoCollection.aggregate(list)
    }

    fun aggregate(
        obj1: Any?,
        obj2: Any?,
        obj3: Any?
    ): AggregateIterable<*> {
        val list: MutableList<Bson> = ArrayList<Bson>()
        list.add(toBson(obj1))
        list.add(toBson(obj2))
        list.add(toBson(obj3))
        return mongoCollection.aggregate(list)
    }

    fun aggregate(
        obj1: Any?,
        obj2: Any?,
        obj3: Any?,
        obj4: Any?
    ): AggregateIterable<*> {
        val list: MutableList<Bson> = ArrayList<Bson>()
        list.add(toBson(obj1))
        list.add(toBson(obj2))
        list.add(toBson(obj3))
        list.add(toBson(obj4))
        return mongoCollection.aggregate(list)
    }


    fun aggregate(
        pipeline: List<*>?,
        aClass: Class<Any>
    ): AggregateIterable<*> {
        return mongoCollection.aggregate(toList(pipeline), aClass)
    }


    fun aggregate(
        clientSession: ClientSession,
        pipeline: List<*>?
    ): AggregateIterable<*> {
        return mongoCollection.aggregate(clientSession, toList(pipeline))
    }


    fun aggregate(
        clientSession: ClientSession,
        pipeline: List<*>?,
        aClass: Class<Any>
    ): AggregateIterable<*> {
        return mongoCollection.aggregate<Any>(clientSession, toList(pipeline), aClass)
    }


    fun watch(): ChangeStreamIterable<*> {
        return mongoCollection.watch()
    }


    fun watch(aClass: Class<*>): ChangeStreamIterable<*> {
        return mongoCollection.watch(aClass)
    }


    fun watch(pipeline: List<Bson>): ChangeStreamIterable<*> {
        return mongoCollection.watch(pipeline)
    }


    fun watch(
        pipeline: List<Bson>,
        aClass: Class<Any>
    ): ChangeStreamIterable<*> {
        return mongoCollection.watch(pipeline, aClass)
    }


    fun watch(clientSession: ClientSession): ChangeStreamIterable<*> {
        return mongoCollection.watch(clientSession)
    }


    fun watch(
        clientSession: ClientSession,
        aClass: Class<Any>
    ): ChangeStreamIterable<*> {
        return mongoCollection.watch<Any>(clientSession, aClass)
    }


    fun watch(
        clientSession: ClientSession,
        pipeline: List<Bson>
    ): ChangeStreamIterable<*> {
        return mongoCollection.watch(clientSession, pipeline)
    }


    fun watch(
        clientSession: ClientSession,
        pipeline: List<Bson>,
        aClass: Class<Any>
    ): ChangeStreamIterable<*> {
        return mongoCollection.watch<Any>(clientSession, pipeline, aClass)
    }


    fun mapReduce(
        mapFunction: String,
        reduceFunction: String
    ): MapReduceIterable<*> {
        return mongoCollection.mapReduce(mapFunction, reduceFunction)
    }

    fun mapReduce(
        mapFunction: Any,
        reduceFunction: Any
    ): MapReduceIterable<*> {
        return mongoCollection.mapReduce(mapFunction.toString(), reduceFunction.toString())
    }


    fun mapReduce(
        mapFunction: String,
        reduceFunction: String,
        aClass: Class<*>
    ): MapReduceIterable<*> {
        return mongoCollection.mapReduce(mapFunction, reduceFunction, aClass)
    }


    fun mapReduce(
        clientSession: ClientSession,
        mapFunction: String,
        reduceFunction: String
    ): MapReduceIterable<*> {
        return mongoCollection.mapReduce(clientSession, mapFunction, reduceFunction)
    }


    fun mapReduce(
        clientSession: ClientSession,
        mapFunction: String,
        reduceFunction: String,
        aClass: Class<Any>
    ): MapReduceIterable<*> {
        return mongoCollection.mapReduce(clientSession, mapFunction, reduceFunction, aClass)
    }


    fun bulkWrite(requests: MutableList<WriteModel<TDocument>>): BulkWriteResult {
        return mongoCollection.bulkWrite(requests)
    }


    fun bulkWrite(
        requests: List<WriteModel<TDocument>>,
        options: BulkWriteOptions
    ): BulkWriteResult {
        return mongoCollection.bulkWrite(requests, options)
    }


    fun bulkWrite(
        clientSession: ClientSession,
        requests: List<WriteModel<TDocument>>
    ): BulkWriteResult {
        return mongoCollection.bulkWrite(clientSession, requests)
    }


    fun bulkWrite(
        clientSession: ClientSession,
        requests: List<WriteModel<TDocument>>,
        options: BulkWriteOptions
    ): BulkWriteResult {
        return mongoCollection.bulkWrite(clientSession, requests, options)
    }


    fun insertOne(input: Map<*, *>) {
        mongoCollection.insertOne(toDocument(input))
    }

    fun insertOne(
        input: Map<*, *>,
        options: InsertOneOptions
    ) {
        mongoCollection.insertOne(toDocument(input), options)
    }


    fun insertOne(
        clientSession: ClientSession,
        input: Map<*, *>
    ) {
        mongoCollection.insertOne(clientSession, toDocument(input))
    }


    fun insertOne(
        clientSession: ClientSession,
        input: Map<*, *>,
        options: InsertOneOptions
    ) {
        mongoCollection.insertOne(clientSession, toDocument(input), options)
    }


    fun insertMany(arr: Array<Any>) {
        for (obj in arr) {
            insertOne(obj as Map<*, *>)
        }
    }

    fun insertMany(list: List<*>) {
        for (obj in list) {
            insertOne(obj as Map<*, *>)
        }
    }

    fun insertMany(obj: Any?) {
        val list: List<*> = toList(obj)
        if (list != null) {
            for (map1 in list as List<Map<*, *>>) {
                insertOne(map1)
            }
        } else if (obj is Map<*, *>) {
            insertOne(obj)
        }
    }

    fun insert(input: Map<*, *>) {
        mongoCollection.insertOne(toDocument(input))
    }


    /*
    public void insertMany(List<Map> list) {
        for ( Map map: list ){
            insertOne( map );
        }
    }
    public void insert(List<Map> list) {
        for ( Map map: list ){
            insertOne( map );
        }
    }*/
    fun insertMany(
        list: List<Map<*, *>?>?,
        options: InsertManyOptions?
    ) {
    }


    fun insertMany(
        clientSession: ClientSession?,
        list: List<*>?
    ) {
    }


    fun insertMany(
        clientSession: ClientSession?,
        list: List<*>?,
        options: InsertManyOptions?
    ) {
    }


    fun deleteOne(filter: Map<*, *>?): DeleteResult {
        return mongoCollection.deleteOne(toBson(filter))
    }


    fun deleteOne(
        filter: Map<*, *>?,
        options: DeleteOptions
    ): DeleteResult {
        return mongoCollection.deleteOne(toBson(filter), options)
    }


    fun deleteOne(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): DeleteResult {
        return mongoCollection.deleteOne(clientSession, toBson(filter))
    }


    fun deleteOne(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        options: DeleteOptions
    ): DeleteResult {
        return mongoCollection.deleteOne(clientSession, toBson(filter), options)
    }

    fun remove(filter: Map<*, *>?): DeleteResult {
        return mongoCollection.deleteMany(toBson(filter))
    }

    fun deleteMany(filter: Map<*, *>?): DeleteResult {
        return mongoCollection.deleteMany(toBson(filter))
    }


    fun deleteMany(
        filter: Map<*, *>?,
        options: DeleteOptions
    ): DeleteResult {
        return mongoCollection.deleteMany(toBson(filter), options)
    }


    fun deleteMany(
        clientSession: ClientSession,
        filter: Map<*, *>?
    ): DeleteResult {
        return mongoCollection.deleteMany(clientSession, toBson(filter))
    }


    fun deleteMany(
        clientSession: ClientSession,
        filter: Map<*, *>?,
        options: DeleteOptions
    ): DeleteResult {
        return mongoCollection.deleteMany(clientSession, toBson(filter), options)
    }


    fun replaceOne(
        filter: Map<*, *>?,
        replacement: Map<*, *>
    ): UpdateResult {
        return mongoCollection.replaceOne(toBson(filter), toDocument(replacement))
    }


    fun replaceOne(
        filter: Bson?,
        replacement: Map<*, *>,
        updateOptions: ReplaceOptions
    ): UpdateResult {
        return mongoCollection.replaceOne(toBson(filter), toDocument(replacement), updateOptions)
    }


    fun replaceOne(
        filter: Bson?,
        replacement: Any?,
        replaceOptions: ReplaceOptions?
    ): UpdateResult? {
        return null
    }


    fun replaceOne(
        clientSession: ClientSession?,
        filter: Bson?,
        replacement: Any?
    ): UpdateResult? {
        return null
    }


    fun replaceOne(
        clientSession: ClientSession?,
        filter: Bson?,
        replacement: Any?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun replaceOne(
        clientSession: ClientSession?,
        filter: Bson?,
        replacement: Any?,
        replaceOptions: ReplaceOptions?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        filter: Map<*, *>?,
        update: Map<*, *>?
    ): UpdateResult {
        return mongoCollection.updateOne(toBson(filter), toBson(update))
    }


    fun updateOne(
        filter: Map<*, *>?,
        update: Map<*, *>?,
        updateOptions: UpdateOptions
    ): UpdateResult {
        return mongoCollection.updateOne(toBson(filter), toBson(update), updateOptions)
    }


    fun updateOne(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        filter: Bson?,
        update: List<*>?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        filter: Bson?,
        update: List<*>?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?
    ): UpdateResult? {
        return null
    }


    fun updateOne(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        filter: Map<*, *>?,
        update: Map<*, *>?
    ): UpdateResult {
        return mongoCollection.updateMany(toBson(filter), toBson(update))
    }

    fun updateMany(
        filter: Bson?,
        update: Bson?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        filter: Map<*, *>?,
        update: List<Bson>
    ): UpdateResult {
        return mongoCollection.updateMany(toBson(filter), update)
    }


    fun updateMany(
        filter: Bson?,
        update: List<*>?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?
    ): UpdateResult? {
        return null
    }


    fun updateMany(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?,
        updateOptions: UpdateOptions?
    ): UpdateResult? {
        return null
    }


    fun findOneAndDelete(filter: Map<*, *>?): Any? {
        return mongoCollection.findOneAndDelete(toBson(filter))
    }


    fun findOneAndDelete(
        filter: Map<*, *>?,
        options: FindOneAndDeleteOptions
    ): Any? {
        return mongoCollection.findOneAndDelete(toBson(filter), options)
    }


    fun findOneAndDelete(
        clientSession: ClientSession?,
        filter: Bson?
    ): Any? {
        return null
    }


    fun findOneAndDelete(
        clientSession: ClientSession?,
        filter: Bson?,
        options: FindOneAndDeleteOptions?
    ): Any? {
        return null
    }


    fun findOneAndReplace(
        filter: Bson?,
        replacement: Any?
    ): Any? {
        return null
    }


    fun findOneAndReplace(
        filter: Bson?,
        replacement: Any?,
        options: FindOneAndReplaceOptions?
    ): Any? {
        return null
    }


    fun findOneAndReplace(
        clientSession: ClientSession?,
        filter: Bson?,
        replacement: Any?
    ): Any? {
        return null
    }


    fun findOneAndReplace(
        clientSession: ClientSession?,
        filter: Bson?,
        replacement: Any?,
        options: FindOneAndReplaceOptions?
    ): Any? {
        return null
    }


    fun update(
        filter: Map<*, *>?,
        update: Map<*, *>?
    ): Any {
        return updateMany(filter, update)
    }


    fun findOneAndUpdate(
        filter: Bson?,
        update: Bson?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        filter: Bson?,
        update: Bson?,
        options: FindOneAndUpdateOptions?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        clientSession: ClientSession?,
        filter: Bson?,
        update: Bson?,
        options: FindOneAndUpdateOptions?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        filter: Bson?,
        update: List<*>?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        filter: Bson?,
        update: List<*>?,
        options: FindOneAndUpdateOptions?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?
    ): Any? {
        return null
    }


    fun findOneAndUpdate(
        clientSession: ClientSession?,
        filter: Bson?,
        update: List<*>?,
        options: FindOneAndUpdateOptions?
    ): Any? {
        return null
    }


    fun drop() {
        mongoCollection.drop()
    }


    fun drop(clientSession: ClientSession) {
        mongoCollection.drop(clientSession)
    }


    fun ensureIndex(keys: Map<*, *>?): String {
        return createIndex(keys)
    }

    fun createIndex(keys: Map<*, *>?): String {
        return mongoCollection.createIndex(toBson(keys))
    }


    fun createIndex(
        keys: Map<*, *>?,
        indexOptions: IndexOptions
    ): String {
        return mongoCollection.createIndex(toBson(keys), indexOptions)
    }

    fun createIndex(
        keys: Map<*, *>?,
        options: Map<*, *>
    ): String {
        val indexOptions = IndexOptions()
        if (options.containsKey(NAME_KEY)) indexOptions.name(options[NAME_KEY].toString())
        if (options.containsKey(PARTIAL_FILTER_EXPRESSION_KEY)) indexOptions.partialFilterExpression(
            toBson(
                options["partialFilterExpression"]
            )
        )
        if (options.containsKey(SPARSE_KEY) && options[SPARSE_KEY] is Boolean) indexOptions.sparse(
            (options[SPARSE_KEY] as Boolean?)!!
        )
        if (options.containsKey(UNIQUE_KEY) && options[UNIQUE_KEY] is Boolean) indexOptions.unique(
            (options[UNIQUE_KEY] as Boolean?)!!
        )
        if (options.containsKey(EXPIRE_AFTER_SECONDS_KEY) && options[EXPIRE_AFTER_SECONDS_KEY] is Number) indexOptions.expireAfter(
            (options[EXPIRE_AFTER_SECONDS_KEY] as Number).toLong(),
            TimeUnit.SECONDS
        )
        return mongoCollection.createIndex(toBson(keys), indexOptions)
    }

    fun createIndex(
        clientSession: ClientSession?,
        keys: Bson?
    ): String? {
        return null
    }


    fun createIndex(
        clientSession: ClientSession?,
        keys: Bson?,
        indexOptions: IndexOptions?
    ): String? {
        return null
    }


    fun createIndexes(indexes: List<IndexModel>): List<String> {
        return mongoCollection.createIndexes(indexes)
    }


    fun createIndexes(
        indexes: List<*>?,
        createIndexOptions: CreateIndexOptions?
    ): List<String>? {
        return null
    }


    fun createIndexes(
        clientSession: ClientSession?,
        indexes: List<*>?
    ): List<String>? {
        return null
    }


    fun createIndexes(
        clientSession: ClientSession?,
        indexes: List<*>?,
        createIndexOptions: CreateIndexOptions?
    ): List<String>? {
        return null
    }

    val indexes: ListIndexesIterable<Document>
        get() = mongoCollection.listIndexes()

    val indexSpecs: Document?
        get() = mongoCollection.listIndexes().first()


    fun listIndexes(): ListIndexesIterable<Document> {
        return mongoCollection.listIndexes()
    }


    fun listIndexes(aClass: Class<Any>): ListIndexesIterable<*> {
        return mongoCollection.listIndexes(aClass)
    }


    fun listIndexes(clientSession: ClientSession): ListIndexesIterable<Document> {
        return mongoCollection.listIndexes(clientSession)
    }


    fun listIndexes(
        clientSession: ClientSession,
        aClass: Class<Any>
    ): ListIndexesIterable<*> {
        return mongoCollection.listIndexes<Any>(clientSession, aClass)
    }


    fun dropIndex(indexName: String) {
        mongoCollection.dropIndex(indexName)
    }


    fun dropIndex(
        indexName: String,
        dropIndexOptions: DropIndexOptions
    ) {
        mongoCollection.dropIndex(indexName, dropIndexOptions)
    }


    fun dropIndex(keys: Map<*, *>?) {
        mongoCollection.dropIndex(toBson(keys))
    }


    fun dropIndex(
        keys: Map<*, *>?,
        dropIndexOptions: DropIndexOptions
    ) {
        mongoCollection.dropIndex(toBson(keys), dropIndexOptions)
    }


    fun dropIndex(
        clientSession: ClientSession?,
        indexName: String?
    ) {
    }


    fun dropIndex(
        clientSession: ClientSession?,
        keys: Bson?
    ) {
    }


    fun dropIndex(
        clientSession: ClientSession?,
        indexName: String?,
        dropIndexOptions: DropIndexOptions?
    ) {
    }


    fun dropIndex(
        clientSession: ClientSession?,
        keys: Bson?,
        dropIndexOptions: DropIndexOptions?
    ) {
    }


    fun dropIndexes() {
        mongoCollection.dropIndexes()
    }


    fun dropIndexes(clientSession: ClientSession?) {
    }


    fun dropIndexes(dropIndexOptions: DropIndexOptions?) {
    }


    fun dropIndexes(
        clientSession: ClientSession?,
        dropIndexOptions: DropIndexOptions?
    ) {
    }


    fun renameCollection(newCollectionNamespace: MongoNamespace) {
        mongoCollection.renameCollection(newCollectionNamespace)
    }

    fun renameCollection(newName: String) {
        mongoCollection.renameCollection(MongoNamespace(namespace.databaseName, newName))
    }


    fun renameCollection(
        newCollectionNamespace: MongoNamespace?,
        renameCollectionOptions: RenameCollectionOptions?
    ) {
    }


    fun renameCollection(
        clientSession: ClientSession?,
        newCollectionNamespace: MongoNamespace?
    ) {
    }


    fun renameCollection(
        clientSession: ClientSession?,
        newCollectionNamespace: MongoNamespace?,
        renameCollectionOptions: RenameCollectionOptions?
    ) {
    }


    companion object {
        private const val SCAN_FIRST_LAST: Long = 100


        private const val PARTIAL_FILTER_EXPRESSION_KEY = "partialFilterExpression"
        private const val NAME_KEY = "name"
        private const val SPARSE_KEY = "sparse"
        private const val UNIQUE_KEY = "unique"
        private const val EXPIRE_AFTER_SECONDS_KEY = "expireAfterSeconds"
    }
}
