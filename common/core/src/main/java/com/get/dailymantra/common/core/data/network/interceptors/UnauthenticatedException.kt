package com.get.dailymantra.common.core.data.network.interceptors

import java.io.IOException

/** Thrown when a request requires auth but no access token is available locally. */
class UnauthenticatedException : IOException("No access token available")