package com.example.primeflixlite

// 1. Dummy Annotations to satisfy the compiler
package com.m3u.annotation {
    annotation class Likable
    annotation class Exclude
}

// 2. Helper extension to fix string checks
package com.m3u.core.util.basic {
    fun String.startsWithAny(vararg prefixes: String, ignoreCase: Boolean = false): Boolean {
        return prefixes.any { this.startsWith(it, ignoreCase) }
    }
}

// 3. Dummy Resources to fix R.string references
package com.m3u.i18n {
    object R {
        object string {
            const val feat_setting_data_source_m3u = 0
            const val feat_setting_data_source_epg = 0
            const val feat_setting_data_source_xtream = 0
            const val feat_setting_data_source_emby = 0
            const val feat_setting_data_source_dropbox = 0
        }
    }
}