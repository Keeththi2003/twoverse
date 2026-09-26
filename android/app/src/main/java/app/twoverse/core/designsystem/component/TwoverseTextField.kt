package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val SingleLineMinHeight = 54.dp
private val MultiLineMinHeight = 92.dp
private val FieldHorizontalPadding = 18.dp
private val FieldVerticalPadding = 14.dp

/**
 * Text field with its label above the box (DESIGN.md §4: 54dp, 16dp radius, 1dp outline).
 * The border turns primary while focused so keyboard and TalkBack users can see focus.
 */
@Composable
fun TwoverseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    errorText: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.input
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val fieldTextStyle = textStyle.copy(color = colors.onSurface)
    val borderColor = when {
        errorText != null -> colors.error
        focused -> colors.primary
        else -> colors.outline
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            textStyle = fieldTextStyle,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .heightIn(min = if (singleLine) SingleLineMinHeight else MultiLineMinHeight)
                        .background(colors.surface, shape)
                        .border(1.dp, borderColor, shape)
                        .padding(start = FieldHorizontalPadding),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                end = if (trailingIcon == null) FieldHorizontalPadding else 0.dp,
                                top = FieldVerticalPadding,
                                bottom = FieldVerticalPadding,
                            ),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = fieldTextStyle, color = colors.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            },
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TwoverseTextFieldPreview() {
    TwoversePreviewBackground {
        Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.md)) {
            TwoverseTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "you@example.com",
            )
            TwoverseTextField(
                value = "secret",
                onValueChange = {},
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.ic_eye),
                            contentDescription = "Show password",
                            tint = TwoverseTheme.colors.onSurfaceVariant,
                        )
                    }
                },
            )
            TwoverseTextField(
                value = "",
                onValueChange = {},
                label = "Your partner's code",
                errorText = "This couple code is invalid or has expired.",
            )
            TwoverseTextField(
                value = "",
                onValueChange = {},
                label = "Caption",
                placeholder = "Write something for her…",
                singleLine = false,
            )
        }
    }
}
