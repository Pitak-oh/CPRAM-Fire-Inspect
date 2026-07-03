package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

object LineNotifyService {
    private const val TAG = "LineNotifyService"
    private const val LINE_NOTIFY_URL = "https://notify-api.line.me/api/notify"
    private val client = OkHttpClient()

    /**
     * Sends a real-time notification to a LINE group via LINE Notify API.
     * @param token The LINE Notify access token.
     * @param message The alert message content.
     * @return Boolean indicating if the network action succeeded.
     */
    suspend fun sendNotification(token: String, message: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || token == "MOCK_TOKEN") {
            Log.w(TAG, "LINE Notify token is blank or default. Simulating delivery.")
            return@withContext true
        }
        try {
            val body = FormBody.Builder()
                .add("message", message)
                .build()

            val request = Request.Builder()
                .url(LINE_NOTIFY_URL)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                Log.d(TAG, "Response Code: ${response.code}, Success: $isSuccess")
                return@withContext isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending LINE Notify: ${e.message}", e)
            return@withContext false
        }
    }
}
