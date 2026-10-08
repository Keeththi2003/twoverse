package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {

    @Test
    fun internationalNumbersKeepTheirCountryCode() {
        assertEquals("+94771234567", normalizePhone("+94 77 123 4567"))
        assertEquals("+447700900123", normalizePhone("+44 (7700) 900-123"))
        assertEquals("+447700900123", normalizePhone("0044 7700 900123"))
    }

    @Test
    fun localNumbersGetTheDefaultCountryCode() {
        assertEquals("+94771234567", normalizePhone("077 123 4567"))
        assertEquals("+94771234567", normalizePhone("771234567"))
        assertEquals("+61412345678", normalizePhone("0412 345 678", countryCode = "61"))
    }

    @Test
    fun invalidNumbersAreRejected() {
        assertNull(normalizePhone(""))
        assertNull(normalizePhone("   "))
        assertNull(normalizePhone("0771"))
        assertNull(normalizePhone("+0771234567"))
        assertNull(normalizePhone("+94 77 abc 4567"))
        assertNull(normalizePhone("+1234567890123456"))
    }
}
