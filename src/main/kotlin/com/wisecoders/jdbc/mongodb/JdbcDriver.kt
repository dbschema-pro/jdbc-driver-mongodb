package com.wisecoders.jdbc.mongodb

import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.mongodb.wrappers.WrappedMongoClient
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.Properties
import org.slf4j.Logger

/**
 * Minimal implementation of the JDBC standards for MongoDb database.
 * The URL is passed as it is to the MongoDb native Java driver.
 * Example :
 * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 *
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class JdbcDriver : Driver {
    private val propertyInfoHelper = DriverPropertyInfoHelper()

    /**
     * Connect to the database using a URL like :
     * jdbc:mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
     * The URL excepting the jdbc: prefix is passed as it is to the MongoDb native Java driver.
     */
    @Throws(SQLException::class)
    override fun connect(
        url: String?,
        info: Properties,
    ): Connection? {
        var url = url
        if (url != null) {
            validateMongoJdbcUrl(url)
            if (acceptsURL(url)) {
                if (url.startsWith("jdbc:")) {
                    url = url.substring("jdbc:".length)
                }
                if (url.startsWith("mongodb:")) {
                    url = url.substring("mongodb:".length)
                }
                LOGGER.atInfo()
                    .setMessage { "Connect URL: $url" }
                    .log()

                var idx: Int
                var scan = ScanStrategy.fast
                var expand = false
                var sortFields = false
                var trustStore: String? = null
                var trustStorePassword: String? = null
                var newUrl: String = url
                var urlWithoutParams: String = url
                if ((url.indexOf("?").also { idx = it }) > 0) {
                    val paramsURL = url.substring(idx + 1)
                    urlWithoutParams = url.substring(0, idx)
                    val sbParams = StringBuilder()
                    for (pair in paramsURL.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                        val pairArr = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val key = if (pairArr.size == 2) pairArr[0].lowercase() else ""
                        val value = if (pairArr.size == 2) pairArr[1] else ""
                        when (key) {
                            "scan" -> {
                                try {
                                    scan = ScanStrategy.valueOf(value)
                                } catch (ignore: IllegalArgumentException) {
                                }
                                LOGGER.atInfo().setMessage("ScanStrategy=$scan").log()
                            }

                            "expand" -> expand = value.toBoolean()
                            "sort" -> sortFields = value.toBoolean()
                            "truststore" -> trustStore = value
                            "truststorepassword" -> trustStorePassword = value
                            else -> {
                                if (!sbParams.isEmpty()) sbParams.append("&")
                                sbParams.append(pair)
                            }
                        }
                    }
                    newUrl = url.substring(0, idx) + "?" + sbParams
                }
                if (trustStore != null) {
                    System.setProperty("javax.net.ssl.trustStore", trustStore)
                }
                if (trustStorePassword != null) {
                    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword)
                }
                var databaseName = "admin"
                if (urlWithoutParams.endsWith("/")) {
                    urlWithoutParams = urlWithoutParams.substring(0, urlWithoutParams.length - 1)
                }
                if ((urlWithoutParams.lastIndexOf("/").also { idx = it }) > 1 && urlWithoutParams[idx - 1] != '/') {
                    databaseName = urlWithoutParams.substring(idx + 1)
                }

                LOGGER.atInfo().setMessage("MongoClient URL: $url rewritten as $newUrl").log()
                val client = WrappedMongoClient(newUrl, info, databaseName, scan, expand, sortFields)
                return MongoConnection(client)
            }
        }
        return null
    }


    /**
     * URLs accepted are of the form: jdbc:mongodb[+srv]://&lt;server&gt;[:27017]/&lt;db-name&gt;
     *
     * @see java.sql.Driver.acceptsURL
     */
    @Throws(SQLException::class)
    override fun acceptsURL(url: String): Boolean {
        return url.startsWith("mongodb") || url.startsWith("jdbc:mongodb") || url.startsWith("jdbc.mongodb:mongodb")
    }

    /**
     * @see java.sql.Driver.getPropertyInfo
     */
    @Throws(SQLException::class)
    override fun getPropertyInfo(
        url: String,
        info: Properties,
    ): Array<DriverPropertyInfo> {
        return propertyInfoHelper.propertyInfo
    }

    /**
     * @see java.sql.Driver.getMajorVersion
     */
    override fun getMajorVersion(): Int {
        return 1
    }

    /**
     * @see java.sql.Driver.getMinorVersion
     */
    override fun getMinorVersion(): Int {
        return 0
    }

    /**
     * @see java.sql.Driver.jdbcCompliant
     */
    override fun jdbcCompliant(): Boolean {
        return true
    }

    @Throws(SQLFeatureNotSupportedException::class)
    override fun getParentLogger(): java.util.logging.Logger? {
        return null
    }

    companion object {
        private val LOGGER: Logger = slf4jLogger()

        init {
            DriverManager.registerDriver(JdbcDriver())
        }
    }

    fun validateMongoJdbcUrl(url: String) {
        // Normalize whitespace
        var trimmed = url.trim()

        if (trimmed.startsWith("jdbc:")) {
            trimmed = trimmed.substring("jdbc:".length)
        }

        if (!trimmed.startsWith("mongodb://")
            && !trimmed.startsWith("mongodb+srv://")
        ) {
            throw IllegalArgumentException(
                "Invalid URL prefix. It must start with 'mongodb://' or 'mongodb+srv://'.\n" +
                        "Example (local): mongodb://user:pass@localhost:27017/mydb\n" +
                        "Example (Atlas): mongodb+srv://user:pass@cluster0.mongodb.net/mydb"
            )
        }

        // Reject invalid combined prefixes
        if (trimmed.contains("mongodb://mongodb+srv://")) {
            throw IllegalArgumentException(
                "Invalid MongoDB URL: mixed 'mongodb://' and 'mongodb+srv://' prefixes.\n" +
                        "Use only one. Example for Atlas:\n" +
                        "  mongodb+srv://user:pass@cluster0.mongodb.net/mydb"
            )
        }

        // SRV URLs must NOT contain a port
        if (trimmed.startsWith("mongodb+srv://") && Regex(":[0-9]+").containsMatchIn(trimmed)) {
            throw IllegalArgumentException(
                "SRV URLs (mongodb+srv://) cannot include a port number.\n" +
                        "Example: mongodb+srv://user:pass@cluster0.mongodb.net/mydb"
            )
        }

        // Ensure database name or single slash
        if (trimmed.endsWith("//")) {
            throw IllegalArgumentException(
                "URL ends with double slashes. Please specify a database name or remove the extra '/'.\n" +
                        "Example: jdbc:mongodb+srv://user:pass@cluster0.mongodb.net/mydb"
            )
        }

    }
}
