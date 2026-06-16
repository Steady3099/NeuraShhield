package com.attentionmanager.data.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

object FilterPreferencesSerializer : Serializer<FilterPreferences> {
    override val defaultValue: FilterPreferences = FilterPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): FilterPreferences {
        try {
            val coded = CodedInputStream.newInstance(input)
            var aiFilterEnabled = true
            val digestMinutes = mutableListOf<Int>()
            var onboardingCompleted = false
            var notificationPermissionGranted = false
            var digestIntervalHours = 1

            readLoop@ while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break@readLoop
                    FIELD_AI_FILTER_ENABLED -> {
                        aiFilterEnabled = coded.readBool()
                    }
                    FIELD_DIGEST_MINUTE -> {
                        digestMinutes += coded.readInt32()
                    }
                    FIELD_DIGEST_MINUTE_PACKED -> {
                        val limit = coded.pushLimit(coded.readRawVarint32())
                        while (coded.bytesUntilLimit > 0) {
                            digestMinutes += coded.readInt32()
                        }
                        coded.popLimit(limit)
                    }
                    FIELD_ONBOARDING_COMPLETED -> {
                        onboardingCompleted = coded.readBool()
                    }
                    FIELD_NOTIFICATION_PERMISSION_GRANTED -> {
                        notificationPermissionGranted = coded.readBool()
                    }
                    FIELD_DIGEST_INTERVAL_HOURS -> {
                        digestIntervalHours = coded.readInt32()
                    }
                    else -> if (!coded.skipField(tag)) break@readLoop
                }
            }
            return FilterPreferences(
                aiFilterEnabled = aiFilterEnabled,
                digestMinutesOfDayList = digestMinutes,
                onboardingCompleted = onboardingCompleted,
                notificationPermissionGranted = notificationPermissionGranted,
                digestIntervalHours = digestIntervalHours.coerceIn(1, 24)
            )
        } catch (exception: Exception) {
            throw CorruptionException("Unable to read filter preferences.", exception)
        }
    }

    override suspend fun writeTo(t: FilterPreferences, output: OutputStream) {
        val coded = CodedOutputStream.newInstance(output)
        coded.writeBool(1, t.aiFilterEnabled)
        t.digestMinutesOfDayList.forEach { coded.writeInt32(2, it) }
        coded.writeBool(3, t.onboardingCompleted)
        coded.writeBool(4, t.notificationPermissionGranted)
        coded.writeInt32(5, t.digestIntervalHours.coerceIn(1, 24))
        coded.flush()
    }

    private const val FIELD_AI_FILTER_ENABLED = 8
    private const val FIELD_DIGEST_MINUTE = 16
    private const val FIELD_DIGEST_MINUTE_PACKED = 18
    private const val FIELD_ONBOARDING_COMPLETED = 24
    private const val FIELD_NOTIFICATION_PERMISSION_GRANTED = 32
    private const val FIELD_DIGEST_INTERVAL_HOURS = 40
}
