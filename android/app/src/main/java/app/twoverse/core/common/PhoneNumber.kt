package app.twoverse.core.common

/** Sri Lanka; numbers typed without a country code use it (FR-PRO-4). */
const val DefaultCountryCode = "94"

private val International = Regex("^\\+[1-9][0-9]{6,14}$")
private val Separators = Regex("[\\s().-]")

/**
 * A typed phone number in international format ("+94771234567"), or null when it isn't valid.
 * "+…" and "00…" keep their country code; "0771234567" and "771234567" get [countryCode].
 */
fun normalizePhone(input: String, countryCode: String = DefaultCountryCode): String? {
    val digits = input.trim().replace(Separators, "")
    if (digits.isEmpty()) return null
    val international = when {
        digits.startsWith("+") -> digits
        digits.startsWith("00") -> "+" + digits.drop(2)
        digits.startsWith("0") -> "+$countryCode" + digits.drop(1)
        else -> "+$countryCode$digits"
    }
    return international.takeIf { International.matches(it) }
}
