package com.codebyramiz.breathefocus.core.data.sync

/** Paths and keys shared by the watch and phone apps. */
object DataLayerContract {
    /**
     * Watch -> phone: recent session history as JSON, split across items
     * `/breathe/sessions/0`, `/breathe/sessions/1`, ... so each stays under the item size limit.
     */
    const val PATH_SESSIONS = "/breathe/sessions"

    /** Phone -> watch: the user's default breathing pattern. */
    const val PATH_SETTINGS = "/breathe/settings"

    const val KEY_SESSIONS_JSON = "sessions_json"
    const val KEY_DEFAULT_PATTERN = "default_pattern"
    const val KEY_UPDATED_AT = "updated_at"

    /** How much history the watch pushes on each sync. */
    const val SYNC_WINDOW_DAYS = 30L

    /**
     * Payload budget per session item. Data items are capped at 100 KB; the rest is headroom
     * for the item's other fields and encoding overhead.
     */
    const val MAX_SESSION_CHUNK_BYTES = 90 * 1024

    fun sessionChunkPath(index: Int): String = "$PATH_SESSIONS/$index"

    /** Index of a session chunk path, or null if [path] is not one. */
    fun sessionChunkIndex(path: String?): Int? = path?.removePrefix("$PATH_SESSIONS/")
        ?.takeIf { it != path }
        ?.toIntOrNull()
}
