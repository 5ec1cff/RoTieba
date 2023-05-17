package io.github.a13e300.ro_tieba

fun ByteArray.toHexString() = this.joinToString("") {
    String.format("%02x", it)
}

fun <T> String.fromJson(klass: Class<T>): T {
    return App.gson.fromJson(this, klass)
}
