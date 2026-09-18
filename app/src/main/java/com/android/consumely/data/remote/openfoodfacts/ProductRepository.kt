package com.android.consumely.data.remote.openfoodfacts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ProductRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(OpenFoodFactsApi::class.java)

    suspend fun fetchProductName(barcode: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getProductByBarcode(barcode)
            if (response.isSuccessful) {
                val body = response.body()
                val product = body?.product
                if (body?.status == 1 && product != null) {
                    val name = product.productNameDe
                        ?: product.productNameEn
                        ?: product.productName
                    val brand = product.brands
                    if (!name.isNullOrBlank()) {
                        if (!brand.isNullOrBlank()) "$brand $name" else name
                    } else {
                        error("Product name not found for barcode.")
                    }
                } else {
                    error("Product not found.")
                }
            } else {
                error("OpenFoodFacts request failed: ${response.code()}")
            }
        }
    }
}
