package com.example.possystembw.DAO

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LineDetailsApi {
    @GET("api/line/{storeId}/{journalId}")
    suspend fun getLineDetails(
        @Path("storeId") storeId: String,
        @Path("journalId") journalId: String
    ): Response<LineDetailsResponse>

    @POST("api/line/{itemId}/{storeId}/{journalId}/{adjustment}/{receivedCount}/{transferCount}/{wasteCount}/{wasteType}/{counted}")
    suspend fun postLineDetails(
        @Path("itemId") itemId: String,
        @Path("storeId") storeId: String,
        @Path("journalId") journalId: String,
        @Path("adjustment") adjustment: String,
        @Path("receivedCount") receivedCount: String,
        @Path("transferCount") transferCount: String,
        @Path("wasteCount") wasteCount: String,
        @Path("wasteType") wasteType: String,
        @Path("counted") counted: String,
        @Query("_method") method: String = "POST"  // Add this for compatibility
    ): Response<Unit>

}

// Response Data Classes
data class LineTransaction(
    @SerializedName("JOURNALID") val journalId: String? = null,
    @SerializedName("LINENUM") val lineNum: Int? = null,
    @SerializedName("TRANSDATE") val transDate: String? = null,
    @SerializedName("ITEMID") val itemId: String? = null,
    @SerializedName("ITEMDEPARTMENT") val itemDepartment: String? = null,
    @SerializedName("STORENAME") val storeName: String? = null,
    @SerializedName("ADJUSTMENT") val adjustment: String? = null,
    @SerializedName("COSTPRICE") val costPrice: String? = null,
    @SerializedName("PRICEUNIT") val priceUnit: String? = null,
    @SerializedName("SALESAMOUNT") val salesAmount: String? = null,
    @SerializedName("INVENTONHAND") val inventOnHand: String? = null,
    @SerializedName("COUNTED") val counted: String? = null,
    @SerializedName("REASONREFRECID") val reasonRefRecId: String? = null,
    @SerializedName("VARIANTID") val variantId: String? = null,
    @SerializedName("POSTED") val posted: Int? = null,
    @SerializedName("POSTEDDATETIME") val postedDateTime: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("WASTECOUNT") val wasteCount: String? = null,
    @SerializedName("RECEIVEDCOUNT") val receivedCount: String? = null,
    @SerializedName("WASTETYPE") val wasteType: String? = null,
    @SerializedName("TRANSFERCOUNT") val transferCount: String? = null,
    @SerializedName("WASTEDATE") val wasteDate: String? = null,
    @SerializedName("itemgroupid") val itemGroupId: String? = null,
    @SerializedName("itemid") val itemIdLower: String? = null,
    @SerializedName("itemname") val itemName: String? = null,
    @SerializedName("itemtype") val itemType: Int? = null,
    @SerializedName("namealias") val nameAlias: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("itemgroup") val itemGroup: String? = null,
    @SerializedName("itemdepartment") val itemDepartmentLower: String? = null,
    @SerializedName("zeropricevalid") val zeroPriceValid: Int? = null,
    @SerializedName("dateblocked") val dateBlocked: String? = null,
    @SerializedName("datetobeblocked") val dateToBeBlocked: String? = null,
    @SerializedName("blockedonpos") val blockedOnPos: Int? = null,
    @SerializedName("Activeondelivery") val activeOnDelivery: Int? = null,
    @SerializedName("barcode") val barcode: String? = null,
    @SerializedName("datetoactivateitem") val dateToActivateItem: String? = null,
    @SerializedName("mustselectuom") val mustSelectUom: Int? = null,
    @SerializedName("PRODUCTION") val production: String? = null,
    @SerializedName("moq") val moq: Int? = null,
    @SerializedName("fgcount") val fgCount: String? = null,
    @SerializedName("TRANSPARENTSTOCKS") val transparentStocks: String? = null,
    @SerializedName("stocks") val stocks: String? = null,
    @SerializedName("posted") val postedLower: Int? = null,
    @SerializedName("sync_status") val syncStatus: Int = 1,  // Default to 1 (synced)
    val isModified: Boolean = false  //


) {
    fun hasModifications(): Boolean {
        return syncStatus == 0
    }

    fun hasAnyValue(): Boolean {
        val adjustmentValue = adjustment?.toDoubleOrNull() ?: 0.0
        val receivedValue = receivedCount?.toDoubleOrNull() ?: 0.0
        val transferValue = transferCount?.toDoubleOrNull() ?: 0.0
        val wasteValue = wasteCount?.toDoubleOrNull() ?: 0.0
        val countedValue = counted?.toDoubleOrNull() ?: 0.0

        return adjustmentValue > 0 ||
                receivedValue > 0 ||
                transferValue > 0 ||
                wasteValue > 0 ||
                countedValue > 0 ||
                (!wasteType.isNullOrBlank() && wasteType != "none" && wasteType != "Select type")
    }

    // Add this method to check if item has meaningful changes for syncing
    fun hasSignificantChanges(): Boolean {
        val adjustmentValue = adjustment?.toDoubleOrNull() ?: 0.0
        val receivedValue = receivedCount?.toDoubleOrNull() ?: 0.0
        val transferValue = transferCount?.toDoubleOrNull() ?: 0.0
        val wasteValue = wasteCount?.toDoubleOrNull() ?: 0.0
        val countedValue = counted?.toDoubleOrNull() ?: 0.0

        // Consider it significant if there are any non-zero values
        return adjustmentValue != receivedValue || // Variance exists
                wasteValue > 0 ||                    // Has waste
                transferValue > 0 ||                 // Has transfer
                countedValue > 0 ||                  // Has manual count
                (!wasteType.isNullOrBlank() && wasteType != "none")
    }
}
data class LineDetailsResponse(
    @SerializedName("journalid") val journalId: String? = null,
    @SerializedName("stockcountingtrans") val transactions: List<LineTransaction> = emptyList()
)

data class LineDetailsData(
    val transactions: List<LineTransaction> = emptyList()
)
