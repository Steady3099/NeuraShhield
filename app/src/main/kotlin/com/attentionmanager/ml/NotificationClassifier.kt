package com.attentionmanager.ml

import android.content.Context
import android.util.Log
import com.attentionmanager.domain.model.ActivityType
import com.attentionmanager.domain.model.AppContext
import com.attentionmanager.domain.model.ClassificationDecision
import com.attentionmanager.domain.model.DecisionSource
import com.attentionmanager.domain.model.PriorityTier
import java.io.Closeable
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

class NotificationClassifier(
    private val modelRunnerFactory: () -> NotificationModelRunner?
) : Closeable {
    constructor(context: Context) : this({
        TensorFlowLiteNotificationModel(context.applicationContext)
    })

    private val modelRunner: NotificationModelRunner? by lazy {
        runCatching { modelRunnerFactory() }
            .onFailure { safeLogWarning(TAG, "TFLite classifier unavailable; heuristic fallback active.", it) }
            .getOrNull()
    }

    fun classify(
        title: String,
        body: String,
        sender: String?,
        senderBoost: Float,
        appContext: AppContext
    ): ClassificationDecision {
        PriorityRules.preClassify(title, body, sender)?.let {
            return ClassificationDecision(it.tier, it.confidence, DecisionSource.REGEX)
        }

        heuristicDecision(senderBoost, appContext)?.let { return it }

        val text = listOfNotNull(title, sender, body).joinToString(separator = " ")
        val probabilities = runCatching {
            modelRunner?.classify(NotificationTokenizer.tokenize(text))
        }.onFailure {
            safeLogWarning(TAG, "TFLite inference failed; using local fallback decision.", it)
        }.getOrNull()?.normalizeProbabilities()

        if (probabilities != null) {
            val tier = tierFromProbabilities(probabilities, appContext)
            return ClassificationDecision(
                tier = tier,
                confidence = probabilities[tier.index],
                source = DecisionSource.TFLITE,
                probabilities = probabilities
            )
        }

        return fallbackDecision(appContext)
    }

    private fun heuristicDecision(
        senderBoost: Float,
        appContext: AppContext
    ): ClassificationDecision? {
        val adjustedBoost = if (appContext.hasMeetingNext60Min) senderBoost - 0.1f else senderBoost
        return when {
            adjustedBoost >= 0.7f -> ClassificationDecision(PriorityTier.URGENT, adjustedBoost, DecisionSource.HEURISTIC)
            adjustedBoost >= 0.25f && appContext.activityType != ActivityType.IN_VEHICLE ->
                ClassificationDecision(PriorityTier.IMPORTANT, adjustedBoost, DecisionSource.HEURISTIC)
            adjustedBoost <= -0.55f -> ClassificationDecision(PriorityTier.LOW, -adjustedBoost, DecisionSource.HEURISTIC)
            else -> null
        }
    }

    private fun tierFromProbabilities(probabilities: FloatArray, appContext: AppContext): PriorityTier {
        val urgent = probabilities[PriorityTier.URGENT.index]
        val important = probabilities[PriorityTier.IMPORTANT.index]
        val low = probabilities[PriorityTier.LOW.index]
        val strongestNonUrgent = max(important, low)

        if (appContext.activityType == ActivityType.IN_VEHICLE) {
            return if (urgent.isConfidentUrgent(strongestNonUrgent, IN_VEHICLE_URGENT_THRESHOLD)) {
                PriorityTier.URGENT
            } else {
                PriorityTier.LOW
            }
        }
        if (appContext.hasMeetingNext60Min) {
            return when {
                urgent.isConfidentUrgent(strongestNonUrgent, MEETING_URGENT_THRESHOLD) -> PriorityTier.URGENT
                important >= MEETING_IMPORTANT_THRESHOLD && important >= low + IMPORTANT_MARGIN -> PriorityTier.IMPORTANT
                else -> PriorityTier.LOW
            }
        }
        return when {
            urgent.isConfidentUrgent(strongestNonUrgent, DEFAULT_URGENT_THRESHOLD) -> PriorityTier.URGENT
            important >= DEFAULT_IMPORTANT_THRESHOLD && important >= low + IMPORTANT_MARGIN -> PriorityTier.IMPORTANT
            else -> PriorityTier.LOW
        }
    }

    private fun Float.isConfidentUrgent(strongestNonUrgent: Float, threshold: Float): Boolean =
        this >= threshold && this >= strongestNonUrgent + URGENT_MARGIN

    private fun fallbackDecision(appContext: AppContext): ClassificationDecision =
        if (appContext.activityType == ActivityType.IN_VEHICLE) {
            ClassificationDecision(PriorityTier.LOW, 0.5f, DecisionSource.FALLBACK)
        } else {
            ClassificationDecision(PriorityTier.IMPORTANT, 0.5f, DecisionSource.FALLBACK)
        }

    override fun close() {
        modelRunner?.close()
    }

    private val PriorityTier.index: Int
        get() = when (this) {
            PriorityTier.URGENT -> 0
            PriorityTier.IMPORTANT -> 1
            PriorityTier.LOW -> 2
        }

    companion object {
        private const val TAG = "NotificationClassifier"
        private const val DEFAULT_URGENT_THRESHOLD = 0.72f
        private const val MEETING_URGENT_THRESHOLD = 0.78f
        private const val IN_VEHICLE_URGENT_THRESHOLD = 0.82f
        private const val DEFAULT_IMPORTANT_THRESHOLD = 0.50f
        private const val MEETING_IMPORTANT_THRESHOLD = 0.72f
        private const val URGENT_MARGIN = 0.12f
        private const val IMPORTANT_MARGIN = 0.08f
    }
}

