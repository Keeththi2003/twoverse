package app.twoverse.feature.star

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The day the partner first sees the star; only today or later can be chosen (FR-STAR-7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StarDatePicker(
    initialDate: LocalDate?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val todayMillis = today.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initialDate ?: today).toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis

            override fun isSelectableYear(year: Int): Boolean = year >= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TwoverseTextButton(
                text = stringResource(R.string.common_ok),
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            )
        },
        dismissButton = {
            TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    ) {
        DatePicker(state = state)
    }
}

/** The time of day it appears, in the phone's 12/24-hour format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StarTimePicker(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
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

/** The Material date picker works in UTC midnight milliseconds. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
