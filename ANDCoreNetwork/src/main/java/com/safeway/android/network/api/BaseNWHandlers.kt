package com.safeway.android.network.api

import android.net.Uri
import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult
import com.safeway.android.network.utils.DomainEngine
import com.safeway.android.network.utils.NetworkConfiguration
import com.distil.protection.model.NetworkFailureException
import kotlinx.coroutines.*
import org.json.JSONException
import java.lang.reflect.Type

abstract class BaseNWHandler<T> {
    protected var numRetries = 0
    lateinit var apiHandler: BaseAPIHandler<T>
    protected var logger : NetworkLogger? = null
    protected var tag =javaClass.simpleName

    open fun getHttpMethod(): NetworkModuleHttpMethods = NetworkModuleHttpMethods.GET

    open fun isPIRequestData() = false           //by default, prevent logging PI in request data
    open fun isPIURL() = false                   //by default, prevent logging PI in URL
    open fun isValidResponse(response: T?) = true
    open fun getResponseType() : Class<T>? {
        //if return type is not a list
        return null
    }

    open suspend fun preexecute() {
        if (!isPIURL()) logger?.log(javaClass.simpleName, "URL: ${getFullUrl() ?: ""}")
        if (!isPIRequestData() && getHttpMethod() != NetworkModuleHttpMethods.GET) logger?.log(javaClass.simpleName, "Request Data: ${getRequestData()}")
    }

    open fun getParts(): List<Pair<String, Any>>? {
        //multipart parts. could be File or String
        //not yet functional in network module in a coroutine context
        return null
    }


    open fun getTypeToken(): Type? {
        //if return type is a list, return typetoken. check erums super dug locator for example
        return null
    }

    /**
     * This enables/disables the protection on using the headers...
     */
    open fun getDomainKey(): String? = null

    /**
     * This function controls the Improva protection. Default = disabled.
     * override this in the handler to disable the protection.
     * return true to enable protection.
     * return false to disable protection.
     */
    open fun enableDomainProtection() = false

    suspend fun execute(withLogging: Boolean) : BaseNetworkResult<T?>? = withContext(Dispatchers.IO) {
        numRetries++
        var config = NetworkConfiguration()
        config.withLogging = withLogging
        config.tag=tag
        apiHandler = APIHandler(config)

        var result: BaseNetworkResult<T?>? = null

        preexecute()

        val additionalHeaders = mutableListOf<Pair<String, String>>()
        if (enableDomainProtection()) {
            val impervaTokenResult = DomainEngine.getImpervaToken(getDomainKey(), tag, withLogging)
            when (impervaTokenResult) {
                is String -> { // Token
                    additionalHeaders.add("X-D-Token" to impervaTokenResult)
                }
                is NetworkFailureException -> { // Network Error
                    return@withContext BaseNetworkResult<T?>(null).apply {
                        err = BaseNetworkError(
                            impervaTokenResult.message ?: BaseNetworkError.IMPROVA_NETWORK_ERROR,
                            BaseNetworkError.IMPROVA_ERROR_CODE
                        ).apply {
                            this.tag = this@BaseNWHandler.tag
                            this.cause = impervaTokenResult
                        }
                    }
                }
                is TimeoutCancellationException -> { // Timeout error
                    logger?.log(javaClass.simpleName, "Imperva Token Timeout.")
                }
                else -> { // value is null, which means that Improva call should no be triggered!
                }
            }
        }

        if (getResponseType() != null || getTypeToken() != null) getUrl()?.let { url ->
            //instead of using callbacks, we are directly waiting for the handler to execute
            result = (apiHandler as APIHandler).executeSuspendedJsonRequest(
                getHttpMethod(),
                url,
                getHeaders().toMutableList().apply { addAll(additionalHeaders) }.toMap(),
                getQueryParameters().toMap(),
                getResponseType() ?: getTypeToken(),
                getRequestData(),
                getErrorLabelName(),
                getParts()
            )
        }

        result
    }

    /**
     * The URL for this API call.  Normally, just the base URL is needed without query parameters
     * (including zip code) appended.
     * Subclasses should use the getQueryParameters() method instead of adding params themselves.
     * Also, subclasses should use setUsingZipForGuest() to true if you wish to fall back to a zip
     * code parameter instead of including this directly.
     *
     * @return
     */
    protected abstract fun getUrl(): String?

    /**
     * Query-string data pairs.  Should be implemented by handlers that need to append query params
     * to their URL strings instead of directly adding to the url.
     *
     * @return empty list by default
     */
    open fun getQueryParameters(): List<Pair<String, String>> {
        return emptyList()
    }

    // Get Full URL with query parameters if exists
    private fun getFullUrl() : String? {
        if (getUrl().isNullOrBlank() || getQueryParameters().isEmpty()) return getUrl()
        val urlBuilder = Uri.parse(getUrl()).buildUpon()

        getQueryParameters().forEach {
            urlBuilder.appendQueryParameter(it.first, it.second)
        }
        return urlBuilder.build().toString()
    }

    /**
     * JSON-formatted data for POST/PUT.  URL-encoded data for FORM POST type requests.
     * Should be overridden by base classes, as default data is likely not what you'd like to send.
     *
     * @return "[]" if no implementation has been provided
     */
    @Throws(JSONException::class)
    open fun getRequestData(): String {
        return "[]"        // Reasonable default for JSON style calls
    }

    /**
     * If there are additional headers needed for some API, override and call
     * super.getHeaders(), adding the results to the custom list
     *
     * @return
     */
    open fun getHeaders(): List<Pair<String, String>> {
        return emptyList()
    }

    /***
     * method for finding the custom API Error Code from the network module's BaseNetworkError object
     * can be overriden by handlers if there's a custom error code format
     */
    abstract fun getAPIErrorCode(error: BaseNetworkError?): String

    /***
     * Abstract method for determining an API Error message from a given BaseNetworkError object
     */
    abstract fun getAPIErrorMessage(error: BaseNetworkError?): String

    /** Abstract method for getting the label name for this handler
     *
     */
    abstract fun getErrorLabelName(): String

    interface NetworkLogger {
        fun log(tag: String, msg: String)
    }
}