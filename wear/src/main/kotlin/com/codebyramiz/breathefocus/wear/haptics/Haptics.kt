package com.codebyramiz.breathefocus.wear.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.codebyramiz.breathefocus.core.model.BreathPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Wrist feedback, kept behind an interface so ViewModels stay unit-testable. */
interface Haptics {
    fun phaseChanged(phase: BreathPhase)

    fun focusBlockFinished()
}

class VibratorHaptics @Inject constructor(@ApplicationContext context: Context) : Haptics {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    override fun phaseChanged(phase: BreathPhase) {
        val effect = when (phase) {
            BreathPhase.INHALE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            BreathPhase.EXHALE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            BreathPhase.HOLD_IN, BreathPhase.HOLD_OUT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
        vibrator.vibrate(effect)
    }

    override fun focusBlockFinished() {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 200, 120, 400), -1))
    }
}
