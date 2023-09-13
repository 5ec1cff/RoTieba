package io.github.a13e300.ro_tieba.api.protobuf

/**
 * https://github.com/Starry-OvO/aiotieba/blob/ed8867f6ac73b523389dd1dcbdd4b5f62a16ff81/aiotieba/client.py#L494
 * 排序方式 对于有热门分区的贴吧 0热门排序 1按发布时间 2关注的人 34热门排序 >=5是按回复时间
 * 对于无热门分区的贴吧 0按回复时间 1按发布时间 2关注的人 >=3按回复时间
 */
enum class FrsPageSortType(val value: Int) {
    HOT(0),
    CREATE_TIME(1),
    FOLLOW(2),
    REPLY_TIME(5)
}