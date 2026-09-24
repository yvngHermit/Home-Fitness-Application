package com.example.homefitness.api

import com.example.homefitness.models.WgerExerciseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WgerApiService {

    @GET("api/v2/exerciseinfo/")
    suspend fun getExercises(
        @Query("language") language: Int? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("category") categoryId: Int? = null
    ): Response<WgerExerciseResponse>
}