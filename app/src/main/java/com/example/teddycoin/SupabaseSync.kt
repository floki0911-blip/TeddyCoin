package com.example.teddycoin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OnlineProfile(
    val userId: String,
    val displayName: String,
    val coins: Long,
    val level: Int,
    val taps: Int
)

class SupabaseSync {
    private fun request(
        path: String,
        method: String,
        body: String? = null,
        query: String = ""
    ): String = run {
        val base = SupabaseConfig.URL.trimEnd('/')
        val conn = URL("$base/rest/v1/$path$query").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("apikey", SupabaseConfig.ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) error("HTTP $code: $text")
        text
    }

    suspend fun upsert(profile: OnlineProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject()
                .put("user_id", profile.userId)
                .put("display_name", profile.displayName)
                .put("coins", profile.coins)
                .put("level", profile.level)
                .put("taps", profile.taps)
            request("players", "POST", json.toString(), "?on_conflict=user_id")
        }
    }

    suspend fun leaderboard(limit: Int = 50): Result<List<OnlineProfile>> = withContext(Dispatchers.IO) {
        runCatching {
            val text = request(
                "players",
                "GET",
                query = "?select=user_id,display_name,coins,level,taps&order=coins.desc&limit=$limit"
            )
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        OnlineProfile(
                            o.optString("user_id"),
                            o.optString("display_name"),
                            o.optLong("coins"),
                            o.optInt("level"),
                            o.optInt("taps")
                        )
                    )
                }
            }
        }
    }
}
