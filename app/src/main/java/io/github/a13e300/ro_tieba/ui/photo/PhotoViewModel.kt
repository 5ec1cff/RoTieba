package io.github.a13e300.ro_tieba.ui.photo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Photo
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.UserProfile


fun Content.ImageContent.toPhoto(source: Any? = null) = Photo(
    src, order,
    imageSource(source, order), content = when (source) {
        is Post -> source.content
        is TiebaThread -> source.content
        else -> null
    }
)

fun imageSource(source: Any?, order: Int = 0) =
    when (source) {
        is Post -> "rotieba_t${source.tid}_p${source.postId}_f${source.floor}_c$order"
        is TiebaThread -> "rotieba_t${source.tid}_p${source.postId}_f1_c$order"
        is UserProfile -> "rotieba_u${source.uid}_${source.portrait}"
        else -> "rotieba"
    }

class PhotoViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var photos: List<Photo> = savedStateHandle.get<List<Photo>>("photos") ?: emptyList()
        set(v) {
            field = v
            savedStateHandle["photos"] = v
        }
    var currentIndex = savedStateHandle.getLiveData<Int>("current_index")
}
