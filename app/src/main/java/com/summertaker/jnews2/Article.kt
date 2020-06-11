package com.summertaker.jnews2

data class Article(
    val id: String,
    val yid: String,
    var title: String,
    val file: String?,
    var displayName: String?
)