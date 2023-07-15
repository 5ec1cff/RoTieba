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
            if (json.isNumber)
                return json.asInt
            else if (json.isString)
                return json.asString.toInt()
            return context.deserialize(json, typeOfT)
        }
        return null
    }
}