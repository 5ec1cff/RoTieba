package io.github.a13e300.ro_tieba.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class NullAdapter : JsonDeserializer<Any?> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): Any? {
        if (json.isJsonObject) {
            return context.deserialize(json, typeOfT)
        }
        return null
    }
}