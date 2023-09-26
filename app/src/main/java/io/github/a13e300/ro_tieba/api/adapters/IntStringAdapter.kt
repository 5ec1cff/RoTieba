package io.github.a13e300.ro_tieba.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type

class IntStringAdapter : JsonDeserializer<Any?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): Any? {
        if (json is JsonPrimitive) {
            if (json.isNumber) {
                if (typeOfT == Integer.TYPE || typeOfT == Integer::class.java)
                    return json.asInt
                else if (typeOfT == java.lang.Long.TYPE || typeOfT == java.lang.Long::class.java)
                    return json.asLong
            } else if (json.isString) {
                if (typeOfT == Integer.TYPE || typeOfT == Integer::class.java)
                    return json.asString.toInt()
                else if (typeOfT == java.lang.Long.TYPE || typeOfT == java.lang.Long::class.java)
                    return json.asString.toLong()
            }
            return context.deserialize(json, typeOfT)
        }
        return null
    }
}