package com.summertaker.jnews2

import android.graphics.Bitmap
import android.net.Uri

data class Video(
    var id: Long,
    var displayName: String,
    var contentUri: Uri,
    var thumbnail: Bitmap,
    var japanese: String? = null,
    var furigana: String? = null,
    var korean: String? = null,
    var style: String? = null
)