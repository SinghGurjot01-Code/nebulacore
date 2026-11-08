package com.nebula.app.di

import android.content.Context
import com.nebula.app.data.local.DataStoreKeyValueStore
import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.data.remote.RemoteAuthRepository
import com.nebula.app.data.remote.RemoteBookingRepository
import com.nebula.app.data.remote.RemoteGameRepository
import com.nebula.app.data.remote.RemoteSupportRepository
import com.nebula.app.data.remote.SupabaseClient
import com.nebula.app.data.remote.SupabaseConfig
import com.nebula.app.data.repositories.AndroidConnectivityChecker
import com.nebula.app.data.repositories.LocalAuthRepository
import com.nebula.app.data.repositories.LocalBookingRepository
import com.nebula.app.data.repositories.LocalGameRepository
import com.nebula.app.data.repositories.LocalProfileRepository
import com.nebula.app.data.repositories.LocalStreamingRepository
import com.nebula.app.data.repositories.LocalSupportRepository
import com.nebula.app.domain.repositories.AuthRepository
import com.nebula.app.domain.repositories.BookingRepository
import com.nebula.app.domain.repositories.GameRepository
import com.nebula.app.domain.repositories.ProfileRepository
import com.nebula.app.domain.repositories.StreamingRepository
import com.nebula.app.domain.repositories.SupportRepository

/**
 * Manual dependency container. Uses Supabase when nebula.supabaseUrl and nebula.supabaseAnonKey
 * are set in gradle.properties, otherwise the local demo repositories.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val store: KeyValueStore = DataStoreKeyValueStore(appContext)

    /** True when no Supabase project is configured and the app uses on-device demo data. */
    val isDemoMode: Boolean = !SupabaseConfig.isConfigured

    private val supabase: SupabaseClient? =
        if (isDemoMode) null else SupabaseClient(SupabaseConfig.url, SupabaseConfig.anonKey, store)

    val authRepository: AuthRepository =
        supabase?.let { RemoteAuthRepository(it, store) } ?: LocalAuthRepository(store)
    val gameRepository: GameRepository =
        supabase?.let { RemoteGameRepository(it) } ?: LocalGameRepository(store)
    val bookingRepository: BookingRepository =
        supabase?.let { RemoteBookingRepository(it) } ?: LocalBookingRepository()
    val supportRepository: SupportRepository =
        supabase?.let { RemoteSupportRepository(it) } ?: LocalSupportRepository()
    val profileRepository: ProfileRepository = LocalProfileRepository(store)
    val streamingRepository: StreamingRepository =
        LocalStreamingRepository(AndroidConnectivityChecker(appContext))
}
