package com.example.teddycoin

object SupabaseConfig {
    const val URL = "https://pcklvdcihyqweytfnnbr.supabase.co"

    // Publishable/anon key provided for this TeddyCoin project.
    const val ANON_KEY = "sb_publishable_pRonuzDyXrOvfAMqxZrAbA_thjXFm7V"

    val isConfigured: Boolean
        get() = URL.isNotBlank() && ANON_KEY.isNotBlank()
}
