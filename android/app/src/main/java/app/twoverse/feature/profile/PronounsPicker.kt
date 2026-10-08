package app.twoverse.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.core.designsystem.component.SegmentedOptions
import app.twoverse.core.designsystem.component.TwoversePreviewBackground
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.model.Pronouns

/** She/her, He/him, They/them as three equal options (FR-PRO-2). */
@Composable
internal fun PronounsPicker(selected: Pronouns?, onSelect: (Pronouns) -> Unit, modifier: Modifier = Modifier) {
    SegmentedOptions(
        options = Pronouns.entries.map { stringResource(it.labelRes()) },
        selectedIndex = selected?.ordinal ?: -1,
        onSelect = { onSelect(Pronouns.entries[it]) },
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun PronounsPickerPreview() {
    TwoversePreviewBackground { PronounsPicker(selected = Pronouns.They, onSelect = {}) }
}
