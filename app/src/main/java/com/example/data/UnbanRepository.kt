package com.example.data

import kotlinx.coroutines.flow.Flow

class UnbanRepository(private val dao: UnbanRequestDao) {
    val allRequests: Flow<List<UnbanRequest>> = dao.getAllRequests()

    suspend fun insert(request: UnbanRequest): Long = dao.insertRequest(request)

    suspend fun update(request: UnbanRequest) = dao.updateRequest(request)

    suspend fun delete(request: UnbanRequest) = dao.deleteRequest(request)

    suspend fun deleteById(id: Int) = dao.deleteById(id)

    suspend fun getById(id: Int): UnbanRequest? = dao.getRequestById(id)
}
