package com.safeway.android.network.utils

/**
 * Configuration information for the network module
 */
class NetworkConfiguration() {

    companion object {

        val DEFAULT_CONNECT_TIMEOUT: Long = 30000 // Max time to establish connection
        val DEFAULT_READ_TIMEOUT: Long = 90000 // Max time to read data (longer)
    }

    var connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT
    var readTimeout: Long = DEFAULT_READ_TIMEOUT
    var withLogging: Boolean = false
    var tag: String = "OkHttp"

}