interface NotificationModelRunner : Closeable {
    fun classify(tokenIds: IntArray): FloatArray
}

class TensorFlowLiteNotificationModel(
    private val context: Context,
    private val assetName: String = "notification_classifier.tflite"
) : NotificationModelRunner {
    private val compatibilityList = CompatibilityList()
    private var gpuDelegate: GpuDelegate? = null
    private val model: MappedByteBuffer = loadModel()
    private var interpreter: Interpreter = createInterpreter()

    override fun classify(tokenIds: IntArray): FloatArray {
        var lastFailure: Throwable? = null
        TOKEN_RETRY_MAX_IDS.forEachIndexed { index, maxTokenId ->
            val safeTokenIds = tokenIds.coerceTokenIds(maxTokenId)
            runCatching {
                runWithCurrentInputType(safeTokenIds)
            }.onSuccess { output ->
                if (index > 0) {
                    safeLogWarning(TAG, "TFLite inference recovered after clamping token ids to $maxTokenId.")
                }
                return output
            }.onFailure { throwable ->
                lastFailure = throwable
                if (index == 0 && gpuDelegate != null) {
                    safeLogWarning(TAG, "GPU delegate inference failed; retrying on CPU.", throwable)
                    recreateCpuInterpreter()
                }
            }
        }
        throw lastFailure ?: IllegalStateException("TFLite inference failed without an exception.")
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
        compatibilityList.close()
    }

    private fun createInterpreter(): Interpreter {
        if (isGpuDelegateApiAvailable() && compatibilityList.isDelegateSupportedOnThisDevice) {
            runCatching {
                val delegate = GpuDelegate()
                val options = Interpreter.Options()
                    .setNumThreads(2)
                    .addDelegate(delegate)
                Interpreter(model, options).also {
                    it.allocateTensors()
                    gpuDelegate = delegate
                }
            }.onSuccess {
                return it
            }.onFailure {
                safeLogWarning(TAG, "GPU delegate failed; falling back to CPU.", it)
                gpuDelegate?.close()
                gpuDelegate = null
            }
        }
        return Interpreter(
            model,
            Interpreter.Options().setNumThreads(2)
        ).also { it.allocateTensors() }
    }

    private fun isGpuDelegateApiAvailable(): Boolean =
        runCatching {
            Class.forName("org.tensorflow.lite.gpu.GpuDelegateFactory\$Options")
            true
        }.getOrDefault(false)

    private fun recreateCpuInterpreter() {
        runCatching { interpreter.close() }
        gpuDelegate?.close()
        gpuDelegate = null
        interpreter = Interpreter(
            model,
            Interpreter.Options().setNumThreads(2)
        ).also { it.allocateTensors() }
    }

    private fun runWithCurrentInputType(tokenIds: IntArray): FloatArray =
        when (interpreter.getInputTensor(0).dataType()) {
            DataType.INT32 -> runWithTypedOutput(arrayOf(tokenIds))
            DataType.FLOAT32 -> {
                val input = arrayOf(tokenIds.map { it.toFloat() }.toFloatArray())
                runWithTypedOutput(input)
            }
            else -> runWithTypedOutput(arrayOf(tokenIds))
        }

    private fun runWithTypedOutput(input: Any): FloatArray {
        val outputTensor = interpreter.getOutputTensor(0)
        return when (outputTensor.dataType()) {
            DataType.FLOAT32 -> {
                val output = Array(1) { FloatArray(3) }
                interpreter.run(input, output)
                output[0]
            }
            DataType.UINT8 -> {
                val output = Array(1) { ByteArray(3) }
                interpreter.run(input, output)
                val params = outputTensor.quantizationParams()
                FloatArray(3) { index ->
                    (((output[0][index].toInt() and 0xFF) - params.zeroPoint) * params.scale)
                }
            }
            DataType.INT8 -> {
                val output = Array(1) { ByteArray(3) }
                interpreter.run(input, output)
                val params = outputTensor.quantizationParams()
                FloatArray(3) { index ->
                    ((output[0][index].toInt() - params.zeroPoint) * params.scale)
                }
            }
            DataType.INT32 -> {
                val output = Array(1) { IntArray(3) }
                interpreter.run(input, output)
                output[0].map { it.toFloat() }.toFloatArray()
            }
            else -> {
                val output = Array(1) { FloatArray(3) }
                interpreter.run(input, output)
                output[0]
            }
        }
    }

    private fun loadModel(): MappedByteBuffer {
        val descriptor = context.assets.openFd(assetName)
        FileInputStream(descriptor.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    companion object {
        private const val TAG = "NotificationClassifier"
        private val TOKEN_RETRY_MAX_IDS = intArrayOf(
            NotificationTokenizer.MAX_TOKEN_ID,
            9_999,
            4_095,
            1_023,
            255,
            1
        )
    }
}

private fun IntArray.coerceTokenIds(maxTokenId: Int): IntArray =
    IntArray(size) { index -> this[index].coerceIn(0, maxTokenId) }

private fun safeLogWarning(tag: String, message: String, throwable: Throwable? = null) {
    runCatching {
        if (throwable == null) {
            Log.w(tag, message)
        } else {
            Log.w(tag, message, throwable)
        }
    }
}

private fun FloatArray.normalizeProbabilities(): FloatArray {
    if (size < 3) return floatArrayOf(0f, 1f, 0f)
    val clipped = FloatArray(3) { index -> this[index].coerceAtLeast(0f) }
    val total = clipped.sum()
    if (total == 0f) return floatArrayOf(0f, 1f, 0f)
    return FloatArray(3) { index -> clipped[index] / total }
}
