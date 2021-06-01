package com.android.safeway.homegrownexitapp.network

import com.android.safeway.homegrownexitapp.BuildConfig
import com.android.safeway.homegrownexitapp.model.OrderLookupResponse
import com.android.safeway.homegrownexitapp.util.Constants
import com.safeway.android.network.api.NWHandler
import com.safeway.android.network.api.NetworkModuleHttpMethods
import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult

class HandleOderLookup(
    private val delegate: BaseNetworkDelegate<OrderLookupResponse>,
    private val orderId: String,
) : NWHandler<OrderLookupResponse>() {

    private val path = "/getReceipt?orderId="
    override fun getResponseType() = OrderLookupResponse::class.java

    override fun getUrl() = Constants.environment.sngBaseUrl + path + orderId


    override fun getHeaders(): List<Pair<String, String>> {
        val headers: MutableList<Pair<String, String>> = ArrayList()
        headers.add(Pair(Constants.KEY_CONTENT_TYPE, Constants.VAL_CONTENT_TYPE))
        headers.add(Pair(Constants.KEY_ACCEPT, Constants.VAL_ACCEPT))
        headers.add(Pair(Constants.APP, Constants.APP_HOMEGROWN_EXIT))
        headers.add(Pair(Constants.KEY_VERSION, BuildConfig.VERSION_NAME))
        headers.add(Pair(Constants.KEY_OCP_APIM_KEY, Constants.environment.apimSubscriptionKey))
        return headers
    }

    override fun getHttpMethod() = NetworkModuleHttpMethods.GET

    override fun getErrorLabelName(): String = HandleOderLookup::class.java.simpleName

    override fun returnError(error: BaseNetworkError) {
        val message: String = getAPIErrorCode(error)
        error.errorString = message
        delegate.onError(error)
    }

    override fun returnResult(res: BaseNetworkResult<OrderLookupResponse?>) {
        delegate.onSuccess(res.outputContent)

    }

    override fun getAPIErrorCode(error: BaseNetworkError?): String {
        error?.errorString?.let {
            return it
        }
        return ""
    }

    override fun getAPIErrorMessage(error: BaseNetworkError?): String {
        error?.errorString?.let {
            return it
        }
        return ""
    }

}