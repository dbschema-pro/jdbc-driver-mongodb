package com.wisecoders.jdbc.mongodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.BasicDBObject
import java.util.regex.Pattern
import org.bson.conversions.Bson
import org.bson.types.ObjectId

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en),
 * copyright [Wise Coders GmbH](https://wisecoders.com),
 * used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the
 * [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
object GraalConvertor {

    private val mapper: ObjectMapper = ObjectMapper()
    private val HEXADECIMAL_PATTERN: Pattern = Pattern.compile("\\p{XDigit}+")

    /**
     * Converts an object into a BSON representation.
     */
    fun toBson(obj: Any?): Bson = when (obj) {
        null -> BasicDBObject()
        is Map<*, *> -> BasicDBObject(convertMap(obj))
        is List<*> -> BasicDBObject("array", toList(obj) as Any) // pack list
        else -> BasicDBObject.parse(mapper.writeValueAsString(obj))
    }

    /**
     * Converts a source into a list of BSON objects if possible.
     */
    fun toList(source: Any?): MutableList<Bson?> {
        return when (source) {
            is Map<*, *> -> if (mapIsArray(source)) {
                val array = mutableListOf<Bson?>()
                for (i in 0 until source.size) {
                    array.add(toBson(source["$i"]))
                }
                array
            } else ArrayList()

            is List<*> -> {
                val ret = mutableListOf<Bson?>()
                for (obj in source) {
                    ret.add(toBson(obj))
                }
                ret
            }

            else -> ArrayList()
        }
    }

    /**
     * Recursively converts a map, handling arrays and ObjectIds.
     */
    fun convertMap(map: Map<*, *>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in map) {
            when {
                value is Map<*, *> -> {
                    result[key!! as String] = if (mapIsArray(value)) {
                        mapToArray(value)
                    } else {
                        convertMap(value)
                    }
                }
                key == "_id" && value is String && HEXADECIMAL_PATTERN.matcher(value).matches() -> {
                    result["_id"] = ObjectId(value)
                }
                value != null -> result[key!! as String] = value
            }
        }
        return result
    }

    /**
     * Checks if a map represents an array-like structure (indexed by stringified integers).
     */
    private fun mapIsArray(map: Map<*, *>): Boolean =
        map.isNotEmpty() && map.containsKey("0")

    /**
     * Converts a map with numeric string keys into a list.
     */
    private fun mapToArray(map: Map<*, *>): List<Any?> {
        val array = mutableListOf<Any?>()
        var i = 0
        while (true) {
            val obj = map["$i"] ?: break
            array += if (obj is Map<*, *>) convertMap(obj) else obj
            i++
        }
        return array
    }
}
