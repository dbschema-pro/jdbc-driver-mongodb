package com.wisecoders.jdbc.mongodb.structure


/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
class MetaIndex internal constructor(
    private val metaMap: MetaObject,
    val name: String,
    val pk: Boolean,
    val unique: Boolean
) {
    val metaFields: MutableList<MetaField> = ArrayList()

    fun addColumn(metaField: MetaField?) {
        if (metaField != null) {
            metaFields.add(metaField)
        }
    }
}
