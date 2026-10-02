package app.twoverse.feature.star

import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.component.Star

/** Star layout from Birthday.dc.html: (top %, left %, size px), every third star gold. */
internal val ShootingStarBackdrop: List<Star> = listOf(
    Triple(6, 12, 3), Triple(9, 80, 2), Triple(14, 30, 2), Triple(18, 90, 3),
    Triple(5, 55, 2), Triple(40, 6, 2), Triple(44, 92, 3), Triple(52, 14, 3),
    Triple(58, 84, 2), Triple(66, 4, 2), Triple(70, 94, 2), Triple(78, 10, 3),
    Triple(83, 88, 2), Triple(26, 4, 2), Triple(30, 70, 2), Triple(62, 50, 2),
).mapIndexed { index, (top, left, size) ->
    Star(x = left / 100f, y = top / 100f, size = size.dp, gold = index % 3 == 0)
}
