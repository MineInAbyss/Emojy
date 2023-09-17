package com.mineinabyss.emojy.translator

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class EmojyLanguage(
    @SerialName("locale") val _locale: String,
    val keys: Map<String, String>
) {
    val locale: Locale get() = Locale.forLanguageTag(_locale)
}
