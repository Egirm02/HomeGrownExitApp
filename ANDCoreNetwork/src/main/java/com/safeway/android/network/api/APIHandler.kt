package com.safeway.android.network.api

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult
import com.safeway.android.network.retrofit.NetworkInstance
import com.safeway.android.network.retrofit.ServiceEndpoints
import com.safeway.android.network.utils.NetworkConfiguration
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

/**
 * Primary class used by the network module
 * There are two primary ways to make network calls using APIHandler. One is using callbacks (deprecated)
 * And another one using Kotlin Coroutines. The Kotlin Coroutines version requires the use
 * of the NetworkConfiguration
 */
class APIHandler<T> : Callback<JsonElement>, BaseAPIHandler<T> {
    private var retrofit: Retrofit
    private var serviceEndpoints: ServiceEndpoints? = null
    private var commonUtilsCallback: com.safeway.android.network.utils.Callback<T>? = null
    private var className: Class<T>? = null
    private var type: Type? = null //for list type return
    private var tag: String? = null

    constructor(configuration: NetworkConfiguration) {
        retrofit = NetworkInstance.Builder(null).setConnectTimeout(configuration.connectTimeout).setReadTimeout(configuration.readTimeout).isWithLogging(configuration.withLogging).setTag(configuration.tag).build()
        serviceEndpoints = retrofit.create(ServiceEndpoints::class.java)
    }

    constructor(callback: com.safeway.android.network.utils.Callback<T>, withLogging: Boolean) : this(callback, NetworkConfiguration.DEFAULT_CONNECT_TIMEOUT, NetworkConfiguration.DEFAULT_READ_TIMEOUT, withLogging) {}

    constructor(callback: com.safeway.android.network.utils.Callback<T>, withLogging: Boolean,tag: String) : this(callback, NetworkConfiguration.DEFAULT_CONNECT_TIMEOUT, NetworkConfiguration.DEFAULT_READ_TIMEOUT, withLogging,tag) {}

    constructor(callback: com.safeway.android.network.utils.Callback<T>, connectTimeout: Long, readTimeout: Long) {
        retrofit = NetworkInstance.Builder(null).setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build()
        serviceEndpoints = retrofit.create(ServiceEndpoints::class.java)
        commonUtilsCallback = callback
    }

    constructor(callback: com.safeway.android.network.utils.Callback<T>, connectTimeout: Long, readTimeout: Long, withLogging: Boolean) {
        retrofit = NetworkInstance.Builder(null).setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).isWithLogging(withLogging).build()
        serviceEndpoints = retrofit.create(ServiceEndpoints::class.java)
        commonUtilsCallback = callback
    }

    constructor(callback: com.safeway.android.network.utils.Callback<T>, connectTimeout: Long, readTimeout: Long, withLogging: Boolean,tag: String) {
        retrofit = NetworkInstance.Builder(null).setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).isWithLogging(withLogging).setTag(tag).build()
        serviceEndpoints = retrofit.create(ServiceEndpoints::class.java)
        commonUtilsCallback = callback
    }

    /**
     * Execute the network call using coroutines
     * accepts Type or Class
     */
    suspend fun executeSuspendedJsonRequest(method: NetworkModuleHttpMethods?, url: String, headers: Map<String, String>, queryOptions: Map<String, String>, typeOrClass: Any?, requestBody: String, tag: String, parts: List<Pair<String, Any>>? = null) : BaseNetworkResult<T?>{

        var networkResponse: BaseNetworkResult<T?>? = null

        if (method == null) {
            networkResponse = constructNetworkErrorResponse(BaseNetworkError.INVALID_METHOD, BaseNetworkError.INVALID_METHOD_CODE, tag)
            return networkResponse
        }

        if(typeOrClass == null || (typeOrClass is Type).not()) {
            networkResponse = constructNetworkErrorResponse(BaseNetworkError.INVALID_METHOD, BaseNetworkError.INVALID_METHOD_CODE, tag)
            return networkResponse
        }

        try {
            var response : Response<JsonElement>? = null
            if(method != NetworkModuleHttpMethods.MULTIPARTPOST) {
                response = method.suspendedExecute(serviceEndpoints!!, url, headers, queryOptions, requestBody)
            } else {
                networkResponse = constructNetworkErrorResponse(BaseNetworkError.INVALID_METHOD, BaseNetworkError.INVALID_METHOD_CODE, tag)
            }

            response?.let { response ->
                if (response.isSuccessful && response.body() != null) {
                    if(typeOrClass is Type) {
                        networkResponse = BaseNetworkResult(Gson().fromJson<T>(response.body(), typeOrClass))
                    }
                    networkResponse?.httpStatusCode = response.code()
                    networkResponse?.responseHeaders = response.headers().toMultimap()
                    networkResponse?.tag = tag
                } else if (response.isSuccessful && response.body() == null) {
                    networkResponse = constructNetworkErrorResponse(BaseNetworkError.EMPTY_RESPONSE, response.code(), tag)
                } else {
                    try {
                        //3xx, 4xx, and 5xx error codes go here
                        networkResponse = constructNetworkErrorResponse(response.errorBody()?.string() ?: BaseNetworkError.UNKNOWN_ERROR, response.code(), tag)
                    } catch (e: IOException) {
                        //if there is a problem with parsing the Retrofit Response errorBody, you go here
                        networkResponse = constructNetworkErrorResponse(e, tag)
                    }

                }
            } ?: run {
                networkResponse = constructNetworkErrorResponse(BaseNetworkError.EMPTY_RESPONSE, BaseNetworkError.EMPTY_RESPONSE_CODE, tag)
            }

        } catch(e: Exception) {
            networkResponse = constructNetworkErrorResponse(e.message
                    ?: BaseNetworkError.EMPTY_RESPONSE, BaseNetworkError.EMPTY_RESPONSE_CODE, tag)
            networkResponse?.error?.cause = e
        }

        if(networkResponse == null) {
            networkResponse = constructNetworkErrorResponse(BaseNetworkError.UNKNOWN_ERROR, BaseNetworkError.UNKNOWN_ERROR_CODE, tag)
        }
        return networkResponse as BaseNetworkResult<T?>
    }

