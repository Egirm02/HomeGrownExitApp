package com.safeway.android.network.utils

import android.content.Context
import android.net.ConnectivityManager

class NetworkUtils {

    companion object {

        @JvmStatic
        fun isNetworkAvailable(context: Context) : Boolean {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                return cm?.activeNetworkInfo?.isConnected == true
            } catch(e: Exception) {
                return false
            }
        }
    }
}