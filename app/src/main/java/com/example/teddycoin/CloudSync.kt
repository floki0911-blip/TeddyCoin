package com.example.teddycoin

/**
 * Backend contract for the future online version.
 *
 * The game intentionally keeps working offline.
 * Replace these methods with Firebase/Supabase/API calls when credentials
 * and a backend project are configured.
 */
data class CloudProfile(
    val userId: String,
    val displayName: String,
    val coins: Long,
    val level: Int,
    val taps: Int
)

interface CloudSync {
    suspend fun signIn(displayName: String): CloudProfile
    suspend fun loadProfile(userId: String): CloudProfile?
    suspend fun saveProfile(profile: CloudProfile)
    suspend fun loadLeaderboard(): List<CloudProfile>
}
