package app.twoverse.core.designsystem.text

import app.twoverse.R
import app.twoverse.core.model.DataError
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTextTest {

    @Test
    fun srsSectionSevenMessagesAreUsed() {
        assertEquals(R.string.pair_invalid_code, DataError.InvalidCoupleCode.messageRes())
        assertEquals(R.string.offline_banner, DataError.Network.messageRes())
    }

    @Test
    fun everyErrorHasItsOwnMessage() {
        val messages = DataError.entries.map { it.messageRes() }
        assertEquals(DataError.entries.size, messages.toSet().size)
    }
}
