package com.wisecoders.jdbc.mongodb


/**
 * How deep the driver should look into collections in order to deduce the collection structure ( fields, data types ).
 *
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
enum class ScanStrategy(val SCAN_COUNT: Long) {
    fast(100), medium(300), full(Long.MAX_VALUE)
}
