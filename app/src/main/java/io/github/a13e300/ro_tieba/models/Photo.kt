package io.github.a13e300.ro_tieba.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Photo(
    val url: String,
    val order: Int,
    val source: String,
    val content: List<Content>? = null
) : Parcelable
