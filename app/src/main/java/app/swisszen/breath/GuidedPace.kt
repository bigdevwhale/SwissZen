package app.swisszen.breath

enum class GuidedPreset(val pattern: List<Int>) {
    BOX(listOf(4, 4, 4, 4)),
    CALM(listOf(4, 7, 8, 0)),
    COHERENT(listOf(5, 0, 5, 0));

    /** "4·4·4·4" — only the non-zero parts. */
    val label get() = pattern.filter { it > 0 }.joinToString("·")
}

/** Phase index into a preset pattern: 0 in, 1 hold, 2 out, 3 hold. */
class GuidedPaceEngine(val preset: GuidedPreset, minutes: Int) {
    var phase = 0; private set
    var left = preset.pattern[0]; private set
    var sessionLeft = minutes * 60; private set
    val done get() = sessionLeft <= 0

    /** One-second step; returns true when a new phase starts. */
    fun tickSecond(): Boolean {
        if (done) return false
        left--; sessionLeft--
        if (done) return false
        if (left <= 0) {
            phase = nextPhase(phase)
            left = preset.pattern[phase]
            return true
        }
        return false
    }

    private fun nextPhase(i: Int): Int {
        var j = i
        do { j = (j + 1) % 4 } while (preset.pattern[j] == 0)
        return j
    }
}
