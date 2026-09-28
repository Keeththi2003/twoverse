package app.twoverse.feature.star

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.OrbitRing
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoFit
import coil3.compose.AsyncImage

private val PhotoTopGap = 44.dp
private val PhotoSize = 240.dp
private val HerSize = 46.dp
private val YouSize = 32.dp
private val OrbitStarSize = 40.dp
private val DecorationSize = 150.dp
private val DecorationHerSize = 30.dp
private val DecorationYouSize = 22.dp
private val DecorationStarSize = 28.dp
private val PhotoToTextGap = 44.dp
private val TitleGap = 10.dp
private val ScrimTopPadding = 72.dp
private const val OverlayMaxHeightFraction = 0.4f

/**
 * What a Shooting Star shows. [content] is normalized, so empty fields are null; [photo] is a
 * signed URL, or the picked photo in the composer's preview.
 */
data class StarDisplay(val content: StarContent, val photo: String?)

/**
 * A Shooting Star as the partner sees it (FR-STAR-2, FR-STAR-10, FR-STAR-11). Empty fields are
 * left out and the gaps close; long text scrolls while [enterLabel] stays pinned at the bottom.
 */
@Composable
internal fun ShootingStarContent(
    display: StarDisplay,
    enterLabel: String,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (display.content.layout) {
        StarLayout.PhotoMessage -> PhotoMessageStar(display, enterLabel, onEnter, modifier)
        StarLayout.MessageOnly -> MessageOnlyStar(display.content, enterLabel, onEnter, modifier)
        StarLayout.FullPhoto -> FullPhotoStar(display, enterLabel, onEnter, modifier)
    }
}

@Composable
private fun PhotoMessageStar(display: StarDisplay, enterLabel: String, onEnter: () -> Unit, modifier: Modifier) {
    StarFrame(enterLabel = enterLabel, onEnter = onEnter, centered = false, modifier = modifier) {
        if (display.photo != null) {
            AsyncImage(
                model = display.photo,
                contentDescription = stringResource(R.string.star_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = PhotoTopGap)
                    .size(PhotoSize)
                    .clip(TwoverseTheme.shapes.cardLarge),
            )
        } else {
            OrbitGraphic(
                size = PhotoSize,
                her = OrbitPosition(angleDegrees = 50f),
                you = OrbitPosition(angleDegrees = 240f, ring = OrbitRing.Inner),
                herSize = HerSize,
                youSize = YouSize,
                starSize = OrbitStarSize,
                modifier = Modifier.padding(top = PhotoTopGap),
            )
        }
        StarTexts(
            content = display.content,
            firstGap = PhotoToTextGap,
            titleStyle = MaterialTheme.typography.displaySmall,
            messageStyle = TwoverseTheme.textStyles.starMessage,
        )
    }
}

@Composable
private fun MessageOnlyStar(content: StarContent, enterLabel: String, onEnter: () -> Unit, modifier: Modifier) {
    StarFrame(enterLabel = enterLabel, onEnter = onEnter, centered = true, modifier = modifier) {
        OrbitGraphic(
            size = DecorationSize,
            her = OrbitPosition(angleDegrees = 50f),
            you = OrbitPosition(angleDegrees = 240f, ring = OrbitRing.Inner),
            herSize = DecorationHerSize,
            youSize = DecorationYouSize,
            starSize = DecorationStarSize,
            modifier = Modifier.padding(top = TwoverseTheme.spacing.xl),
        )
        StarTexts(
            content = content,
            firstGap = TwoverseTheme.spacing.xxl,
            titleStyle = TwoverseTheme.textStyles.starTitleLarge,
            messageStyle = TwoverseTheme.textStyles.starMessageLarge,
            modifier = Modifier.padding(bottom = TwoverseTheme.spacing.xl),
        )
    }
}

