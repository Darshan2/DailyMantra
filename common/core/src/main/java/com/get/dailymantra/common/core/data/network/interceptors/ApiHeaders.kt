package com.get.dailymantra.common.core.data.network.interceptors

object ApiHeaders {
    object Names {
        const val AUTHORIZATION = "Authorization"
        const val APP_LANGUAGE = "App-Language"
        const val APP_VERSION = "App-Version"
        const val REQUEST_ID = "Request-Id" // Idem potent value for PATCH and POST requests
        const val CONTENT_TYPE = "Content-Type"
        const val RETRY_AFTER = "Retry-After" // Rate limit header sent by server
    }

    object Values {
        const val BEARER_PREFIX = "Bearer "
        const val CONTENT_TYPE_JSON = "application/json"
    }
}