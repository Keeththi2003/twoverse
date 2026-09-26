package app.twoverse.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** Confirmation before an action that can't be undone. [destructive] colours the confirm action red. */
@Composable
fun TwoverseConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
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
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TwoverseTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                color = if (destructive) colors.error else colors.primary,
            )
        },
        dismissButton = {
            TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    )
}

@PreviewLightDark
@Composable
private fun TwoverseConfirmDialogPreview() {
    TwoverseTheme {
        TwoverseConfirmDialog(
            title = "Delete this memory?",
            message = "It will be removed for both of you. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
        )
    }
}
