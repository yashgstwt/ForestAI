package com.theo.forest.ui.viewmodals

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theo.forest.data.modal.DiseaseInfo
import com.theo.forest.data.modal.MLResult
import com.theo.forest.data.modal.Response
import com.theo.forest.data.repository.ApiRepository
import com.theo.forest.ml.DiseaseDetectionModal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModal @Inject constructor(
    private val mlModal: DiseaseDetectionModal,
    private val repository: ApiRepository,
) : ViewModel() {

    val result: MutableState<MLResult> = mutableStateOf(MLResult("Detecting...", 0f))
    val res = MutableStateFlow<MLResult>(MLResult("Detecting...", 0f))
    
    private val _pickedImage = mutableStateOf<Bitmap?>(null)
    val pickedImage: MutableState<Bitmap?> = _pickedImage

    private val _diseaseInfo = MutableStateFlow<Response<DiseaseInfo?>>(Response.Loading)
    val diseaseInfo = _diseaseInfo.asStateFlow()

    init {
        mlModal.loadLabels()
        mlModal.loadMetadata()
        mlModal.loadModel()
    }

    fun getPrediction(bitmap: Bitmap) {
        _pickedImage.value = bitmap
        viewModelScope.launch {
            _diseaseInfo.value = Response.Loading
            val mlResult = mlModal.runInference(bitmap)
            result.value = mlResult
            mlResult.disease.replace("___", ": ").replace("_", " ")


            if(mlResult.disease.contains("Background", ignoreCase = true)){
                _diseaseInfo.value = Response.Success(
                    null
                )
            } else if (!mlResult.disease.contains("healthy", ignoreCase = true)) {
                _diseaseInfo.value = repository.getDiseaseInfo(mlResult.disease)
                Log.d("TFLite", _diseaseInfo.value.toString())
//                Log.d("TFLite", "background leaf detected")
            } else {
                _diseaseInfo.value = Response.Success(
                    DiseaseInfo(
                        description = "The plant appears to be healthy.",
                        symptoms = "No symptoms detected.",
                        causes = "N/A",
                        treatment = "Continue regular maintenance and monitoring.",
                        prevention = "Maintain good cultural practices."
                    )
                )
            }
        }
    }
}
