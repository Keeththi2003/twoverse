package app.twoverse.feature.star

import androidx.annotation.StringRes
import app.twoverse.R

/** Starting points that fill in the eyebrow and title, which stay editable (FR-STAR-3). */
enum class StarTemplate(
    @param:StringRes val labelRes: Int,
    @param:StringRes val eyebrowRes: Int?,
    @param:StringRes val titleRes: Int?,
) {
    Birthday(R.string.star_template_birthday, R.string.star_template_birthday_eyebrow, R.string.star_template_birthday_title),
    Anniversary(R.string.star_template_anniversary, R.string.star_template_anniversary_eyebrow, R.string.star_template_anniversary_title),
    GoodLuck(R.string.star_template_good_luck, R.string.star_template_good_luck_eyebrow, R.string.star_template_good_luck_title),
    ThinkingOfYou(R.string.star_template_thinking_of_you, R.string.star_template_thinking_of_you_eyebrow, R.string.star_template_thinking_of_you_title),
    Blank(R.string.star_template_blank, null, null),
}

/** A template's words, resolved from string resources by the screen. */
data class StarTemplateText(val eyebrow: String, val title: String)
