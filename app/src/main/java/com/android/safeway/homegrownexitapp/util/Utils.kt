package com.android.safeway.homegrownexitapp.util

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.android.safeway.homegrownexitapp.R
import com.android.safeway.homegrownexitapp.network.Environment

object Utils {



    private var dialog: Dialog? = null

    @SuppressLint("InflateParams")
    fun showProgressDialog(context: Context, isCancellable: Boolean = true) {
        //if dialog is alredy visible, dont do anything
        dialog?.let {
            if (it.isShowing) {
                return
            }
        }
        val inflater: LayoutInflater? =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val view: View? = inflater?.inflate(R.layout.progress, null)
        dialog = Dialog(context)
        view?.let { it ->
            dialog?.setContentView(it)
            dialog?.setCancelable(isCancellable)
            dialog?.show()
        }
    }

    fun dismissProgressDialog() {
        dialog?.dismiss()
        dialog = null //just to not keep the instance
    }
}