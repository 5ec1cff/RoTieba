package io.github.a13e300.ro_tieba.models

sealed class PostId {
    abstract val tid: Long
    open val pid: Long = 0L
    open val spid: Long = 0L

    data class Thread(override val tid: Long) : PostId()
    data class Post(override val tid: Long, override val pid: Long) : PostId()
    data class Comment(override val tid: Long, override val pid: Long, override val spid: Long) :
        PostId()
}
