package com.dicoding.skripsiapp.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStreamReader

object JsonUtils {
    fun <T> readJsonFromAssets(context: Context, fileName: String, clazz: Class<T>): T? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val reader = InputStreamReader(inputStream)
                Gson().fromJson(reader, clazz)
            }
        } catch (e: IOException) {
            Log.e("JsonUtils", "Error reading JSON file: ${e.message}", e)
            null
        }
    }
}