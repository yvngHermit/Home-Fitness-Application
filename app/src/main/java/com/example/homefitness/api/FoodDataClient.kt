package com.example.homefitness.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FoodDataClient {
    val api: FoodDataApi by lazy {
        Retrofit.Builder().baseUrl("https://api.nal.usda.gov/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(FoodDataApi::class.java)
    }
}
