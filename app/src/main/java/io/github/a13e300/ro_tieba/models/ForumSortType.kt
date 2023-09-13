package io.github.a13e300.ro_tieba.models

import io.github.a13e300.ro_tieba.api.protobuf.FrsPageSortType
import io.github.a13e300.ro_tieba.api.protobuf.GeneralTabListSortType

enum class ForumSortType(val frsType: FrsPageSortType, val tabType: GeneralTabListSortType) {
    REPLY_TIME(FrsPageSortType.REPLY_TIME, GeneralTabListSortType.REPLY_TIME),
    CREATE_TIME(FrsPageSortType.CREATE_TIME, GeneralTabListSortType.CREATE_TIME)
}
