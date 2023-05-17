package io.github.a13e300.ro_tieba.api

import java.io.IOException

class TiebaApiError(val errno: Int, val msg: String?) : IOException() {
    override val message: String
        get() = "$errno: $msg"
}