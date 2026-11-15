package com.codebyramiz.breathefocus.wear.haptics

import android.content.Context
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VibratorHapticsTest {

    @Test
    fun `focus block buzz carries the alarm usage so it is not dropped in the background`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vibrator: Vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

        VibratorHaptics(context).focusBlockFinished()

        assertEquals(
            VibrationAttributes.USAGE_ALARM,
            (shadowOf(vibrator).vibrationAttributesFromLastVibration as VibrationAttributes).usage,
        )
    }
}
