package com.get.dailymantra.common.core.data.network

import javax.inject.Inject

class DefaultTokenProvider @Inject constructor() : TokenProvider {
    override suspend fun getToken(): String? = null
}
