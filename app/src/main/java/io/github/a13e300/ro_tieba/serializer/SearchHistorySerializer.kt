package io.github.a13e300.ro_tieba.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.a13e300.ro_tieba.datastore.SearchHistory
import java.io.InputStream
import java.io.OutputStream

object SearchHistorySerializer :
    Serializer<SearchHistory> {
    override val defaultValue: SearchHistory = SearchHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SearchHistory {
        try {
            return SearchHistory.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: SearchHistory,
        output: OutputStream
    ) = t.writeTo(output)
}
