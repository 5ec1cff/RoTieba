package io.github.a13e300.ro_tieba.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type

class IntBooleanAdapter : JsonDeserializer<Any?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): Any? {
        if (json is JsonPrimitive) {
            if (json.isBoolean)
                return json.asBoolean
            else if (json.isNumber)
                return json.asInt != 0
            return context.deserialize(json, typeOfT)
        }
        return null
    }
}