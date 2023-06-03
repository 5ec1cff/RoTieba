package io.github.a13e300.ro_tieba.models

sealed class Content {
    data class TextContent(val text: String) : Content()
    data class ImageContent(
        val previewSrc: String,
        val src: String,
        val width: Int,
        val height: Int,
        val order: Int
    ) : Content()

    data class EmojiContent(
        val id: String
    ) : Content()

    data class LinkContent(val text: String, val link: String) : Content()
}