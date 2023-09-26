package io.github.a13e300.ro_tieba.arch

class Event<T>(val data: T) {
    var handled = false

    inline fun handle(handler: (T) -> Unit) {
        if (!handled) {
            handler(data)
            handled = true
        }
    }
}
