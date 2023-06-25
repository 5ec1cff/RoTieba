package io.github.a13e300.ro_tieba.models

import java.util.Date

interface IPost {
    val user: User
    val content: List<Content>
    val floor: Int
    val id: Long
    val time: Date
}