package com.wisecoders.jdbc.mongodb.structure

import com.google.gson.GsonBuilder
import com.mongodb.DBRef
import com.wisecoders.jdbc.mongodb.Util
import java.sql.Types
import org.bson.Document
import org.bson.types.ObjectId

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
open class MetaObject internal constructor(
    parentObject: MetaObject?,
    name: String?
) : MetaField(parentObject, name) {

    val fields: MutableList<MetaField> = ArrayList()

    private fun getField(name: String): MetaField? {
        for (field in fields) {
            if (field.name == name) return field
        }
        return null
    }

    private fun createField(
        name: String?,
        sortFields: Boolean
    ): MetaField {
        val field = MetaField(this, name)
        fields.add(field)
        if (sortFields) {
            fields.sortWith(FIELDS_COMPARATOR)
        }
        return field
    }

    private fun createField(
        name: String,
        typeName: String?,
        javaType: Int,
        mandatory: Boolean,
        sortFields: Boolean
    ): MetaField {
        for (field in fields) {
            if (field.name == name) return field
        }
        val field = MetaField(this, name)
        field.typeName = typeName
        field.javaType = javaType
        field.isMandatory = mandatory
        fields.add(field)
        if (sortFields) {
            fields.sortWith(FIELDS_COMPARATOR)
        }
        return field
    }

    private fun createObjectField(
        name: String,
        mandatory: Boolean,
        sortFields: Boolean
    ): MetaObject {
        for (field in fields) {
            if (field is MetaObject && field.name == name) return field
        }
        val json = MetaObject(this, name)
        json.typeName = "object"
        json.javaType = TYPE_OBJECT
        fields.add(json)
        if (sortFields) {
            fields.sortWith(FIELDS_COMPARATOR)
        }
        json.isMandatory = mandatory
        return json
    }

    private fun createArrayField(
        name: String,
        typeName: String?,
        mandatoryIfNew: Boolean,
        sortFields: Boolean
    ): MetaObject {
        for (field in fields) {
            if (field is MetaObject && field.name == name) return field
        }
        val json = MetaObject(this, name)
        json.typeName = typeName
        json.javaType = TYPE_ARRAY
        json.isMandatory = mandatoryIfNew
        fields.add(json)
        if (sortFields) {
            fields.sortWith(FIELDS_COMPARATOR)
        }
        return json
    }

    override fun collectFieldsWithObjectId(unsolvedFields: MutableList<MetaField>) {
        super.collectFieldsWithObjectId(unsolvedFields)
        for (field in fields) {
            field.collectFieldsWithObjectId(unsolvedFields)
        }
    }

    fun findField(name: String?): MetaField? {
        for (other in fields) {
            if (name != null && name.startsWith(other.nameWithPath)) {
                var found: MetaField? = null
                if (other is MetaObject) {
                    found = other.findField(name)
                }
                return found ?: other
            }
        }
        return null
    }


    fun visitValidatorNode(
        name: String?,
        mandatory: Boolean,
        bsonDefinition: Document,
        sortFields: Boolean
    ) {
        var enumValues: List<Any?>? = null
        try {
            enumValues = bsonDefinition.getList("enum", Any::class.java)
        } catch (ignore: Throwable) {
        }
        if (enumValues == null) {
            when (val bsonType = Util.getBsonType(bsonDefinition)) {
                "object" -> {
                    val intoObject = if (name != null) createObjectField(name, mandatory, sortFields) else this
                    intoObject.visitValidatorFields(
                        bsonDefinition["properties"] as Document?, bsonDefinition.getList(
                            "required",
                            String::class.java
                        ), sortFields
                    )
                    intoObject.description = bsonDefinition.getString("description")
                }

                "array" -> {
                    val itemsDefinition = bsonDefinition["items"] as Document?
                    if (itemsDefinition != null) {
                        val itemType = Util.getBsonType(itemsDefinition)

                        val intoObject = if (name != null) createArrayField(
                            name,
                            "array[$itemType]", mandatory, sortFields
                        ) else this
                        intoObject.description = bsonDefinition.getString("description")
                        val objDefinition = itemsDefinition["properties"] as Document?
                        if (objDefinition != null) {
                            intoObject.visitValidatorFields(
                                objDefinition, bsonDefinition.getList(
                                    "required",
                                    String::class.java
                                ), sortFields
                            )
                        }
                    } else if (bsonDefinition["properties"] != null) {
                        val intoObject =
                            if (name != null) createArrayField(name, "array[object]", mandatory, sortFields) else this
                        intoObject.visitValidatorFields(
                            bsonDefinition["properties"] as Document?, bsonDefinition.getList(
                                "required",
                                String::class.java
                            ), sortFields
                        )
                        intoObject.description = bsonDefinition.getString("description")
                    }
                }

                else -> {
                    val metaField = createField(name, sortFields)
                    metaField.typeName = bsonType
                    metaField.isMandatory = mandatory
                    metaField.description = bsonDefinition.getString("description")
                    if (bsonDefinition.containsKey("pattern")) metaField.addOption("pattern:'" + bsonDefinition["pattern"] + "'")
                    if (bsonDefinition.containsKey("minimum")) metaField.addOption("minimum:" + bsonDefinition["minimum"])
                    if (bsonDefinition.containsKey("maximum")) metaField.addOption("maximum:" + bsonDefinition["maximum"])
                }
            }
        } else {
            val field = createField(name, sortFields)
            field.typeName = "enum"
            field.javaType = Types.ARRAY
            field.isMandatory = mandatory
            var anEnum = GsonBuilder().create().toJson(
                bsonDefinition.getList(
                    "enum",
                    Any::class.java
                )
            )
            if (anEnum.startsWith("[") && anEnum.endsWith("]")) {
                anEnum = anEnum.substring(1, anEnum.length - 1)
            }
            field.addOption("enum:$anEnum")
            field.description = bsonDefinition.getString("description")
        }
    }


    private fun visitValidatorFields(
        document: Document?,
        requiredFields: List<String>?,
        sortFields: Boolean
    ) {
        if (document != null) {
            for ((key, value) in document) {
                if (value != null) {
                    val mandatory = requiredFields != null && requiredFields.contains(key)
                    visitValidatorNode(key, mandatory, value as Document, sortFields)
                }
            }
        }
    }


    private var isFirstDiscover = true

    protected fun scanDocument(
        objDocument: Any?,
        sortFields: Boolean,
        level: Int
    ) {
        if (level < DISCOVER_CHILD_CASCADE_DEEPNESS && objDocument is Map<*, *>) {
            for (key in objDocument.keys) {
                val value = objDocument[key]
                if (value is Map<*, *>) {
                    // "suburbs":[ { name: "Scarsdale" }, { name: "North Hills" } ] WOULD GENERATE SUB-ENTITIES 0,1,2,... FOR EACH LIST ENTRY. SKIP THIS
                    if (Util.allKeysAreNumbers(value)) {
                        val childrenMap = createArrayField(key.toString(), "array[int]", isFirstDiscover, sortFields)
                        for (subKey in value.keys) {
                            childrenMap.scanDocument(value[subKey], sortFields, level + 1)
                        }
                    } else {
                        val childrenMap = createObjectField(key.toString(), isFirstDiscover, sortFields)
                        childrenMap.scanDocument(value, sortFields, level + 1)
                    }
                } else if (value is List<*>) {
                    val cls = Util.getListElementsClass(value)
                    if (cls == MutableMap::class.java) {
                        val subDocument = createArrayField(key.toString(), "array[object]", isFirstDiscover, sortFields)
                        for (child in value) {
                            subDocument.scanDocument(child, sortFields, level + 1)
                        }
                    } else if (cls == null || cls == Any::class.java) {
                        createField(key as String, "array", 2003, isFirstDiscover, sortFields)
                    } else {
                        val field = createField(
                            key as String,
                            "array[" + cls.simpleName.lowercase() + "]",
                            2003,
                            isFirstDiscover,
                            sortFields
                        )
                        if (value.isNotEmpty() && value[0] is ObjectId) {
                            field.setObjectId(value[0] as ObjectId?)
                        }
                    }
                } else {
                    var field = getField(key as String)
                    if (field == null) {
                        field = createField(key, sortFields)
                        field.isMandatory = isFirstDiscover
                    }
                    field.setTypeFromValue(value)
                    // VALUES WHICH ARE OBJECTID AND ARE NOT _id IN THE ROOT MAP
                    if (value is ObjectId && "_id" != field.nameWithPath) {
                        field.setObjectId(value)
                    }
                    if (value is DBRef) {
                        val targetCollection = metaCollection.metaDatabase.getMetaCollection(value.collectionName)
                        if (targetCollection != null) {
                            field.createReferenceTo(targetCollection)
                        }
                    }
                }
            }
            for (field in fields) {
                if (!objDocument.containsKey(field.name)) {
                    field.isMandatory = false
                }
            }
        }
        isFirstDiscover = false
    }

    override val metaCollection: MetaCollection
        get() = generateSequence(this as MetaObject?) { it.parentObject }
            .filterIsInstance<MetaCollection>()
            .first()

    override val fieldCount: Int
        get() {
            var count = 0
            for (_field in fields) {
                count += _field.fieldCount
            }
            return count
        }

    companion object {
        const val TYPE_OBJECT: Int = 4999544
        const val TYPE_ARRAY: Int = 4999545

        val FIELDS_COMPARATOR: Comparator<MetaField> = Comparator { o1: MetaField, o2: MetaField ->
            if (o1 == o2) {
                return@Comparator 0
            } else if ("_id" == o1.name) {
                return@Comparator -1
            } else if ("_id" == o2.name) {
                return@Comparator 1
            }
            o1.name.compareTo(o2.name)
        }

        private const val DISCOVER_CHILD_CASCADE_DEEPNESS = 25
    }
}
