package com.gal101.foodexpirytracker.data.model

import com.google.gson.annotations.SerializedName

data class ProductResponse(
        @SerializedName("code") val code: String?,
        @SerializedName("product") val product: Product?,
        @SerializedName("status") val status: Int,
        @SerializedName("status_verbose") val statusVerbose: String?
)

data class Product(
        @SerializedName("product_name") val productName: String?,
        @SerializedName("product_name_en") val productNameEn: String?,
        @SerializedName("product_name_ro") val productNameRo: String?, // Romanian support
        @SerializedName("brands") val brands: String?,
        @SerializedName("generic_name") val genericName: String?,
        @SerializedName("generic_name_ro") val genericNameRo: String?,
        @SerializedName("quantity") val quantity: String?, // e.g. "1 L"
        @SerializedName("product_quantity") val productQuantity: Double?, // e.g. 1.0
        @SerializedName("product_quantity_unit") val productQuantityUnit: String?, // e.g. "l"
        @SerializedName("image_url") val imageUrl: String?
)
