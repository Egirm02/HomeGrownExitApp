package com.safeway.android.network.api

import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult

abstract class BaseAPIHandler<T> {

    protected fun constructNetworkErrorResponse(message: String, statusCode: Int, tag: String): BaseNetworkResult<T?> {
        var networkError = BaseNetworkError(message, statusCode)
        networkError.tag = tag
        var networkResponse = BaseNetworkResult<T?>(null)
        networkResponse.err = networkError
        return networkResponse
    }
}