package io.github.a13e300.ro_tieba.ui.photo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class Photo(
    val url: String,
    val order: Int,
    val source: Any? = null
)

class PhotoViewModel : ViewModel() {
    lateinit var photos: List<Photo>
    var currentIndex = MutableLiveData<Int>()
}