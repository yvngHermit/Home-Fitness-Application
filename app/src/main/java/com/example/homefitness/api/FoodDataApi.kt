package com.example.homefitness.api

import com.example.homefitness.models.FoodSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FoodDataApi {
    @GET("fdc/v1/foods/search")
    suspend fun searchFoods(@Query("query") query: String, @Query("api_key") apiKey: String, @Query("pageSize") pageSize: Int = 20): Response<FoodSearchResponse>
}
