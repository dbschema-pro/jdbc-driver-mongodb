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
class WrappedFindIterable<TResult>(private val findIterable: FindIterable<TResult?>) : MongoIterable<TResult?> {

    private fun toDocument(map: Map<String, *>): TResult {
        return (Document(map)) as TResult
    }

    fun filter(str: String): WrappedFindIterable<*> {
        findIterable.filter(BasicDBObject.parse(str))
        return this
    }

    fun filter(map: Map<*, *>?): WrappedFindIterable<*> {
        findIterable.filter(GraalConvertor.toBson(map))
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

    fun count(): Long {
        var cnt: Long = 0
        for (res in findIterable) {
            cnt++
        }
        return cnt
    }

    //---------------------------------------------------------------
    fun filter(bson: Bson?): WrappedFindIterable<*> {
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
