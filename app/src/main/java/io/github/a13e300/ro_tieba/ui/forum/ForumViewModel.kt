package io.github.a13e300.ro_tieba.ui.forum

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.api.TiebaClient
import io.github.a13e300.ro_tieba.api.protobuf.FrsPageSortType
import io.github.a13e300.ro_tieba.models.Forum
import io.github.a13e300.ro_tieba.models.ForumSortType
import io.github.a13e300.ro_tieba.models.ForumTab
import io.github.a13e300.ro_tieba.models.ThreadType
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.models.User
import io.github.a13e300.ro_tieba.models.toUser
import io.github.a13e300.ro_tieba.toImageContentList
import io.github.a13e300.ro_tieba.toPostContent
import java.util.Date


class ForumViewModel : ViewModel() {
    var forumInitialized = false
    val forumInfo = MutableLiveData<Forum>()
    lateinit var forumName: String
    val tabs = MutableLiveData<List<ForumTab>>(
        listOf(
            ForumTab.LatestTab,
            ForumTab.HotTab,
            ForumTab.GoodTab
        )
    )
    var forumSortType: ForumSortType = ForumSortType.REPLY_TIME
    var tabPosition = 0

    inner class ThreadPagingSource(
        private val client: TiebaClient,
        private val tab: ForumTab,
        private val forumSortType: ForumSortType
    ) : PagingSource<Int, TiebaThread>() {
        private suspend fun loadForPrimaryTab(
            page: Int,
            tab: ForumTab.PrimaryTab
        ): Pair<List<TiebaThread>, Boolean> {
            val response = client.getThreads(
                forumName, page, sort = when (tab) {
                    is ForumTab.LatestTab, is ForumTab.GoodTab -> forumSortType.frsType
                    is ForumTab.HotTab -> FrsPageSortType.HOT
                }, good = tab is ForumTab.GoodTab
            )
            if (!forumInitialized) {
                forumInfo.value = Forum(
                    response.forum.name,
                    response.forum.id,
                    response.forum.avatar,
                    response.forum.slogan
                )
                val newTabs = mutableListOf<ForumTab>(ForumTab.LatestTab)
                if (response.navTabInfo.tabList.find { it.isGeneralTab == 0 && it.tabType == 12 } != null) newTabs.add(
                    ForumTab.HotTab
                )
                newTabs.add(ForumTab.GoodTab)
                response.navTabInfo.tabList.filter { it.isGeneralTab == 1 }.forEach {
                    newTabs.add(
                        ForumTab.GeneralTab(
                            it.tabId, it.tabName, it.tabType
                        )
                    )
                }
                tabs.value = newTabs
                forumInitialized = true
            }
            val users = response.userListList.associateBy({ it.id },
                { it.toUser() })
            return response.threadListList.map { p ->
                TiebaThread(
                    p.id,
                    p.firstPostId,
                    p.title,
                    users[p.authorId] ?: User(),
                    p.firstPostContentList.toPostContent(),
                    Date(p.lastTimeInt.toLong() * 1000),
                    p.replyNum,
                    p.isGood == 1,
                    createTime = Date(p.createTime.toLong() * 1000),
                    viewNum = p.viewNum,
                    agreeNum = p.agree.agreeNum,
                    disagreeNum = p.agree.disagreeNum,
                    images = p.mediaList.toImageContentList(),
                    tabInfo = tabs.value!!.find { it is ForumTab.GeneralTab && it.id == p.tabId } as? ForumTab.GeneralTab,
                    threadType = if (p.threadType == 71) ThreadType.HELP else ThreadType.NORMAL
                )
            } to (response.page.hasMore == 1)
        }

        private suspend fun loadForGeneralTab(
            page: Int,
            tab: ForumTab.GeneralTab
        ): Pair<List<TiebaThread>, Boolean> {
            val response = client.getThreadsInTab(
                forumInfo.value!!.id, tab.id, page, tab.name, tab.type, 1, forumSortType.tabType
            )
            return response.generalListList.map { p ->
                TiebaThread(
                    p.id,
                    p.firstPostId,
                    p.title,
                    p.author.toUser(),
                    p.firstPostContentList.toPostContent(),
                    Date(p.lastTimeInt.toLong() * 1000),
                    p.replyNum,
                    p.isGood == 1,
                    createTime = Date(p.createTime.toLong() * 1000),
                    viewNum = p.viewNum,
                    agreeNum = p.agree.agreeNum,
                    disagreeNum = p.agree.disagreeNum,
                    images = p.mediaList.toImageContentList(),
                    tabInfo = tabs.value!!.find { it is ForumTab.GeneralTab && it.id == p.tabId } as? ForumTab.GeneralTab,
                    threadType = if (p.threadType == 71) ThreadType.HELP else ThreadType.NORMAL
                )
            } to (response.hasMore == 1)
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TiebaThread> {
            val page = params.key ?: 1
            try {
                val (posts, more) = when (tab) {
                    is ForumTab.PrimaryTab -> loadForPrimaryTab(page, tab)
                    is ForumTab.GeneralTab -> loadForGeneralTab(page, tab)
                }
                return LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = if (more) page + 1 else null
                )
            } catch (t: Throwable) {
                Logger.e("failed to load forum", t)
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, TiebaThread>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }
    }

    val flow = Pager(
        PagingConfig(pageSize = 30)
    ) {
        ThreadPagingSource(App.instance.client, tabs.value!![tabPosition], forumSortType)
    }.flow
        .cachedIn(viewModelScope)
}
