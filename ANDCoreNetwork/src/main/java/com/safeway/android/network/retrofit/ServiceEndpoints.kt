package com.safeway.android.network.retrofit

import com.google.gson.JsonElement

import org.json.JSONObject

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ServiceEndpoints {

    /**
     * Method for making GET calls. Uses callbacks. Deprecated in favor of the couroutine version
     * @param url       The URL for the call
     * @param headers   The header map
     * @param options   The query map
     */
    @Deprecated("Use suspendedGet instead")
    @GET
    fun get(@Url url: String, @HeaderMap headers: Map<String, String>,
                     @QueryMap options: Map<String, String>): Call<JsonElement>

    /**
     * Method for making GET calls. Uses coroutines
     * @param url       The URL for the call
     * @param headers   The header map
     * @param options   The query map
     */
    @GET
    suspend fun suspendedGet(@Url url: String, @HeaderMap headers: Map<String, String>,
                             @QueryMap options: Map<String, String>): Response<JsonElement>

    /**
     * Method for making GET image calls. Uses coroutines
     * @param url       The URL for the call
     * @param headers   The header map
     * @param options   The query map
     */
    @GET
    suspend fun suspendedGetString(@Url url: String, @HeaderMap headers: Map<String, String>,
                             @QueryMap options: Map<String, String>): Response<String>

    /**
     * Method for making POST calls. Uses callbacks. Deprecated in favor of the couroutine version
     * @param url       The URL for the call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @Deprecated("Use suspendedPost instead")
    @POST
    fun post(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?,@QueryMap options: Map<String, String>): Call<JsonElement>

    /**
     * Method for making POST calls. Uses coroutines
     * @param url       The URL for the call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @POST
    suspend fun suspendedPost(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?,@QueryMap options: Map<String, String>): Response<JsonElement>

    @Multipart
    @POST
    fun postMultipart(@Url url: String, @HeaderMap headers: Map<String, String>, @Part files: List<MultipartBody.Part>): Call<JsonElement>

    /**
     * Method for making DELETE calls. Uses callbacks. Deprecated in favor of the couroutine version
     * @param url       The URL for the call
     * @param headers   The header map
     */
    @Deprecated("Use suspendedDelete instead")
    @DELETE
    fun delete(@Url url: String, @HeaderMap headers: Map<String, String>): Call<JsonElement>

    /**
     * Method for making DELETE calls. Uses coroutines
     * @param url       The URL for the call
     * @param headers   The header map
     */
    @HTTP(method = "DELETE", hasBody = true)
    suspend fun suspendedDelete(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?): Response<JsonElement>

    /**
     * Method for making PUT calls. Uses callbacks.
     * @param url       The URL for the call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @Deprecated("Use suspendedPut instead")
    @PUT
    fun put(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?): Call<JsonElement>

    /**
     * Method for making PUT calls. Uses coroutines
     * @param url       The URL for the GET call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @PUT
    suspend fun suspendedPut(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?): Response<JsonElement>

    /**
     * Method for making PATCH calls. Uses callbacks.
     * @param url       The URL for the call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @Deprecated("Use suspendedPatch instead")
    @PATCH
    fun patch(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?): Call<JsonElement>

    /**
     * Method for making PATCH calls. Uses coroutines
     * @param url       The URL for the call
     * @param headers   The header map
     * @param requestBody The request body
     */
    @PATCH
    suspend fun suspendedPatch(@Url url: String, @HeaderMap headers: Map<String, String>, @Body requestBody: String?): Response<JsonElement>

}
