package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSizingTest {

    @Test
    fun landscapePhotoIsScaledToTheLongEdge() {
        assertEquals(PixelSize(1080, 810), scaledToLongEdge(4000, 3000))
    }

    @Test
    fun portraitPhotoIsScaledToTheLongEdge() {
        assertEquals(PixelSize(608, 1080), scaledToLongEdge(3024, 5376))
    }

    @Test
    fun squarePhotoIsScaledOnBothSides() {
        assertEquals(PixelSize(1080, 1080), scaledToLongEdge(2048, 2048))
    }

    @Test
    fun smallPhotosAreNeverEnlarged() {
        assertEquals(PixelSize(800, 600), scaledToLongEdge(800, 600))
        assertEquals(PixelSize(1080, 720), scaledToLongEdge(1080, 720))
    }

    @Test
    fun veryThinPanoramaKeepsAtLeastOnePixel() {
        assertEquals(PixelSize(1080, 1), scaledToLongEdge(20_000, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPhotoIsRejected() {
        scaledToLongEdge(0, 100)
    }
}
