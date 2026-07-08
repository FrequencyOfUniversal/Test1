package com.example.musicplayer.player

import android.media.audiofx.Equalizer

/**
 * Bọc android.media.audiofx.Equalizer để UI có thể đọc/ghi mức gain
 * theo từng dải tần (band) mà không cần biết chi tiết API hệ thống.
 */
class EqualizerManager(audioSessionId: Int) {

    private val equalizer: Equalizer = Equalizer(0, audioSessionId).apply {
        enabled = true
    }

    val bandCount: Int get() = equalizer.numberOfBands.toInt()

    val levelRangeMillibel: Pair<Int, Int> get() {
        val range = equalizer.bandLevelRange
        return range[0].toInt() to range[1].toInt()
    }

    fun bandFrequencyHz(band: Int): Int =
        equalizer.getCenterFreq(band.toShort()) / 1000

    fun getBandLevel(band: Int): Int =
        equalizer.getBandLevel(band.toShort()).toInt()

    fun setBandLevel(band: Int, levelMillibel: Int) {
        equalizer.setBandLevel(band.toShort(), levelMillibel.toShort())
    }

    fun applyPreset(presetIndex: Int) {
        equalizer.usePreset(presetIndex.toShort())
    }

    fun presetNames(): List<String> =
        (0 until equalizer.numberOfPresets).map { equalizer.getPresetName(it.toShort()) }

    fun release() {
        equalizer.release()
    }
}
