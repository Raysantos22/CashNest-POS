package com.example.possystembw.data

import com.example.possystembw.DAO.CashfundDao
import com.example.possystembw.database.Cashfund

class CashFundRepository(private val cashFundDao: CashfundDao) {
    suspend fun getCashFundForDate(date: String): Cashfund? {
        return cashFundDao.getCashFundByDate(date)
    }    suspend fun insert(cashFund: Cashfund) {
        cashFundDao.insert(cashFund) // Use the instance variable
    }

    suspend fun deleteAll() {
        cashFundDao.deleteAll()
    }

    suspend fun deleteByDate(date: String) {
        cashFundDao.deleteByDate(date)
    }
    suspend fun getCashFundByDate(date: String): Cashfund? {
        return cashFundDao.getCashFundByDate(date) // Use the instance variable
    }
}
