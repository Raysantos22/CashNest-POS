package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.StaffEntity

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE storeId = :storeId")
    suspend fun getStaffByStore(storeId: String): List<StaffEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staff: List<StaffEntity>)

    @Query("SELECT * FROM staff WHERE storeId = :storeId AND passcode = :passcode LIMIT 1")
    suspend fun getStaffByStoreAndPasscode(storeId: String, passcode: String): StaffEntity?

    @Query("DELETE FROM staff WHERE storeId = :storeId")
    suspend fun deleteByStore(storeId: String)

    @Query("DELETE FROM staff")
    suspend fun deleteAll()

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("SELECT * FROM staff WHERE id = :staffId")
    suspend fun getStaffById(staffId: String): StaffEntity?



    @Transaction
    suspend fun refreshStaffData(storeId: String, newStaff: List<StaffEntity>) {
        deleteByStore(storeId)
        insertAll(newStaff)
    }
}