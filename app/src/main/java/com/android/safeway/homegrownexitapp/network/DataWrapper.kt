package com.android.safeway.homegrownexitapp.network

class DataWrapper<T>{

    var data: T? = null
    var status: STATUS? = null
    var message: String? = null //for error handling
    var errorCode: String? = null
    var customErrorObject: Any? = null //for error types that errorCode/message alone cannot handle; currently used only by StoreRedirectFilterObject
    var id: String? = null // Unique id for each call

    enum class STATUS {
        SUCCESS, FAILED
    }

    constructor() {}

    constructor(data: T?, status: STATUS) {
        this.data = data
        this.status = status
    }

    constructor(data: T?, status: STATUS, message: String) {
        this.data = data
        this.status = status
        this.message = message
    }

    constructor(data: T?, status: STATUS, message: String, errorCode: String) {
        this.data = data
        this.status = status
        this.message = message
        this.errorCode = errorCode
    }

    constructor(data: T?, status: STATUS, message: String, errorCode: String, customErrorObject: Any) {
        this.data = data
        this.status = status
        this.message = message
        this.errorCode = errorCode
        this.customErrorObject = customErrorObject
    }

    constructor(data: T?, status: STATUS, message: String, errorCode: String, customErrorObject: Any, id: String) : this(data, status, message, errorCode, customErrorObject) {
        this.id = id
    }
}
