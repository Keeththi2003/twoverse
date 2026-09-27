package app.twoverse.core.data.supabase.di

import org.junit.Assert.assertEquals
import org.junit.Test

class SupabaseModuleTest {

    @Test
    fun projectUrlIsUsedAsIs() {
        assertEquals("https://abc.supabase.co", SupabaseModule.projectUrl("https://abc.supabase.co"))
    }

    @Test
    fun restEndpointAndTrailingSlashesAreRemoved() {
        assertEquals("https://abc.supabase.co", SupabaseModule.projectUrl("https://abc.supabase.co/rest/v1/"))
        assertEquals("https://abc.supabase.co", SupabaseModule.projectUrl(" https://abc.supabase.co/ "))
    }
}
