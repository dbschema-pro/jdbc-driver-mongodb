package com.wisecoders.jdbc.mongodb

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.sql.Types
import java.util.Date
import java.util.regex.Pattern
import org.bson.Document
import org.bson.types.ObjectId

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
object Util {
    @JvmStatic
    fun getByPath(
        document: Document?,
        path: String
    ): Any? {
        val pathEls = path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var cursor: Any? = document
        var idx = 0
        while (idx < pathEls.size && cursor is Map<*, *>) {
            cursor = cursor[pathEls[idx]]
            idx++
        }
        return if (idx == pathEls.size) cursor else null
    }

    fun getJavaType(bsonType: String): Int {
        return when (bsonType) {
            "int" -> Types.INTEGER
            "double" -> Types.DOUBLE
            "array" -> Types.ARRAY
            "objectId" -> Types.ROWID
            "bool" -> Types.BOOLEAN
            "date" -> Types.DATE
            "null" -> Types.NULL
            "dbPointer" -> Types.DATALINK
            "javascript" -> Types.JAVA_OBJECT
            "timestamp" -> Types.TIMESTAMP
            "long" -> Types.BIGINT
            "decimal" -> Types.DECIMAL
            "minKey", "maxKey" -> Types.SMALLINT
            "regex", "javascriptWithScope", "string", "symbol" -> Types.VARCHAR
            else -> Types.VARCHAR
        }
    }


    private val PATTERN_NUMBER: Pattern = Pattern.compile("\\d+")

    fun allKeysAreNumbers(map: Map<*, *>): Boolean {
        if (map.isEmpty()) return false
        for (key in map.keys) {
            val isNumber = key is Number || (key is String && PATTERN_NUMBER.matcher(
                key
            ).matches())
            if (!isNumber) return false
        }
        return true
    }

    fun getJavaType(value: Any?): Int {
        if (value is Int) return Types.INTEGER
        else if (value is Timestamp) return Types.TIMESTAMP
        else if (value is Date) return Types.DATE
        else if (value is Double) return Types.DOUBLE
        return Types.VARCHAR
    }

    fun getListElementsClass(obj: Any?): Class<*>? {
        if (obj is List<*>) {
            var cls: Class<*>? = null
            for (`val` in obj) {
                var _cls: Class<*>? = null
                if (`val` is Map<*, *>) _cls = MutableMap::class.java
                else if (`val` is Int) _cls = Int::class.java
                else if (`val` is Double) _cls = Double::class.java
                else if (`val` is Long) _cls = Long::class.java
                else if (`val` is Boolean) _cls = Boolean::class.java
                else if (`val` is Date) _cls = Date::class.java
                else if (`val` is String) _cls = String::class.java
                else if (`val` is ObjectId) _cls = ObjectId::class.java
                if (cls == null) cls = _cls
                else if (cls != _cls) cls = Any::class.java
            }
            return cls
        }
        return null
    }

    fun getBsonType(bsonDefinition: Document): String {
        val bsonTypeObj = bsonDefinition["bsonType"]
        return if (bsonTypeObj is List<*>) {
            bsonTypeObj[0].toString()
        } else {
            bsonTypeObj.toString()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readStringFromInputStream(inputStream: InputStream): String {
        if (inputStream == null) {
            throw IOException("Got empty Input Stream")
        }
        val `in` = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val sb = StringBuilder()
        var str: String?
        while (null != ((`in`.readLine().also { str = it }))) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(str)
        }
        `in`.close()
        return sb.toString()
    }
}
