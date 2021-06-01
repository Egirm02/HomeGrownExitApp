package com.android.safeway.homegrownexitapp.model

data class OrderLookupResponse(
    val ack: String?,
    val data: Data?,
    val errors: List<Error>?
)

data class Data(val item_details: List<ItemDetail>?)

data class ItemDetail(
    val item_id: String?,
    val quantity: Int?,
    val restricted_item: Boolean?,
    val scan_code: String?,
    val status: String?,
    val upc_type: String?,
)