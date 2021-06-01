package com.android.safeway.homegrownexitapp.network


enum class Environment(
    val sngBaseUrl: String,
    val apimSubscriptionKey: String
) {
    PROD(
        "https://retail-api.azure-api.net/scanandgo",
        "703f24b413024248b302a2f16ab5a0fe"
    ),
    QA(
        "https://retail-api.azure-api.net/scanandgoqa",
        "c179355bad114429966b29453d540066"
    ),
    DEV(
        "https://retail-api.azure-api.net/scanandgodev",
        "7024f91451d74393bbf891483210dc28"
    );

}
