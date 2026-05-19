package com.ben.inly.data.local.prefs

object SyncConstants {
    // Sorting
    const val KEY_SORT_TYPE = "sort_type"
    const val KEY_SORT_ORDER = "sort_order"

    // Desktop State
    const val KEY_LAST_OPENED_STATE = "last_opened_desktop_state"

    // Sync Keys
    const val KEY_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val KEY_SYNC_AUTH_TOKEN = "sync_auth_token"
    const val KEY_SYNC_IP_ADDRESS = "sync_ip_address"
    const val KEY_SYNC_PORT = "sync_port"
    const val KEY_SYNC_ENCRYPTION_KEY = "sync_encryption_key"

    // Defaults
    const val DEFAULT_PORT = 8080
    const val DEFAULT_SORT_TYPE = "LAST_EDITED"
    const val DEFAULT_SORT_ORDER = "DESCENDING"

    // API Routes
    const val ROUTE_FETCH = "/sync/fetch"
    const val ROUTE_PUSH = "/sync/push"

    // Auth
    const val AUTH_REALM = "sync-auth"
}