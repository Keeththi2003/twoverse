package app.twoverse.core.data.push

import app.twoverse.core.model.LaunchScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingPushTest {

    @Test
    fun typesFromSendPushAreRecognised() {
        assertEquals(IncomingPush.WakeUp, IncomingPush.fromType("wake_up"))
        assertEquals(IncomingPush.PartnerJoined, IncomingPush.fromType("partner_joined"))
        assertEquals(IncomingPush.ReunionDay, IncomingPush.fromType("reunion_day"))
        assertEquals(IncomingPush.NewMemory, IncomingPush.fromType("new_memory"))
        assertEquals(IncomingPush.ShootingStar, IncomingPush.fromType("shooting_star"))
    }

    @Test
    fun unknownOrMissingTypesAreIgnored() {
        assertNull(IncomingPush.fromType("something_else"))
        assertNull(IncomingPush.fromType(null))
    }

    @Test
    fun theWakeUpIsSilentAndTheOthersOpenTheRightScreen() {
        assertNull(IncomingPush.WakeUp.opens)
        assertEquals(LaunchScreen.Home, IncomingPush.PartnerJoined.opens)
        assertEquals(LaunchScreen.Countdown, IncomingPush.ReunionDay.opens)
        assertEquals(LaunchScreen.Vault, IncomingPush.NewMemory.opens)
        assertEquals(LaunchScreen.ShootingStar, IncomingPush.ShootingStar.opens)
    }
}
