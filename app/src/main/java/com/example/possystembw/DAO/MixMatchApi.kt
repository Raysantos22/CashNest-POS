package com.example.possystembw.DAO

import retrofit2.http.GET

interface MixMatchApi {
    @GET("mixmatch")
    suspend fun getMixMatches(): List<MixMatchApiResponse>
}
data class MixMatchApiResponse(
    val id: String,
    val name: String = "", // Added name field with default value
    val description: String,
    val discounttype: Int,
    val dealpricevalue: Double,
    val discountpctvalue: Double,
    val discountamountvalue: Double,
    val line_groups: List<LineGroupApiResponse>
)

data class LineGroupApiResponse(
    val linegroup: String,
    val name: String = "", // Added name field with default value
    val description: String,
    val noofitemsneeded: Int,
    val discount_lines: List<DiscountLineApiResponse>
)

data class DiscountLineApiResponse(
    val id: Int,
    val itemid: String,
    val name: String = "", // Added name field with default value
    val disctype: Int,
    val dealpriceordiscpct: Double,
    val linegroup: String,
    val qty: Int,
    val itemData: ItemDataApiResponse
)

data class ItemDataApiResponse(
    val itemid: String,
    val name: String = "", // Added name field with default value
    val Activeondelivery: Int,
    val itemname: String,
    val itemgroup: String,
    val specialgroup: String,
    val production: String,
    val moq: Int,
    val price: Double,
    val cost: Double,
    val barcode: String
)