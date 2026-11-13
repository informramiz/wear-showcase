package com.codebyramiz.breathefocus.core.data.sync

/** Paths and keys shared by the watch and phone apps. */
object DataLayerContract {
    /** Watch -> phone: recent session history as JSON. */
    const val PATH_SESSIONS = "/breathe/sessions"

    /** Phone -> watch: the user's default breathing pattern. */
    const val PATH_SETTINGS = "/breathe/settings"

    const val KEY_SESSIONS_JSON = "sessions_json"
    const val KEY_DEFAULT_PATTERN = "default_pattern"
    const val KEY_UPDATED_AT = "updated_at"

    /** How much history the watch pushes on each sync. */
    const val SYNC_WINDOW_DAYS = 30L
}
