package io.github.a13e300.ro_tieba.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.models.UserProfile
import io.github.a13e300.ro_tieba.models.toUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {
    val user = MutableLiveData<UserProfile>()
    suspend fun requestUser(uid: Long, portrait: String?) {
        val p = withContext(Dispatchers.IO) {
            App.instance.client.getUserProfile(portrait, uid)
        }
        user.value = p.user.toUserProfile()
    }
}