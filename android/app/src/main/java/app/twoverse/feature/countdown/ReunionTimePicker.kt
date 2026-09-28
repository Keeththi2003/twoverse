package app.twoverse.feature.countdown

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import java.time.LocalTime

private val DefaultTime: LocalTime = LocalTime.of(10, 0)

/** Time of the reunion, in the phone's 12/24-hour format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReunionTimePicker(
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val start = initialTime ?: DefaultTime
    val state = rememberTimePickerState(
        initialHour = start.hour,
        initialMinute = start.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwoverseTheme.colors.surface,
        text = { TimePicker(state = state) },
        confirmButton = {
            TwoverseTextButton(
                text = stringResource(R.string.common_ok),
                onClick = { onTimeSelected(LocalTime.of(state.hour, state.minute)) },
            )
        },
        dismissButton = { TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss) },
    )
}
