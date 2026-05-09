    package com.theo.forest.ml

    import android.content.Context
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.util.Log
    import androidx.core.graphics.scale
    import com.google.gson.Gson
    import com.theo.forest.R
    import com.theo.forest.data.modal.DiseaseMetadata
    import com.theo.forest.data.modal.MLResult
    import org.tensorflow.lite.Interpreter
    import java.io.FileInputStream
    import java.nio.ByteBuffer
    import java.nio.ByteOrder
    import java.nio.channels.FileChannel

    class DiseaseDetectionModal(private val context: Context) {
        private lateinit var tflite: Interpreter
        private lateinit var metadata: DiseaseMetadata
        private lateinit var labels: List<String>

        init {
            loadModel()
            loadMetadata()
            loadLabels()
        }

        fun loadModel() {
            try {
                val assetFileDescriptor = context.assets.openFd("plant_disease_classifier_mobilenetv2 modal 3.tflite")
                val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val modelBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.declaredLength
                )

                tflite = Interpreter(modelBuffer)
                Log.d("TFLite", "Model loaded successfully")
            } catch (e: Exception) {
                Log.e("TFLite", "Model load failed: ${e.message}")
            }
        }

        fun loadMetadata() {
            try {
                // Using your specific filename: disease_metadat.json
                val jsonString =
                    context.assets.open("modal3.json").bufferedReader().use { it.readText() }
                metadata = Gson().fromJson(jsonString, DiseaseMetadata::class.java)
            } catch (e: Exception) {
                Log.e("TFLite", "Metadata load failed: ${e.message}")
            }
        }

        fun loadLabels() {
            try {
                labels = context.assets.open("labels modal 3.txt").bufferedReader().readLines()
            } catch (e: Exception) {
                Log.e("TFLite", "Labels load failed: ${e.message}")
            }
        }

        fun runInference(bitmap: Bitmap): MLResult {
            // Ensure all components are loaded before running
            if (!::tflite.isInitialized || !::metadata.isInitialized || !::labels.isInitialized) {
                Log.e("TFLite", "Inference aborted: Initialization not complete")
                return MLResult("Inference aborted: Initialization not complete", 100f)
            }

            try {
                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                // Use the existing drawable resource since 'test_leaf.jpg' is missing from assets
                val inputWidth = metadata.input.shape[0]
                val inputHeight = metadata.input.shape[1]
                val resizedBitmap = softwareBitmap.scale(inputWidth, inputHeight)

                // Prepare Input Buffer
                val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

                // Prepare Output Buffer
                val outputBuffer = Array(1) { FloatArray(labels.size) }

                // RUN MODEL
                tflite.run(inputBuffer, outputBuffer)

                // Find best result
                val probabilities = outputBuffer[0]
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
                val resultLabel = labels.getOrNull(maxIndex) ?: "Unknown"
                val confidence = probabilities[maxIndex]
                Log.d("TFLite", "Prediction: $resultLabel (${confidence * 100}%)")
                return MLResult(resultLabel, confidence * 100)
            } catch (e: Exception) {
                Log.e("TFLite", "Inference error: ${e.message}")
            }
            return MLResult("Unable to Detect Disease", 100f)
        }

        private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
            val width = metadata.input.shape[0]
            val height = metadata.input.shape[1]
            val buffer = ByteBuffer.allocateDirect(4 * width * height * 3)
            buffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(width * height)
            bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

            val mean = metadata.input.mean[0] // 127.5
            val std = metadata.input.std[0]   // 127.5

            for (pixel in intValues) {
                buffer.putFloat(((pixel shr 16 and 0xFF) - mean) / std)
                buffer.putFloat(((pixel shr 8 and 0xFF) - mean) / std)
                buffer.putFloat(((pixel and 0xFF) - mean) / std)
            }
            return buffer
        }

    }