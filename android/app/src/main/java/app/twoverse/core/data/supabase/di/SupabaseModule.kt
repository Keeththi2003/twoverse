package app.twoverse.core.data.supabase.di

import app.twoverse.BuildConfig
import app.twoverse.core.data.supabase.AuthCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SupabaseModule {

    /**
     * The only Supabase client. It uses the publishable key from local.properties; secret keys
     * never ship in the app. OkHttp is used because Realtime needs WebSockets.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        check(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in android/local.properties"
        }
        return createSupabaseClient(projectUrl(BuildConfig.SUPABASE_URL), BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            httpEngine = OkHttp.create()
            install(Auth) {
                scheme = AuthCallback.Scheme
                host = AuthCallback.Host
                flowType = FlowType.PKCE
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    /**
     * The client needs the bare project URL; accept the REST endpoint that the dashboard
     * also shows ("…/rest/v1") so a copied value doesn't crash the app.
     */
    internal fun projectUrl(configured: String): String =
        configured.trim().trimEnd('/').removeSuffix("/rest/v1").trimEnd('/')
}
