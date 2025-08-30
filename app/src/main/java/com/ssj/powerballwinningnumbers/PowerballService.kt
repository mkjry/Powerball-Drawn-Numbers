package com.ssj.powerballwinningnumbers

import retrofit2.Response
import retrofit2.http.GET

interface PowerballService {
    @GET("previous-results")
    suspend fun getPreviousResults(): Response<String>
}
