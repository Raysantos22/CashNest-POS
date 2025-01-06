package com.example.possystembw.Repository


import android.util.Log
import com.example.possystembw.DAO.StaffApi
import com.example.possystembw.DAO.StaffDao
import com.example.possystembw.database.StaffEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.net.SocketTimeoutException

class StaffRepository(
    private val staffApi: StaffApi,
    private val staffDao: StaffDao
) {
    suspend fun fetchAndStoreStaff(storeId: String): Result<List<StaffEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = staffApi.getStaffData(storeId)

            if (response.isSuccessful && response.body() != null) {
                val existingStaff = staffDao.getStaffByStore(storeId)
                val existingStaffMap = existingStaff.associateBy { "${it.name}_${it.storeId}" }

                val staffList = response.body()!!.map { staff ->
                    val staffId = "${staff.name ?: ""}_${staff.storeid ?: storeId}"
                    val existingStaffEntity = existingStaffMap[staffId]

                    StaffEntity(
                        id = staffId,
                        name = staff.name ?: "",
                        passcode = staff.passcode ?: "",
                        // Preserve existing image if new image is null
                        image = staff.image ?: existingStaffEntity?.image,
                        role = staff.role ?: "",
                        storeId = staff.storeid ?: storeId
                    )
                }

                if (staffList.isNotEmpty()) {
                    staffDao.refreshStaffData(storeId, staffList)
                }

                Result.success(staffList)
            } else {
                Result.failure(Exception("Failed to fetch staff data: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("StaffRepository", "Error fetching staff: ${e.message}")
            Result.failure(e)
        }
    }
}