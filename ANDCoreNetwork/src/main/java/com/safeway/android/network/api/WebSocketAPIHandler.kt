package com.safeway.android.network.api

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult
import com.safeway.android.network.retrofit.NetworkInstance
import com.safeway.android.network.retrofit.ServiceEndpoints
import com.safeway.android.network.utils.NetworkConfiguration
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import okio.ByteString
import java.lang.reflect.Type

/**
 * APIHandler specifically made for consuming web sockets
 */
class WebSocketAPIHandler<T> : BaseAPIHandler<T> {

    //Retrofit does not support WebSockets yet, but OkHttp does so we're bypassing Retrofit for WS calls

    private var client: OkHttpClient? = null
    private var ws: WebSocket? = null
    private var liveData : MutableLiveData<BaseNetworkResult<T?>>
    private var config: NetworkConfiguration
    //Gian: Watch this space. Kotlin has Channel and ConflatedBroadcastChannel classes that are meant to be used for streaming data
    //however, integration with JetPack's LiveData is still in development
    //so for the meantime, we are using LiveData directly to post the results of our WebSocketListener
    //However, this is not ideal because the LiveData has to be observed up to the UI level in order to function
    //Ideally, we want the API Handler to expose a Kotlin Channel instead of a LiveData to make it fully indepenent from the consuming app

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
        val TAG = WebSocketAPIHandler::class.java.simpleName
    }

    constructor(configuration: NetworkConfiguration, ld: MutableLiveData<BaseNetworkResult<T?>>) {
        client = NetworkInstance.Builder(null).setConnectTimeout(configuration.connectTimeout).setReadTimeout(configuration.readTimeout).isWithLogging(configuration.withLogging).setTag(configuration.tag).createOkHttpClient()
        liveData = ld
        config = configuration
    }

    fun closeWebSocket() {
        ws?.close(NORMAL_CLOSURE_STATUS, null)
    }

    fun openWebSocket(url: String, typeOrClass: Any?) {
        client?.let {
            val request = Request.Builder().url(url).build();
            val listener = DefaultWebSocketListener(typeOrClass, liveData, config.withLogging);
            ws = it.newWebSocket(request, listener)
        }
    }

    class DefaultWebSocketListener<T>(private var typeOrClass: Any?, private var liveData: MutableLiveData<BaseNetworkResult<T?>>, private var withLogging: Boolean): WebSocketListener() {

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            if(withLogging) {
                Log.v(TAG, "onOpen")
            }
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            if(withLogging) {
                Log.v(TAG, "text: " + text)
            }
            if(text.isNullOrBlank().not()) {
                var networkResponse: BaseNetworkResult<T?>? = null
                try {
                    if(typeOrClass is Type) {
                        text?.let {
                            networkResponse = BaseNetworkResult(Gson().fromJson<T>(text, typeOrClass as Type))
                            liveData.postValue(networkResponse)
                        }
                    }
                } catch (e: Exception) {
                    //do nothing for now
                    Log.v(TAG, "error: " + e.toString())
                }
            }

        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
//            webSocket?.close(NORMAL_CLOSURE_STATUS, null)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            var nw = BaseNetworkResult<T?>(null)
            nw.error = BaseNetworkError(t.toString(), response?.code() ?: -1)
            liveData.postValue(nw)
        }
    }

}