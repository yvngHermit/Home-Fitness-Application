package com.example.homefitness.api

import com.example.homefitness.models.WgerExerciseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface WgerApiService {

    @GET("api/v2/exerciseinfo/")
    suspend fun getExercises(
        @Query("language") language: Int? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("category") categoryId: Int? = null
    ): Response<WgerExerciseResponse>

    @GET("api/v2/exerciseinfo/{id}/")
    suspend fun getExerciseById(@Path("id") id: Int): Response<com.example.homefitness.models.WgerExercise>
}