package com.dicoding.skripsiapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,  // Mengatur suhu untuk pengacakan
    val top_p: Double = 1.0       // Mengatur probabilitas untuk pengambilan keputusan
)
data class Message(val role: String, val content: String)
data class OpenAiResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface OpenAiService {

    @Headers(
        "Content-Type: application/json"
    )
    @POST("v1/chat/completions")
    suspend fun getFunFact(
        @Body request: OpenAiRequest,
        @retrofit2.http.Header("Authorization") authHeader: String
    ): Response<OpenAiResponse>
}

