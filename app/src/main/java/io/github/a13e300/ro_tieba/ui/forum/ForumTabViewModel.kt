package io.github.a13e300.ro_tieba.ui.forum

import androidx.lifecycle.ViewModel
import io.github.a13e300.ro_tieba.models.ForumSortType
import io.github.a13e300.ro_tieba.models.ForumTab

class ForumTabViewModel : ViewModel() {
    var initialized = false
    lateinit var tab: ForumTab
    var forumSortType: ForumSortType = ForumSortType.REPLY_TIME


}