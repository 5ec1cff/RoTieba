package io.github.a13e300.ro_tieba.models

sealed class ForumTab(open val name: String) {
    sealed class PrimaryTab(override val name: String) : ForumTab(name)
    data object HotTab : PrimaryTab("热门")
    data object LatestTab : PrimaryTab("最新")
    data object GoodTab : PrimaryTab("精品")

    data class GeneralTab(
        val id: Int,
        override val name: String,
        val type: Int
    ) : ForumTab(name)
}
