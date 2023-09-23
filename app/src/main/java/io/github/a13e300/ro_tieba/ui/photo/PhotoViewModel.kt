package io.github.a13e300.ro_tieba.ui.photo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.a13e300.ro_tieba.models.Content

data class Photo(
    val url: String,
    val order: Int,
    val source: Any? = null
)

fun Content.ImageContent.toPhoto(source: Any? = null) = Photo(src, order, source)

class PhotoViewModel : ViewModel() {
    lateinit var photos: List<Photo>
    var currentIndex = MutableLiveData<Int>()
}