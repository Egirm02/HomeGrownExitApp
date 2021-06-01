package com.safeway.android.network.utils;

import com.safeway.android.network.model.BaseNetworkError;
import com.safeway.android.network.model.BaseNetworkResult;

public interface Callback<T>  {

    void returnResult(BaseNetworkResult<T> result);
    void returnError(BaseNetworkError error);
}
