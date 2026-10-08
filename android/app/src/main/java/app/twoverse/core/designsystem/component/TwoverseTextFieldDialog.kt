package app.twoverse.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** A dialog asking for one value, with an optional note and an extra action (e.g. "Remove nickname"). */
@Composable
fun TwoverseTextFieldDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    extraActionLabel: String? = null,
    onExtraAction: () -> Unit = {},
) {
    val colors = TwoverseTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        shape = TwoverseTheme.shapes.card,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm)) {
                TwoverseTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = label,
                    errorText = errorText,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                )
                note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                extraActionLabel?.let { TwoverseTextButton(text = it, onClick = onExtraAction, color = colors.error) }
            }
        },
        confirmButton = { TwoverseTextButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = { TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss) },
    )
}

@PreviewLightDark
@Composable
private fun TwoverseTextFieldDialogPreview() {
    TwoverseTheme {
        TwoverseTextFieldDialog(
            title = "Your nickname for them",
            value = "Chellam",
            onValueChange = {},
            label = "Nickname",
            confirmLabel = "Save",
            onConfirm = {},
            onDismiss = {},
            note = "Only you see this",
            extraActionLabel = "Remove nickname",
        )
    }
}
