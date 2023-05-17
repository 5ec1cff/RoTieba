package io.github.a13e300.ro_tieba

import android.util.Log

object Logger {
    private const val TAG = "RoTieba"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }

    fun e(msg: String, t: Throwable) {
        Log.e(TAG, msg, t)
    }
}