//    private fun constructNetworkErrorResponse(message: String, statusCode: Int, tag: String): BaseNetworkResult<T?> {
//        var networkError = BaseNetworkError(message, statusCode)
//        networkError.tag = tag
//        var networkResponse = BaseNetworkResult<T?>(null)
//        networkResponse.err = networkError
//        return networkResponse
//    }

    private fun constructNetworkErrorResponse(e: Exception, tag: String): BaseNetworkResult<T?> {
        var networkError = BaseNetworkError(e)
        networkError.tag = tag
        var networkResponse = BaseNetworkResult<T?>(null)
        networkResponse.err = networkError
        return networkResponse
    }

    //for multipart formpost
    @JvmOverloads
    fun executeJSONRequest(method: NetworkModuleHttpMethods?, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, type: Type?, requestBody: String?, tag: String?, parts: List<Pair<String, Any>>? = null) {
        if (method == null) {
            return
        }
        this.tag = tag
        this.type = type
        val call: Call<JsonElement>?
        if (parts != null && method == NetworkModuleHttpMethods.MULTIPARTPOST) {
            call = method.execute(serviceEndpoints!!, url, headers, queryOptions, requestBody, parts)
        } else {
            call = method.execute(serviceEndpoints!!, url, headers, queryOptions, requestBody)
        }

        call!!.enqueue(this)
    }

    @JvmOverloads
    fun executeJSONRequest(method: NetworkModuleHttpMethods?, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, className : Class<T>?, requestBody: String?, tag: String?, parts: List<Pair<String, Any>>? = null) {
        if (method == null) {
            return
        }
        this.tag = tag
        this.className = className
        var call: Call<JsonElement>? = null
        if (parts != null && method == NetworkModuleHttpMethods.MULTIPARTPOST) {
            call = method.execute(serviceEndpoints!!, url, headers, queryOptions, requestBody, parts)
        } else {
            call = method.execute(serviceEndpoints!!, url, headers, queryOptions, requestBody)
        }

        call!!.enqueue(this)
    }

    override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
        if (response.isSuccessful && (className != null || type != null)) {
            val networkResult: BaseNetworkResult<T>
            if (className != null) {
                networkResult = BaseNetworkResult(Gson().fromJson(response.body(), className!!))
            } else {
                networkResult = BaseNetworkResult(Gson().fromJson<T>(response.body(), type))
            }
            networkResult.httpStatusCode = response.code()
            networkResult.responseHeaders = response.headers().toMultimap()
            networkResult.tag = tag
            commonUtilsCallback!!.returnResult(networkResult)
        } else if (response.isSuccessful && className == null && type == null) {
            //Assume that T in this case is not defined by the app (possible only in Java)
            //this will throw an exception otherwise
            val networkResult = BaseNetworkResult(response.body() as T)
            networkResult.responseHeaders = response.headers().toMultimap()
            networkResult.tag = tag
            commonUtilsCallback!!.returnResult(networkResult)
//            throw Exception()
        } else {
            try {
                //3xx, 4xx, and 5xx error codes go here
                val baseNetworkError = BaseNetworkError(response.errorBody()!!.string(), response.code())
                baseNetworkError.tag = tag
                commonUtilsCallback!!.returnError(baseNetworkError)
            } catch (e: IOException) {
                //if there is a problem with parsing the Retrofit Response errorBody, you go here
                val baseNetworkError = BaseNetworkError(e)
                baseNetworkError.tag = tag
                commonUtilsCallback!!.returnError(baseNetworkError)
            }

        }
    }

    override fun onFailure(call: Call<JsonElement>, t: Throwable) {
        //if there is a Retrofit-related error (not a 3xx, 4xx, or 5xx error, but things like SocketTimeoutException and UnknownHostException), they go here.
        //        Log.v("Temp", "IN HERE ON FAILURE");
        val baseNetworkError: BaseNetworkError
        if (t is Exception) {
            baseNetworkError = BaseNetworkError(t)
            baseNetworkError.tag = tag
        } else {
            baseNetworkError = BaseNetworkError(t.message, 1001)
            baseNetworkError.tag = tag
        }
        commonUtilsCallback!!.returnError(baseNetworkError)
    }

}
