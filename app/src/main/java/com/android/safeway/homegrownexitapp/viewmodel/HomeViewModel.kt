package com.android.safeway.homegrownexitapp.viewmodel

import android.app.Application
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.safeway.homegrownexitapp.model.ItemDetail
import com.android.safeway.homegrownexitapp.model.OrderLookupResponse
import com.android.safeway.homegrownexitapp.network.DataWrapper
import com.android.safeway.homegrownexitapp.repository.HomeRepository
import com.android.safeway.homegrownexitapp.util.Utils


class HomeViewModel(application: Application) :
    BaseObservableViewModel(application) {
    internal class Factory(
        private val application: Application
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(application) as T
        }
    }

    private val tag = "HomeViewModel"
    private val homeRepository = HomeRepository()

    var showRed = ObservableBoolean(false)
    var showGreen = ObservableBoolean(false)
    var showWelcome = ObservableBoolean(true)
    var showSurfaceView = ObservableBoolean(true)
    var showImage = ObservableBoolean(false)


    enum class CALLBACK {
        NONE, SHOW_RED, SHOW_GREEN, SHOW_PROGRESS
    }

    var event: MutableLiveData<CALLBACK> = MutableLiveData(CALLBACK.NONE)
     var showredlight = false

    fun initLookup(orderId: String) {
        event.value = CALLBACK.SHOW_PROGRESS
        homeRepository.initOrderLookup(orderId)
            .observeForever { response: DataWrapper<OrderLookupResponse> ->
                Utils.dismissProgressDialog()
                if (response.status == DataWrapper.STATUS.SUCCESS) {

                    response.data?.let { response ->
                        if (response.ack != null && response.ack == "0") {
                            //success
                            var isRestrictedItem = false
                            if (response.data?.item_details != null
                                && response.data.item_details.isNotEmpty()
                            ) {
                                for (item: ItemDetail in response.data.item_details) {
                                    if (item.restricted_item == true) {
                                        isRestrictedItem = true
                                        break
                                    }
                                }
                            }
                            if (isRestrictedItem) {

                                /*//failed
                                setRed()*/

                                //todo: Shrini - remove harcoding showredlight logic once services are up and running.
                                if(!showredlight){
                                    showredlight = true
                                    showGreen.set(true)
                                    showRed.set(false)
                                    event.setValue(CALLBACK.SHOW_GREEN)
                                }else{
                                    showredlight = false
                                    //failed
                                    setRed()
                                }
                            } else {
                                /*showGreen.set(true)
                                showRed.set(false)
                                event.postValue(CALLBACK.SHOW_GREEN)*/

                                //todo: Shrini - remove harcoding showredlight logic once services are up and running.

                                if(!showredlight){
                                    showredlight = true
                                    showGreen.set(true)
                                    showRed.set(false)
                                    event.setValue(CALLBACK.SHOW_GREEN)
                                }else{
                                    showredlight = false
                                    //failed
                                    setRed()
                                }
                            }

                        } else {
                            //todo: Shrini - remove harcoding showredlight logic once services are up and running.
                            if(!showredlight){
                                showredlight = true
                                showGreen.set(true)
                                showRed.set(false)
                                event.setValue(CALLBACK.SHOW_GREEN)
                            }else{
                                showredlight = false
                                //failed
                                setRed()
                            }
                            /*//failed
                            setRed()*/


                        }
                    }
                } else {
                  /*  //failed
                    setRed()*/
                    if(!showredlight){
                        showredlight = true
                        showGreen.set(true)
                        showRed.set(false)
                        event.setValue(CALLBACK.SHOW_GREEN)
                    }else{
                        showredlight = false
                        //failed
                        setRed()
                    }
                }
            }
    }

    private fun setRed() {
        showRed.set(true)
        showGreen.set(false)
        event.setValue(CALLBACK.SHOW_RED)

    }
}
