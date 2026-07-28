package com.zayne.applock.util

object GracePeriods {
    // Anzeigename zu Millisekunden. "Sofort" heißt: bei jedem Betreten erneut sperren.
    val options: List<Pair<String, Long>> = listOf(
        "Sofort" to 0L,
        "5 Sekunden" to 5_000L,
        "15 Sekunden" to 15_000L,
        "30 Sekunden" to 30_000L,
        "1 Minute" to 60_000L,
        "5 Minuten" to 5 * 60_000L,
        "15 Minuten" to 15 * 60_000L,
        "30 Minuten" to 30 * 60_000L
    )

    fun labelFor(ms: Long): String = options.firstOrNull { it.second == ms }?.first ?: "$ms ms"
}
