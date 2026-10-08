package app.twoverse.feature.orbit

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseTextButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A date between [minDate] and [maxDate] (inclusive, either open-ended). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrbitDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val minMillis = minDate?.toUtcMillis()
    val maxMillis = maxDate?.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                (minMillis == null || utcTimeMillis >= minMillis) && (maxMillis == null || utcTimeMillis <= maxMillis)

            override fun isSelectableYear(year: Int): Boolean =
                (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
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
        dismissButton = { TwoverseTextButton(text = stringResource(R.string.common_cancel), onClick = onDismiss) },
    ) {
        DatePicker(state = state)
    }
}

/** The Material date picker works in UTC midnight milliseconds. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
