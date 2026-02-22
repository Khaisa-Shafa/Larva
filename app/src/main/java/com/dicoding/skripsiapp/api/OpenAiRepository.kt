package com.dicoding.skripsiapp.api

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenAiRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(OpenAiService::class.java)

    suspend fun fetchFunFact(prompt: String): String {
        val messages = listOf(Message("user", prompt))
        val request = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = messages,
            temperature = 0.7,
            top_p = 1.0
        )

        val response = service.getFunFact(request)

        return if (response.isSuccessful) {
            response.body()?.choices?.firstOrNull()?.message?.content ?: "No fun fact available."
        } else {
            Log.e("API_ERROR", "Code: ${response.code()}, Message: ${response.errorBody()?.string()}")
            "Failed to fetch fun fact."
        }
    }
}