/** Star backdrop, scrolling content (centred when short) and the pinned Enter button. */
@Composable
private fun StarFrame(
    enterLabel: String,
    onEnter: () -> Unit,
    centered: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = TwoverseTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TwoverseTheme.colors.background),
    ) {
        StarField(stars = ShootingStarBackdrop, twinkle = false, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = spacing.screenHorizontalWide)
                .padding(bottom = spacing.lg),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top,
                    content = content,
                )
            }
            TwoversePrimaryButton(
                text = enterLabel,
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.md),
            )
        }
    }
}

/** Eyebrow, title, message and signature; only the ones that are set, with gaps only between them. */
@Composable
private fun StarTexts(
    content: StarContent,
    firstGap: Dp,
    titleStyle: TextStyle,
    messageStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val locale = LocalConfiguration.current.locales[0]
    var isFirst = true
    fun gap(between: Dp): Dp = if (isFirst) firstGap.also { isFirst = false } else between

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        content.eyebrow?.let {
            Text(
                text = it.uppercase(locale),
                style = TwoverseTheme.textStyles.eyebrow,
                color = colors.goldText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = gap(0.dp)),
            )
        }
        content.title?.let {
            Text(
                text = it,
                style = titleStyle,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = gap(TitleGap))
                    .semantics { heading() },
            )
        }
        content.message?.let {
            Text(
                text = it,
                style = messageStyle,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = gap(spacing.md)),
            )
        }
        content.signature?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = gap(spacing.smd)),
            )
        }
    }
}

/**
 * The photo fills the screen edge to edge, behind the system bars, and can be pinched to zoom.
 * A gradient scrim keeps the optional text and the Enter button readable on any photo.
 */
@Composable
private fun FullPhotoStar(display: StarDisplay, enterLabel: String, onEnter: () -> Unit, modifier: Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val content = display.content
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        val overlayMaxHeight = maxHeight * OverlayMaxHeightFraction
        ZoomablePhoto(
            model = display.photo,
            contentScale = if (content.photoFit == StarPhotoFit.Fit) ContentScale.Fit else ContentScale.Crop,
            contentDescription = stringResource(R.string.star_photo_description),
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.photoScrim.copy(alpha = 0f), colors.photoScrim)))
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenHorizontalWide)
                .padding(top = ScrimTopPadding, bottom = spacing.lg),
        ) {
            if (content.title != null || content.message != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = overlayMaxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    content.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onPhotoScrim,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    content.message?.let {
                        Text(text = it, style = TwoverseTheme.textStyles.starMessage, color = colors.onPhotoScrim)
                    }
                }
            }
            TwoversePrimaryButton(text = enterLabel, onClick = onEnter, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val PreviewMessage = "I built a little universe,\njust for the two of us."

@PreviewLightDark
@Composable
private fun PhotoMessageStarPreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(
                content = StarContent(
                    layout = StarLayout.PhotoMessage,
                    eyebrow = "For you",
                    title = "Happy Birthday",
                    message = PreviewMessage,
                    signature = "Keeththi",
                ),
                photo = null,
            ),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoMessageStarOnlyMessagePreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(StarContent(layout = StarLayout.PhotoMessage, message = PreviewMessage), photo = null),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MessageOnlyStarPreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(
                content = StarContent(
                    layout = StarLayout.MessageOnly,
                    eyebrow = "Right now",
                    title = "Thinking of you",
                    message = PreviewMessage,
                    signature = "Keeththi",
                ),
                photo = null,
            ),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MessageOnlyStarLongPreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(
                content = StarContent(
                    layout = StarLayout.MessageOnly,
                    title = "Good luck",
                    message = List(12) { PreviewMessage }.joinToString("\n\n"),
                ),
                photo = null,
            ),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FullPhotoStarPreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(
                content = StarContent(layout = StarLayout.FullPhoto, title = "Happy Anniversary", message = PreviewMessage),
                photo = null,
            ),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FullPhotoStarNoTextPreview() {
    TwoverseTheme {
        ShootingStarContent(
            display = StarDisplay(StarContent(layout = StarLayout.FullPhoto), photo = null),
            enterLabel = stringResource(R.string.star_enter),
            onEnter = {},
        )
    }
}
