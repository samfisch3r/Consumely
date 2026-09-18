package com.android.consumely.data.remote.openfoodfacts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class OpenFoodFactsResponse(
    val status: Int = 0,
    val product: ProductDto? = null
)

@Serializable
data class ProductDto(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_de") val productNameDe: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    @SerialName("brands") val brands: String? = null,
    @SerialName("quantity") val quantity: String? = null
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>
}
