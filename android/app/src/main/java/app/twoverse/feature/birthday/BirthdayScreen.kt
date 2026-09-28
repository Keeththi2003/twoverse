package app.twoverse.feature.birthday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.OrbitRing
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import coil3.compose.AsyncImage

private val OrbitTopGap = 44.dp
private val OrbitSize = 240.dp
private val HerSize = 46.dp
private val YouSize = 32.dp
private val StarSize = 40.dp
private val OrbitToTextGap = 44.dp
private val TitleGap = 10.dp

/** Birthday welcome (FR-BDY-3, FR-BDY-4), with the partner's optional photo in place of the orbit. */
@Composable
fun BirthdayScreen(uiState: BirthdayUiState, onEnter: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        StarField(stars = BirthdayStars, twinkle = false, modifier = Modifier.fillMaxSize())
        if (uiState !is BirthdayUiState.Welcome) return@Box
        BoxWithConstraints(modifier = Modifier.safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontalWide)
                    .padding(bottom = spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.photoUrl != null) {
                        AsyncImage(
                            model = uiState.photoUrl,
                            contentDescription = stringResource(R.string.birthday_photo_from, uiState.fromName),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(top = OrbitTopGap)
                                .size(OrbitSize)
                                .clip(TwoverseTheme.shapes.cardLarge),
                        )
                    } else {
                        OrbitGraphic(
                            size = OrbitSize,
                            her = OrbitPosition(angleDegrees = 50f),
                            you = OrbitPosition(angleDegrees = 240f, ring = OrbitRing.Inner),
                            herSize = HerSize,
                            youSize = YouSize,
                            starSize = StarSize,
                            modifier = Modifier.padding(top = OrbitTopGap),
                        )
                    }
                    Text(
                        text = stringResource(R.string.birthday_for_you).uppercase(LocalConfiguration.current.locales[0]),
                        style = TwoverseTheme.textStyles.eyebrow,
                        color = colors.goldText,
                        modifier = Modifier.padding(top = OrbitToTextGap),
                    )
                    Text(
                        text = stringResource(R.string.birthday_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = TitleGap)
                            .semantics { heading() },
                    )
                    Text(
                        text = uiState.message,
                        style = TwoverseTheme.textStyles.birthdayMessage,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = spacing.md),
                    )
                    Text(
                        text = uiState.fromName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.smd),
                    )
                }
                TwoversePrimaryButton(
                    text = stringResource(R.string.birthday_enter),
                    onClick = onEnter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xl),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BirthdayScreenPreview() {
    TwoverseTheme {
        BirthdayScreen(
            uiState = BirthdayUiState.Welcome(
                message = "I built a little universe,\njust for the two of us.",
                fromName = "Your name",
            ),
            onEnter = {},
        )
    }
}
