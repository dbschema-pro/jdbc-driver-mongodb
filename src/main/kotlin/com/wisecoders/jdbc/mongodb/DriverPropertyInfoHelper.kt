package com.wisecoders.jdbc.mongodb

import java.sql.DriverPropertyInfo

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
internal class DriverPropertyInfoHelper {
    val propertyInfo: Array<DriverPropertyInfo>
        get() {
            val propInfos = ArrayList<DriverPropertyInfo>()


            addPropInfo(
                propInfos,
                CONNECTIONS_PER_HOST,
                "10",
                ("The maximum number of connections allowed per "
                        + "host for this Mongo instance. Those connections will be kept in a pool when idle. Once the "
                        + "pool is exhausted, any operation requiring a connection will block waiting for an available "
                        + "connection."),
                null
            )

            addPropInfo(
                propInfos,
                CONNECT_TIMEOUT,
                "10000",
                ("The connection timeout in milliseconds. A value "
                        + "of 0 means no timeout. It is used solely when establishing a new connection "
                        + "Socket.connect(java.net.SocketAddress, int)"),
                null
            )

            addPropInfo(
                propInfos,
                CURSOR_FINALIZER_ENABLED,
                "true",
                ("Sets whether there is a a finalize "
                        + "method created that cleans up instances of DBCursor that the client does not close. If you "
                        + "are careful to always call the close method of DBCursor, then this can safely be set to false."),
                null
            )

            addPropInfo(
                propInfos,
                READ_PREFERENCE,
                "primary",
                "represents preferred replica set members to which a query or command can be sent",
                arrayOf(
                    "primary",
                    "primary preferred",
                    "secondary",
                    "secondary preferred",
                    "nearest"
                )
            )

            addPropInfo(
                propInfos,
                SOCKET_TIMEOUT,
                "0",
                ("The socket timeout in milliseconds It is used for "
                        + "I/O socket read and write operations "
                        + "Socket.setSoTimeout(int) Default is 0 and means no timeout."),
                null
            )

            return propInfos.toTypedArray<DriverPropertyInfo>()
        }

    private fun addPropInfo(
        propInfos: ArrayList<DriverPropertyInfo>,
        propName: String,
        defaultVal: String,
        description: String,
        choices: Array<String>?
    ) {
        val newProp = DriverPropertyInfo(propName, defaultVal)
        newProp.description = description
        if (choices != null) {
            newProp.choices = choices
        }
        propInfos.add(newProp)
    }

    companion object {
        private const val CONNECTIONS_PER_HOST = "connectionsPerHost"
        private const val CONNECT_TIMEOUT = "connectTimeout"
        private const val CURSOR_FINALIZER_ENABLED = "cursorFinalizerEnabled"
        private const val READ_PREFERENCE = "readPreference"
        private const val SOCKET_TIMEOUT = "socketTimeout"
    }
}
