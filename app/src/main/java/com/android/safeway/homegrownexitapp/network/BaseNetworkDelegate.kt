package com.android.safeway.homegrownexitapp.network

import com.safeway.android.network.model.BaseNetworkError

interface BaseNetworkDelegate<S> {
    fun onSuccess(response: S?)
    fun onError(error: BaseNetworkError?)
}