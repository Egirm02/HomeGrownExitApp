package com.safeway.android.network.api

import android.os.AsyncTask.execute
import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult
import com.safeway.android.network.utils.NetworkConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.lang.reflect.Type

/**
 * Optional handler class that could be extended in order to call APIHandler
 * Use this if you do not wish to use delegates in your repository classes
 * Just call startNwConnection within a couroutine context
 * Patterned after NWHandlerBase2 of eComm, but without authentication
 */
abstract class BaseCoroutineNWHandler<T>: BaseNWHandler<T>() {

    /***
    * main entry point for Handlers to start execution.
    * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
    */
    open suspend fun startNwConnection() : BaseNetworkResult<T?>?{
        return startNwConnection(false)
    }

    suspend fun startNwConnection(withLogging: Boolean) : BaseNetworkResult<T?>?{
        numRetries = 0
        return execute(withLogging)
    }
}