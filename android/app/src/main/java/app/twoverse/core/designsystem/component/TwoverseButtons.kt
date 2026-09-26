package app.twoverse.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

@Composable
fun TwoversePrimaryButton(
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
        buttonColors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
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
 * `accent = true` gives a transparent button with a 1.5dp primary border and primary text (Pair "Connect").
 */
@Composable
fun TwoverseOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    val colors = TwoverseTheme.colors
    val borderColor = when {
        !enabled -> colors.outline
        accent -> colors.primary
        else -> colors.outline
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = buttonHeight(small)),
        enabled = enabled,
        shape = buttonShape(small),
        border = BorderStroke(if (accent) AccentBorderWidth else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (accent) Color.Transparent else colors.surface,
            contentColor = if (accent) colors.primary else colors.onSurface,
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
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = TwoverseTheme.spacing.minTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = TwoverseTheme.colors.primary),
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
    buttonColors: ButtonColors,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = buttonHeight(small)),
        enabled = enabled,
        shape = buttonShape(small),
        colors = buttonColors,
        contentPadding = ButtonContentPadding,
    ) {
        ButtonLabel(text = text, leadingIcon = leadingIcon)
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
                accent = true,
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
