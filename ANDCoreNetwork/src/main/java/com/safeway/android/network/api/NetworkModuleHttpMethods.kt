package com.safeway.android.network.api

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.safeway.android.network.retrofit.ServiceEndpoints
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.util.*

enum class NetworkModuleHttpMethods {
    GET_STRING {
        override suspend fun suspendedExecute(
            serviceEndpoints: ServiceEndpoints,
            url: String,
            headers: Map<String, String>?,
            queryOptions: Map<String, String>?,
            body: String?
        ): Response<JsonElement>? {
            val response = serviceEndpoints.suspendedGetString(url, headers ?: HashMap(), queryOptions ?: HashMap())
            return if (response.isSuccessful && response.body() !=null){
                Response.success(JsonParser().parse(JSONObject().put("data",response.body()).toString()),response.headers())
            }else{
                Response.error(response.code(),response.errorBody())
            }
        }
    },
    GET {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.get(url, headers ?: HashMap(), queryOptions ?: HashMap())
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedGet(url, headers ?: HashMap(), queryOptions ?: HashMap())
        }
    },
    FORMPOST {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.post(url, headers ?: HashMap(), requestBody,queryOptions?: HashMap())
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedPost(url, headers ?: HashMap(), requestBody,queryOptions?: HashMap())
        }
    },
    POST {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.post(url, headers ?: HashMap(), requestBody,queryOptions?: HashMap())
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedPost(url, headers ?: HashMap(), requestBody,queryOptions?: HashMap())
        }
    },
    DELETE {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.delete(url, headers ?: HashMap())
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedDelete(url, headers ?: HashMap(), requestBody)
        }
    },
    PUT {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.put(url, headers ?: HashMap(), requestBody)
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedPut(url, headers ?: HashMap(), requestBody)
        }
    },
    PATCH {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Call<JsonElement>? {
            return serviceEndpoints.patch(url, headers ?: HashMap(), requestBody)
        }
        override suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, requestBody: String?): Response<JsonElement>? {
            return serviceEndpoints.suspendedPatch(url, headers ?: HashMap(), requestBody)
        }
    },
    MULTIPARTPOST {
        override fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, body: String?, partsMap: List<Pair<String, Any>>?): Call<JsonElement>? {

            val partList = ArrayList<MultipartBody.Part>()
            if (partsMap != null) {
                val it = partsMap.iterator()
                while (it.hasNext()) {
                    val map = it.next()
                    if (map.second is File) {
                        val part = prepareFilePart(map.first, map.second as File)
                        part?.let{ partList.add(part) }
                    } else if (map.second is String) {
                        val part = prepareStringPart(map.first, map.second as String)
                        part?.let{ partList.add(part) }
                    }
                }
            }
            return serviceEndpoints.postMultipart(url, headers ?: HashMap(), partList)
        }
    };

    /**
     * Method for picking the right ServiceEndpoint call. Should be overriden within the enums. Deprecated in favor of the coroutine version. Not all parameters are used. This depends on the particular call
     * @param serviceEndpoints
     * @param url
     * @param headers
     * @param queryOptions
     * @param body
     * @return
     */
    @Deprecated("Use suspendedExecute instead", replaceWith = ReplaceWith("executeSuspended"))
    open fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, body: String?): Call<JsonElement>? {
        return null
    }

    open suspend fun suspendedExecute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, body: String?): Response<JsonElement>? {
        return null
    }

    open fun execute(serviceEndpoints: ServiceEndpoints, url: String, headers: Map<String, String>?, queryOptions: Map<String, String>?, body: String?, partsMap: List<Pair<String, Any>>?): Call<JsonElement>? {
        return null
    }

    fun prepareStringPart(partName: String, json: String): MultipartBody.Part {

        // create RequestBody instance from file
        val requestString = RequestBody.create(
                MediaType.parse("multipart/form-data"),
                json
        )

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, null, requestString)

    }


    fun prepareFilePart(partName: String, file: File): MultipartBody.Part? {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri

        // create RequestBody instance from file
        if (file.exists()) {
            val requestFile = RequestBody.create(
                    MediaType.parse("multipart/form-data"),
                    file
            )

            // MultipartBody.Part is used to send also the actual file name
            return MultipartBody.Part.createFormData(partName, file.name, requestFile)
        } else {
            return null
        }

    }
}