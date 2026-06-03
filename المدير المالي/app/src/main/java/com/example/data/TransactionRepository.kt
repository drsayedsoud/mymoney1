package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAll() {
        transactionDao.clearAllTransactions()
    }
}
