package com.safeway.android.network.api

import com.safeway.android.network.model.BaseNetworkError
import com.safeway.android.network.model.BaseNetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Optional handler class that could be extended in order to call APIHandler
 * Use this if you do not wish to use coroutines in your repository classes
 * Just implement returnResult and returnError
 * Patterned after NWHandlerBase2 of eComm, but without authentication
 */
abstract class NWHandler<T>: BaseNWHandler<T>() {

    /***
     * main entry point for Handlers to start execution.
     * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
     */
    open fun startNwConnection() {
        startNwConnection(false)
    }

    /***
     * main entry point for Handlers to start execution.
     * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
     */
    open suspend fun startSuspendedNwConnection() {
        startNwConnection(false)
    }

    /***
     * main entry point for Handlers to start execution.
     * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
     */
    open suspend fun startSuspendedNwConnection(withLogging: Boolean) = withContext(Dispatchers.IO){
        numRetries = 0
        var result = execute(withLogging)
        result?.let {res ->
            res.outputContent?.let {_ ->
                if (isValidResponse(res.outputContent))
                    returnSuspendedResult(res)
                else {
                    returnSuspendedError(BaseNetworkError(
                            BaseNetworkError.RESPONSE_NOT_ACCEPTABLE,
                            res.httpStatusCode
                    ).apply {
                        tag = getErrorLabelName()
                    })
                    logger?.log(
                            javaClass.simpleName,
                            "Error: ${BaseNetworkError.RESPONSE_NOT_ACCEPTABLE}"
                    )
                }
            } ?: run {
                if(res.err != null) {
                    returnSuspendedError(res.err)
                    logger?.log(javaClass.simpleName, "Error: ${res.err.errorString ?: ""}")
                }
            }
        }
    }

    /***
     * main entry point for Handlers to start execution.
     * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
     */
    open suspend fun getSuspendedNwConnectionResults(withLogging: Boolean) :
            BaseNetworkResult<T?>?  {

        numRetries = 0
        val result = execute(withLogging)
        result?.let { res ->
            res.outputContent?.let {_ ->
                if (isValidResponse(res.outputContent))
                    returnSuspendedResult(res)
                else {
                    val errorObj = BaseNetworkError(
                            BaseNetworkError.RESPONSE_NOT_ACCEPTABLE,
                            res.httpStatusCode
                    ).apply {
                        tag = getErrorLabelName()
                    }
                    // to return error object as part of result to calling function
                    result.err = errorObj
                    returnSuspendedError(errorObj)
                    logger?.log(
                            javaClass.simpleName,
                            "Error: ${BaseNetworkError.RESPONSE_NOT_ACCEPTABLE}"
                    )
                }
            } ?: run {
                if(res.err != null) {
                    returnSuspendedError(res.err)
                    logger?.log(javaClass.simpleName, "Error: ${res.err.errorString ?: ""}")
                }
            }
        }
        return result
    }

    /***
     * main entry point for Handlers to start execution.
     * Main execute() function is made separate because we restart our trial counter once startNwConnection() is called
     */
    open fun startNwConnection(withLogging: Boolean) = CoroutineScope(Dispatchers.Main).launch{
        numRetries = 0
        var result = execute(withLogging)
        result?.let { res ->
            res.outputContent?.let { _ ->
                if (isValidResponse(res.outputContent)) {
                    returnResult(res)
                } else {
                    returnError(BaseNetworkError(
                        BaseNetworkError.RESPONSE_NOT_ACCEPTABLE,
                        res.httpStatusCode
                    ).apply {
                        tag = getErrorLabelName()
                    })
                    logger?.log(
                        javaClass.simpleName,
                        "Error: ${BaseNetworkError.RESPONSE_NOT_ACCEPTABLE}"
                    )
                }
            } ?: run {
                if (res.err != null) {
                    returnError(res.err)
                    logger?.log(javaClass.simpleName, "Error: ${res.err.errorString ?: ""}")
                }
            }
        }
    }

    open fun returnResult(res: BaseNetworkResult<T?>) {

    }

    open suspend fun returnSuspendedResult(res: BaseNetworkResult<T?>) = withContext(Dispatchers.IO){

    }

    open suspend fun returnSuspendedError(error: BaseNetworkError) = withContext(Dispatchers.IO){

    }

    open fun returnError(error: BaseNetworkError) {

    }

}