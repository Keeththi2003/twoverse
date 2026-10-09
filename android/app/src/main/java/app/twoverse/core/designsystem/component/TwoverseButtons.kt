package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val RegularButtonHeight = 56.dp
private val SmallButtonHeight = 52.dp
private val ButtonIconSize = 20.dp
private val AccentBorderWidth = 1.5.dp
private val ButtonContentPadding = PaddingValues(horizontal = 24.dp)
private val AdaptiveButtonContentPadding = PaddingValues(horizontal = 12.dp)

/**
 * Filled primary button. An [adaptive] button uses compact padding and a smaller label so two
 * fit side by side; when the label still does not fit on one line, the icon moves above it
 * instead of truncating. [contentDescription] replaces the label for screen readers.
 */
@Composable
fun TwoversePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    adaptive: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = TwoverseTheme.colors
    FilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        small = small,
        enabled = enabled,
        adaptive = adaptive,
        contentDescription = contentDescription,
        buttonColors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.chip,
            disabledContentColor = colors.onSurfaceVariant,
        ),
    )
}

/**
 * Tonal button: chip background with primary-coloured text and icon. See [TwoversePrimaryButton]
 * for [adaptive] and [contentDescription].
 */
@Composable
fun TwoverseTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    adaptive: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = TwoverseTheme.colors
    FilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        small = small,
        enabled = enabled,
        adaptive = adaptive,
        contentDescription = contentDescription,
        buttonColors = ButtonDefaults.buttonColors(
            containerColor = colors.chip,
            contentColor = colors.primary,
            disabledContainerColor = colors.chip,
            disabledContentColor = colors.onSurfaceVariant,
        ),
    )
}

@Composable
fun TwoverseSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = TwoverseTheme.colors
    FilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        small = small,
        enabled = enabled,
        adaptive = false,
        contentDescription = null,
        buttonColors = ButtonDefaults.buttonColors(
            containerColor = colors.chip,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.chip,
            disabledContentColor = colors.onSurfaceVariant,
        ),
    )
}

/**
 * Outlined button. Neutral (surface with a 1dp outline, e.g. "Continue with Google") by default;
 * an [accentColor] gives a transparent button with a 1.5dp border and text in that colour
 * (primary for Pair "Connect", error for Memory "Delete").
 */
@Composable
fun TwoverseOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color? = null,
) {
    val colors = TwoverseTheme.colors
    val accent = accentColor != null
    val borderColor = if (enabled && accentColor != null) accentColor else colors.outline
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = buttonHeight(small)),
        enabled = enabled,
        shape = buttonShape(small),
        border = BorderStroke(if (accent) AccentBorderWidth else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (accent) Color.Transparent else colors.surface,
            contentColor = accentColor ?: colors.onSurface,
            disabledContentColor = colors.onSurfaceVariant,
        ),
        contentPadding = ButtonContentPadding,
    ) {
        ButtonLabel(text = text, leadingIcon = leadingIcon)
    }
}

@Composable
fun TwoverseTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = TwoverseTheme.colors.primary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = TwoverseTheme.spacing.minTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    @DrawableRes leadingIcon: Int?,
    small: Boolean,
    enabled: Boolean,
    adaptive: Boolean,
    contentDescription: String?,
    buttonColors: ButtonColors,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = buttonHeight(small))
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        enabled = enabled,
        shape = buttonShape(small),
        colors = buttonColors,
        contentPadding = if (adaptive) AdaptiveButtonContentPadding else ButtonContentPadding,
    ) {
        if (adaptive) {
            AdaptiveButtonLabel(text = text, leadingIcon = leadingIcon)
        } else {
            ButtonLabel(text = text, leadingIcon = leadingIcon)
        }
    }
}

@Composable
private fun ButtonLabel(text: String, @DrawableRes leadingIcon: Int?) {
    if (leadingIcon != null) {
        Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            modifier = Modifier.size(ButtonIconSize),
        )
        Spacer(modifier = Modifier.width(TwoverseTheme.spacing.xs))
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun AdaptiveButtonLabel(text: String, @DrawableRes leadingIcon: Int?) {
    var stacked by remember { mutableStateOf(false) }
    val style = MaterialTheme.typography.titleSmall
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                )
                Spacer(modifier = Modifier.height(TwoverseTheme.spacing.xxs))
            }
            Text(text = text, style = style, textAlign = TextAlign.Center)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                )
                Spacer(modifier = Modifier.width(TwoverseTheme.spacing.xxs))
            }
            Text(
                text = text,
                style = style,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { if (it.hasVisualOverflow) stacked = true },
            )
        }
    }
}

private fun buttonHeight(small: Boolean): Dp = if (small) SmallButtonHeight else RegularButtonHeight

@Composable
private fun buttonShape(small: Boolean) =
    if (small) TwoverseTheme.shapes.buttonSmall else TwoverseTheme.shapes.button

@PreviewLightDark
@Composable
private fun TwoverseButtonsPreview() {
    TwoversePreviewBackground {
        Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm)) {
            TwoversePrimaryButton(
                text = "Send a memory",
                onClick = {},
                leadingIcon = R.drawable.ic_lock,
                modifier = Modifier.fillMaxWidth(),
            )
            TwoverseSecondaryButton(text = "Copy", onClick = {}, small = true)
            TwoverseOutlineButton(
                text = "Continue with Google",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            TwoverseOutlineButton(
                text = "Connect",
                onClick = {},
                accentColor = TwoverseTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            TwoverseTextButton(text = "I have a couple code", onClick = {})
            TwoversePrimaryButton(
                text = "Connect",
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwoverseTheme.spacing.xs),
            )
        }
    }
}

@Preview(name = "360dp", widthDp = 360)
@Preview(name = "360dp dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", widthDp = 360, fontScale = 1.6f)
@Preview(name = "Large font dark", widthDp = 360, fontScale = 1.6f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TwoverseAdaptiveButtonsPreview() {
    TwoversePreviewBackground {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
        ) {
            TwoversePrimaryButton(
                text = "Memory",
                onClick = {},
                leadingIcon = R.drawable.ic_image,
                adaptive = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            TwoverseTonalButton(
                text = "Shooting Star",
                onClick = {},
                leadingIcon = R.drawable.ic_sparkle,
                adaptive = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}
