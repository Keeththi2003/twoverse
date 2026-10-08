package app.twoverse.core.designsystem.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.common.PartnerName
import app.twoverse.core.model.Pronouns

/**
 * One sentence written for each pronoun ("She can save it", "He can save it", "They can save it").
 * Separate resources per pronoun, never pieced together, so every language can word them well.
 */
data class PronounStrings(
    @param:StringRes val she: Int,
    @param:StringRes val he: Int,
    @param:StringRes val they: Int,
)

/** The variant for [pronouns]; without pronouns the sentence uses they/them. */
@StringRes
fun PronounStrings.resFor(pronouns: Pronouns?): Int = when (pronouns) {
    Pronouns.She -> she
    Pronouns.He -> he
    Pronouns.They, null -> they
}

@StringRes
fun Pronouns.labelRes(): Int = when (this) {
    Pronouns.She -> R.string.pronouns_she
    Pronouns.He -> R.string.pronouns_he
    Pronouns.They -> R.string.pronouns_they
}

/**
 * The partner's name and pronouns for every screen, provided once at the top of the app from the
 * couple (FR-PRO-1). Null when unpaired; texts then say "your partner".
 */
val LocalPartnerName = staticCompositionLocalOf<PartnerName?> { null }

/** The partner's name mid-sentence ("Send to Ammu"), or "your partner". */
@Composable
fun partnerName(): String = LocalPartnerName.current?.name ?: stringResource(R.string.partner_fallback_name)

/** The partner's name starting a sentence ("Ammu's location"), or "Your partner". */
@Composable
fun partnerNameStart(): String = LocalPartnerName.current?.name ?: stringResource(R.string.partner_fallback_name_start)

/** The sentence for the partner's pronouns, with [formatArgs]. */
@Composable
fun partnerString(strings: PronounStrings, vararg formatArgs: Any): String =
    stringResource(strings.resFor(LocalPartnerName.current?.pronouns), *formatArgs)
