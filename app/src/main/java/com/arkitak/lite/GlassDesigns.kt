package com.arkitak.lite

/** One glass-orb design. [id] must match a key in assets/glass_orb.html. */
data class GlassDesign(val id: String, val name: String, val about: String, val colorA: Int, val colorB: Int)

object GlassDesigns {
    val ALL = listOf(
        GlassDesign("crystal", "Crystal", "Clear glass with soft halo rings", 0xFFDCE8FF.toInt(), 0xFF7CC4FF.toInt()),
        GlassDesign("ruby", "Ruby", "Deep red glass with a heartbeat pulse", 0xFFD0102A.toInt(), 0xFFFF6B7D.toInt()),
        GlassDesign("amber", "Amber", "Golden glass with orbiting beads", 0xFFFFB300.toInt(), 0xFFFFD978.toInt()),
        GlassDesign("sapphire", "Sapphire", "Blue glass with endless sound ripples", 0xFF1F5CFF.toInt(), 0xFF9DBBFF.toInt()),
        GlassDesign("ember", "Ember", "Red and amber flowing into each other", 0xFFD0102A.toInt(), 0xFFFFB300.toInt()),
        GlassDesign("twilight", "Twilight", "Amber to sapphire, with orbiting beads", 0xFFFFB300.toInt(), 0xFF1F5CFF.toInt()),
        GlassDesign("nebula", "Nebula", "Sapphire to red, with ripples and stardust", 0xFF1F5CFF.toInt(), 0xFFD0102A.toInt()),
        GlassDesign("emerald", "Emerald", "Green and aqua glass with sparkles", 0xFF12D68A.toInt(), 0xFF00B8D9.toInt())
    )
    const val DEFAULT_ID = "crystal"
    fun byId(id: String): GlassDesign = ALL.firstOrNull { it.id == id } ?: ALL[0]
}
