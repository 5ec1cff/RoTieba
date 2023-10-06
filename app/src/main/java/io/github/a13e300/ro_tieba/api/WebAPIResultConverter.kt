package io.github.a13e300.ro_tieba.api

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import io.github.a13e300.ro_tieba.api.web.WebApiResult
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

@Suppress("Unchecked_Cast")
class WebAPIResultConverter(
    private val gson: Gson,
    val type: Type
) : Converter<ResponseBody, Any> {
    private val adapter = gson.getAdapter(
        TypeToken.getParameterized(
            WebApiResult::class.java,
            type
        )
    ) as TypeAdapter<WebApiResult<Any>>

    override fun convert(value: ResponseBody): Any {
        val jsonReader = gson.newJsonReader(value.charStream())
        return value.use { _ ->
            val result = adapter.read(jsonReader)
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw JsonIOException("JSON document was not fully consumed.")
            }
            if (result.errorCode != 0) {
                throw TiebaApiError(result.errorCode, result.errorMsg)
            }
            result.data
        }
    }
}

class WebAPIResultConverterFactory(private val gson: Gson) : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return WebAPIResultConverter(gson, type)
    }
}
