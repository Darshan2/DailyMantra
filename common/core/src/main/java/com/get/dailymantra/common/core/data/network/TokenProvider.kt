package com.get.dailymantra.common.core.data.network

interface TokenProvider {
    suspend fun getToken(): String?
}
