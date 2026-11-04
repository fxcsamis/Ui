package com.arkitak.lite

/** The ten orb looks. Each id is a theme name inside assets/orb.html. [words] are spoken colour names that select it. */
object OrbDesigns {
    data class Design(val id: String, val label: String, val words: List<String>)

    val all = listOf(
        Design("nebula", "Nebula (blue)", listOf("nebula", "blue", "নীল")),
        Design("ember", "Ember (red)", listOf("ember", "red", "লাল", "lal")),
        Design("aurora", "Aurora (green)", listOf("aurora", "green", "সবুজ", "sobuj")),
        Design("dusk", "Dusk (orange)", listOf("dusk", "orange", "কমলা")),
        Design("gold", "Gold (yellow)", listOf("gold", "yellow", "হলুদ", "সোনালি")),
        Design("ice", "Ice (white)", listOf("ice", "white", "সাদা")),
        Design("rose", "Rose (pink)", listOf("rose", "pink", "গোলাপি")),
        Design("mono", "Mono (silver)", listOf("mono", "silver", "grey", "gray", "ধূসর")),
        Design("neon", "Neon (lime)", listOf("neon", "lime", "নিয়ন")),
        Design("royal", "Royal (purple)", listOf("royal", "purple", "violet", "বেগুনি"))
    )

    fun indexOf(id: String): Int = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
    fun next(id: String): String = all[(indexOf(id) + 1) % all.size].id
}
