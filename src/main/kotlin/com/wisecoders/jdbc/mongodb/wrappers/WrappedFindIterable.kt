package com.wisecoders.jdbc.mongodb.wrappers

import com.mongodb.BasicDBObject
import com.mongodb.CursorType
import com.mongodb.Function
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoIterable
import com.wisecoders.jdbc.mongodb.GraalConvertor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Copyright Wise Coders GmbH. The MongoDB JDBC driver is build to be used with  [DbSchema Database Designer](https://dbschema.com)
 * Free to use by everyone, code modifications allowed only to the  [public repository](https://github.com/wise-coders/mongodb-jdbc-driver)
 */
class WrappedFindIterable<TResult>(
    private val findIterable: FindIterable<TResult?>,
    private val countDocuments: ((Bson?) -> Long)? = null,
    initialFilter: Bson? = null,
) : MongoIterable<TResult?> {

    private var filter: Bson? = initialFilter

    private fun toDocument(map: Map<String, *>): TResult {
        return (Document(map)) as TResult
    }

    fun filter(str: String): WrappedFindIterable<*> {
        val bson: Bson = BasicDBObject.parse(str)
        filter = bson
        findIterable.filter(bson)
        return this
    }

    fun filter(map: Map<*, *>?): WrappedFindIterable<*> {
        val bson: Bson = GraalConvertor.toBson(map)
        filter = bson
        findIterable.filter(bson)
        return this
    }

    fun projection(str: String): WrappedFindIterable<*> {
        findIterable.projection(BasicDBObject.parse(str))
        return this
    }

    fun projection(map: Map<*, *>?): WrappedFindIterable<*> {
        findIterable.projection(GraalConvertor.toBson(map))
        return this
    }

    fun sort(str: String): WrappedFindIterable<*> {
        findIterable.sort(BasicDBObject.parse(str))
        return this
    }

    fun sort(map: Map<*, *>?): WrappedFindIterable<*> {
        findIterable.sort(GraalConvertor.toBson(map))
        return this
    }

    fun pretty(): WrappedFindIterable<*> {
        return this
    }

    /**
     * Like the legacy mongo shell cursor.count(): computed server side from the filter,
     * ignoring skip() and limit(). Falls back to iterating the cursor when no count
     * callback was provided.
     */
    fun count(): Long {
        if (countDocuments != null) {
            return countDocuments(filter)
        }
        var cnt: Long = 0
        iterator().use { cursor: MongoCursor<TResult?> ->
            while (cursor.hasNext()) {
                cursor.next()
                cnt++
            }
        }
        return cnt
    }

    //---------------------------------------------------------------
    fun filter(bson: Bson?): WrappedFindIterable<*> {
        filter = bson
        findIterable.filter(bson)
        return this
    }

    fun limit(i: Int): WrappedFindIterable<*> {
        findIterable.limit(i)
        return this
    }

    fun skip(i: Int): WrappedFindIterable<*> {
        findIterable.skip(i)
        return this
    }

    fun maxTime(
        l: Long,
        timeUnit: TimeUnit
    ): WrappedFindIterable<*> {
        findIterable.maxTime(l, timeUnit)
        return this
    }

    fun projection(bson: Bson?): WrappedFindIterable<*> {
        findIterable.projection(bson)
        return this
    }

    fun sort(bson: Bson?): WrappedFindIterable<*> {
        findIterable.sort(bson)
        return this
    }

    fun noCursorTimeout(b: Boolean): WrappedFindIterable<*> {
        findIterable.noCursorTimeout(b)
        return this
    }

    fun partial(b: Boolean): WrappedFindIterable<*> {
        findIterable.partial(b)
        return this
    }

    fun cursorType(cursorType: CursorType): WrappedFindIterable<*> {
        findIterable.cursorType(cursorType)
        return this
    }

    override fun batchSize(i: Int): MongoIterable<TResult?> {
        findIterable.batchSize(i)
        return this
    }

    override fun iterator(): MongoCursor<TResult?> {
        return findIterable.iterator()
    }

    override fun first(): TResult? {
        return findIterable.first()
    }

    override fun <U> map(tResultUFunction: Function<TResult?, U>): MongoIterable<U> {
        return findIterable.map(tResultUFunction)
    }

    override fun <A : MutableCollection<in TResult?>?> into(p0: A & Any): A & Any {
        return findIterable.into(p0)
    }

    override fun forEach(action: Consumer<in TResult?>?) {
        findIterable.forEach(action)
    }

    override fun cursor(): MongoCursor<TResult?> {
        return findIterable.cursor()
    }

    fun explain(): Document {
        return findIterable.explain()
    }
}
