package com.codebyramiz.breathefocus.wear.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.unregisterMeasureCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

sealed interface HeartRate {
    /** No sensor, no permission, or Health Services missing: the session still runs. */
    data class Unavailable(val reason: String) : HeartRate

    data object Acquiring : HeartRate

    data class Bpm(val value: Int) : HeartRate
}

interface HeartRateMonitor {
    fun readings(): Flow<HeartRate>
}

/** Live heart rate through Health Services [androidx.health.services.client.MeasureClient]. */
class HealthServicesHeartRateMonitor @Inject constructor(@ApplicationContext private val context: Context) :
    HeartRateMonitor {

    override fun readings(): Flow<HeartRate> {
        if (!hasPermission(context)) return flowOf(HeartRate.Unavailable("Permission needed"))
        val measureClient = HealthServices.getClient(context).measureClient
        return callbackFlow {
            val supported = runCatching {
                DataType.HEART_RATE_BPM in measureClient.getCapabilities().supportedDataTypesMeasure
            }.getOrDefault(false)
            if (!supported) {
                trySend(HeartRate.Unavailable("No heart-rate sensor"))
                awaitClose { }
                return@callbackFlow
            }
            trySend(HeartRate.Acquiring)
            val callback = object : MeasureCallback {
                override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                    if (availability is DataTypeAvailability && availability != DataTypeAvailability.AVAILABLE) {
                        trySend(HeartRate.Acquiring)
                    }
                }

                override fun onDataReceived(data: DataPointContainer) {
                    val bpm = data.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: return
                    if (bpm > 0) trySend(HeartRate.Bpm(bpm.toInt()))
                }
            }
            measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            awaitClose {
                runBlocking {
                    runCatching { measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback) }
                }
            }
        }
    }

    companion object {
        /** READ_HEART_RATE replaces BODY_SENSORS from Android 16 (API 36). */
        val permission: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                "android.permission.health.READ_HEART_RATE"
            } else {
                Manifest.permission.BODY_SENSORS
            }

        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
