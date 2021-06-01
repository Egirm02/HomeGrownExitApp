# ANDCoreNetwork

## Steps to implement

1. Run these commands in you project directory:
    ```groovy
    git submodule add https://github.com/J4U-Nimbus/ANDCoreNetwork.git
    git submodule update --init --recursive
    ```

2. Start by adding an SDK version map in the app root `build.gradle` file:
    ```groovy
    ext {

        // Used in buildscript (executed first)
        kotlin_version = '1.3.72'

        //Common Module dependencies
        commonModuleDependencies = [
            lifecycle   : "2.1.0"
        ]

        //Network Module dependencies
        networkModuleDependencies = [
            retrofit         : "2.6.2",
            okHttpInterceptor: "3.4.1",
            coroutinesCore   : "1.3.7",
            jwtDecode        : "1.1.1"
        ]
    }
    ```
    **Note**: these version values are the default if the map is not defined or one of the SDK/library versions is missing.

3. In your app's `build.gradle` file, add this line:
    ```groovy
    implementation project(':ANDCoreNetwork')
    ```
4. In your app's `settings.gradle` file, add this line:
    ```groovy
    include ':ANDCoreNetwork'
    ```
    
## Features

### REST Calls

Make REST calls using Retrofit. You may either extend ```NWHandler``` or ```BaseCoroutineNWHandler``` in order to do a quick setup, or use the ```APIHandler``` to implement your own network handlers. ```BaseCoroutineNWHandler``` requires projects to be enabled for Kotlin development.

#### Set-up

<details>
<summary>NWHandler</summary>

```kotlin
class YourBaseHandler<T> : NWHandler<T>(){

    override fun getErrorLabelName(): String = "" //add labels for error catching here
    override fun getAPIErrorCode(error: BaseNetworkError?): String {
       return "" //return the API error code for a given BaseNetworkError
    }

    override fun getAPIErrorMessage(error: BaseNetworkError?): String {
        return "" //Get the error message from a given BaseNetworkError
    }

    /**
    * Stock headers, plus token and content type headers
    * If there are additional headers needed for some API, override and call
    * super.getHeaders(), adding the results to the custom list
    *
    * @return
    */
    override fun getHeaders(): List<Pair<String, String>> {
        var copyHdrs = super.getHeaders()
        copyHdrs += Pair("Content-Type", "application/json") //add common headers here
        return copyHdrs
    }
    
    fun returnResult(res: BaseNetworkResult<T?>) {
        //indicate what to do with BaseNetworkResult
    }

    fun returnError(error: BaseNetworkError) {
        //indicate what to do with a given BaseNetworkError
    }
}
```
</details>
<details>
<summary>BaseCoroutineNWHandler</summary>

```kotlin
class YourBaseHandler<T> : BaseCoroutineNWHandler<T>(){

    override fun getErrorLabelName(): String = "" //add labels for error catching here
    override fun getAPIErrorCode(error: BaseNetworkError?): String {
       return "" //return the API error code for a given BaseNetworkError
    }

    override fun getAPIErrorMessage(error: BaseNetworkError?): String {
        return "" //Get the error message from a given BaseNetworkError
    }

    /**
    * Stock headers, plus token and content type headers
    * If there are additional headers needed for some API, override and call
    * super.getHeaders(), adding the results to the custom list
    *
    * @return
    */
    override fun getHeaders(): List<Pair<String, String>> {
        var copyHdrs = super.getHeaders()
        copyHdrs += Pair("Content-Type", "application/json") //add common headers here
        return copyHdrs
    }
}
```

</details>

### Web Sockets

Extend ```WebSocketAPIHandler``` and pass a ```LiveData``` object in order to open web sockets. This handler uses Retrofit's internal OkHttp web socket handler to make the connections.
