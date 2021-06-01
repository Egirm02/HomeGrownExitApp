package com.safeway.android.network.model

import android.text.TextUtils

/**
 * Class to encapsulate what can go wrong when a network error happens.
 *
 * Created by tsanidas on 3/9/18.
 */

open class BaseNetworkError {

    var errorString: String? = null
    var httpStatus: Int = 0
    var cause: Exception? = null
    var tag: String? = null

    constructor(errorString: String?, httpStatusCode: Int) {
        this.errorString = errorString
        this.httpStatus = httpStatusCode
    }

    constructor(cause: Exception) {
        this.cause = cause
    }

    companion object {
        val INVALID_METHOD = "INVALID_METHOD"
        val UNKNOWN_ERROR = "UNKNOWN_ERROR"
        val EMPTY_RESPONSE = "EMPTY_RESPONSE"
        val RESPONSE_NOT_ACCEPTABLE = "RESPONSE_NOT_ACCEPTABLE"
        val IMPROVA_NETWORK_ERROR = "IMPROVA_NETWORK_ERROR"

        val INVALID_METHOD_CODE = 9001
        val UNKNOWN_ERROR_CODE = 9002
        val EMPTY_RESPONSE_CODE = 9003
        val RESPONSE_NOT_ACCEPTABLE_CODE = 9004
        val IMPROVA_ERROR_CODE = 9005
    }
}
