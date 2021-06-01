package com.safeway.android.network.utils

import android.content.Context
import android.util.Log
import com.distil.protection.android.Protection
import com.distil.protection.functional.Receiver
import com.distil.protection.model.NetworkFailureException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withTimeout
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton class which handles and saves all the protection objects
 * and will be used for every API call.
 */
object DomainEngine {

    var impervaEnabled = false
    val domainMap = mutableMapOf<String, DomainWrapper>()

    /**
     * This function should be called on the application level.
     */
    @JvmStatic
    fun init(context: Context, domainMap: Map<String, String>, impervaEnabled: Boolean = false) {
        this.impervaEnabled = impervaEnabled
        if (impervaEnabled) {
            this.domainMap.clear()
            domainMap.forEach {
                this.domainMap[it.key] = DomainWrapper(
                    protection = Protection.protection(context, URL(it.value))
                )
            }
        }
    }

    /**
     * function that takes care of all the cases possible to get the token from Impreva SDK.
     */
    suspend fun getImpervaToken(domainKey: String?, tag: String, withLogging: Boolean): Any? {
        try {
            val finalResult = withTimeout(IMPERVA_TOKEN_TIMEOUT) {
                var result: Any? = null
                if (domainKey != null && impervaEnabled) {
                    // Imperva is enabled, and the api is supported.
                    domainMap[domainKey]?.let { domainWrapper ->
                        // Protection object is created in the app lvl, get the token...
                        val tokenReceived = AtomicBoolean(false)
                        domainWrapper.protection.getToken(
                            Receiver<String> { token ->
                                // Token is successfully coming
                                result = token
                                tokenReceived.set(true)
                            },
                            Receiver<NetworkFailureException> { exception ->
                                // Exception, probably from a network connection issues...
                                result = exception
                                if (withLogging) Log.w(
                                    tag, "Improva Exception: " +
                                            "SDK or Network connection issues."
                                )
                                tokenReceived.set(true)
                            }, Dispatchers.IO.asExecutor()
                        )
                        /**
                         * This is needed because currently the sdk does not support Coroutines,
                         * and has an attribute of an executor.
                         * one of the callbacks has to be triggered!
                         */
                        while (true) {
                            if (tokenReceived.get()) break
                        }
                    } ?: run {
                        // No need to do anything here!
                        if (withLogging) Log.w(
                            tag,
                            "The Protection has not been found. " +
                                    "Check if the domain $domainKey is configured!"
                        )
                    }
                } else {
                    // No need to do anything here!
                    if (withLogging) Log.w(
                        tag,
                        "Imperva is disabled or the api domain is not configured yet!"
                    )
                }
                result
            }
            return finalResult
        } catch (timeoutException: TimeoutCancellationException) {
            return timeoutException
        }
    }
}

/**
 * This class is just a wrapper class that holds the protection object.
 * Reason why this is added is only for scaling, in case we have other values, we can just add it here...
 */
class DomainWrapper(
    val protection: Protection
)

private const val IMPERVA_TOKEN_TIMEOUT = 500L