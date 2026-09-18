package com.android.consumely.data.remote.jsonbin

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface JsonBinApi {
    @GET("v3/b/{binId}/latest")
    suspend fun getBin(
        @Path("binId") binId: String,
        @Header("X-Access-Key") apiKey: String
    ): Response<JsonBinRawResponse>

    @PUT("v3/b/{binId}")
    suspend fun updateBin(
        @Path("binId") binId: String,
        @Header("X-Access-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body payload: SyncPayload
    ): Response<JsonBinRawResponse>
}
