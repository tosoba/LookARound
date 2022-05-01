package com.lookaround.core.android.ext

import android.net.Uri

val String.uri: Uri
    get() =
        Uri.parse(
            if (!startsWith("https://") && !startsWith("http://")) {
                "http://$this"
            } else {
                this
            }
        )
