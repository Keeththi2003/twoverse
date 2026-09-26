package app.twoverse.feature.splash

import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.component.Star

/** Star layout from SplashDark.dc.html: (top %, left %, size px), every fourth star gold. */
internal val SplashDarkStars: List<Star> = listOf(
    Triple(6, 10, 2), Triple(9, 70, 3), Triple(14, 42, 2), Triple(18, 86, 2),
    Triple(22, 22, 3), Triple(27, 60, 2), Triple(31, 5, 2), Triple(35, 93, 3),
    Triple(62, 12, 2), Triple(66, 80, 3), Triple(70, 34, 2), Triple(74, 58, 2),
    Triple(79, 90, 2), Triple(83, 18, 3), Triple(87, 66, 2), Triple(91, 44, 2),
    Triple(95, 8, 2), Triple(4, 54, 2), Triple(12, 28, 2), Triple(58, 95, 2),
    Triple(76, 4, 2), Triple(24, 74, 2),
).mapIndexed { index, (top, left, size) ->
    Star(x = left / 100f, y = top / 100f, size = size.dp, gold = index % 4 == 0)
}
