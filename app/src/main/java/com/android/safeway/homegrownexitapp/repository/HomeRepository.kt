package com.android.safeway.homegrownexitapp.repository

import androidx.lifecycle.MutableLiveData
import com.android.safeway.homegrownexitapp.model.OrderLookupResponse
import com.android.safeway.homegrownexitapp.network.BaseNetworkDelegate
import com.android.safeway.homegrownexitapp.network.DataWrapper
import com.android.safeway.homegrownexitapp.network.Environment
import com.android.safeway.homegrownexitapp.network.HandleOderLookup
import com.android.safeway.homegrownexitapp.util.Constants
import com.safeway.android.network.model.BaseNetworkError

class HomeRepository {
    fun initOrderLookup(
        orderId: String
    ): MutableLiveData<DataWrapper<OrderLookupResponse>> {
        val liveData: MutableLiveData<DataWrapper<OrderLookupResponse>> = MutableLiveData()

        HandleOderLookup(
            object : BaseNetworkDelegate<OrderLookupResponse> {
                override fun onSuccess(response: OrderLookupResponse?) {
                    val wrapper: DataWrapper<OrderLookupResponse> = DataWrapper()
                    wrapper.data = response
                    wrapper.status = DataWrapper.STATUS.SUCCESS
                    liveData.postValue(wrapper)
                }

                override fun onError(error: BaseNetworkError?) {
                    val wrapper: DataWrapper<OrderLookupResponse> = DataWrapper()
                    wrapper.errorCode = "" + error?.httpStatus
                    wrapper.message = error?.errorString
                    wrapper.status = DataWrapper.STATUS.FAILED
                    liveData.postValue(wrapper)
                }
            }, orderId
        ).startNwConnection((Constants.isDevelopping))
        return liveData
    }
}