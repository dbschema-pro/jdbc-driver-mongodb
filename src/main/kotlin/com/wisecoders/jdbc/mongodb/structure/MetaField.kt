package com.wisecoders.jdbc.mongodb.structure

import com.wisecoders.jdbc.mongodb.Util
import org.bson.types.ObjectId


/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/deed.en), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/mongodb-jdbc-driver).
 */
open class MetaField internal constructor(
    @JvmField val parentObject: MetaObject?,
    name: String?
) {
    @JvmField
    val name: String = (name ?: "")
    private var typeClass: Class<*>? = null

    var typeName: String? = null
        get() {
            if (field != null) {
                return field
            }
            if (typeClass != null) {
                var type = typeClass!!.name
                if ("Boolean".equals(type, ignoreCase = true)) {
                    type = "bool"
                }
                if (type.lastIndexOf('.') > 0) type = type.substring(type.lastIndexOf('.') + 1)
                return type
            }
            return "string"
        }

    var javaType: Int = Int.MIN_VALUE
        get() {
            if (field != Int.MIN_VALUE) {
                return field
            }
            return Util.getJavaType(typeName)
        }
    private var objectId: ObjectId? = null
    val references: MutableList<MetaReference> = ArrayList()
    var isMandatory: Boolean = true
    var options: String? = null
    var description: String? = null

    fun setObjectId(objectId: ObjectId?) {
        if (objectId != null) {
            this.objectId = objectId
        }
    }

    fun getObjectId(): ObjectId? {
        return objectId
    }

    val nameWithPath: String
        get() = (if (parentObject != null && parentObject !is MetaCollection) parentObject.nameWithPath + "." + name else name)

    val pkColumnName: String
        get() {
            var pkColumnName = name
            val idx = pkColumnName.lastIndexOf('.')
            if (idx > 0) {
                pkColumnName = pkColumnName.substring(idx + 1)
            }
            return pkColumnName
        }

    fun createReferenceTo(pkCollection: MetaCollection): MetaReference {
        val ifk = MetaReference(this, pkCollection)
        references.add(ifk)
        return ifk
    }

    open val metaCollection: MetaCollection
        get() {
            var _field : MetaField? = this
            while (_field !is MetaCollection) {
                _field = _field?.parentObject
            }
            return _field
        }

    open fun collectFieldsWithObjectId(unsolvedFields: MutableList<MetaField>) {
        if (objectId != null) {
            unsolvedFields.add(this)
        }
    }

    override fun toString(): String {
        return nameWithPath
    }

    fun addOption(options: String) {
        if (this.options == null) {
            this.options = options
        } else {
            this.options += ", $options"
        }
    }

    open val fieldCount: Int
        get() = 1

    fun setTypeFromValue(value: Any?) {
        if (value != null) {
            val valueCls: Class<Any> = value.javaClass
            var _typeClass = typeClass
            if (_typeClass == null) {
                _typeClass = valueCls
            } else if (_typeClass != valueCls) {
                // valueCls is superclass or typeClass
                if (valueCls.isAssignableFrom(_typeClass)) _typeClass = valueCls
                else if (!_typeClass.isAssignableFrom(valueCls)) _typeClass = Any::class.java
            }
            this.typeClass = _typeClass
        }
    }

    fun setTypeClass(typeClass: Class<*>?) {
        this.typeClass = typeClass
    }
}
