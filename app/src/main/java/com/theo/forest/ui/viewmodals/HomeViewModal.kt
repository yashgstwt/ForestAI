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
import com.theo.forest.data.modal.WeatherResponse
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
    val pickedImage = mutableStateOf<Bitmap?>(null)
    
    private val _diseaseInfo = MutableStateFlow<Response<DiseaseInfo?>>(Response.Loading)
    val diseaseInfo = _diseaseInfo.asStateFlow()

    private val _weatherInfo = MutableStateFlow<Response<WeatherResponse>>(Response.Loading)
    val weatherInfo = _weatherInfo.asStateFlow()

    private val _saveState = MutableStateFlow<Response<Unit>>(Response.Loading)
    val saveState = _saveState.asStateFlow()

    val useGemini = mutableStateOf(false)

    init {
        mlModal.loadLabels()
        mlModal.loadMetadata()
        mlModal.loadModel()
    }

    fun toggleModal(isGemini: Boolean) {
        useGemini.value = isGemini
    }

    fun getWeather(location: String) {
        viewModelScope.launch {
            _weatherInfo.value = Response.Loading
            _weatherInfo.value = repository.getWeatherData(location, com.theo.forest.data.Constant.WEATHER_API_KEY)
        }
    }

    fun getPrediction(bitmap: Bitmap) {
        viewModelScope.launch {
            _diseaseInfo.value = Response.Loading
            _saveState.value = Response.Loading
            
            if (useGemini.value) {
                val geminiResponse = repository.getGeminiPrediction(bitmap)
                when (geminiResponse) {
                    is Response.Success -> {
                        val (mlResult, info) = geminiResponse.result
                        result.value = mlResult
                        _diseaseInfo.value = Response.Success(info)
                        
                        // Save to Supabase including disease details
                        _saveState.value = repository.saveScanResult(bitmap, mlResult, info)
                    }
                    is Response.Error -> {
                        val errorMsg = geminiResponse.error
                        _diseaseInfo.value = Response.Error(errorMsg)
                        _saveState.value = Response.Error(errorMsg)
                    }
                    else -> {}
                }
            } else {
                val mlResult = mlModal.runInference(bitmap)
                result.value = mlResult

                var diseaseInfo: DiseaseInfo? = null
                if(mlResult.disease.contains("Background", ignoreCase = true)){
                    _diseaseInfo.value = Response.Success(null)
                } else if (!mlResult.disease.contains("healthy", ignoreCase = true)) {
                    val infoResponse = repository.getDiseaseInfo(mlResult.disease)
                    _diseaseInfo.value = infoResponse
                    if (infoResponse is Response.Success) {
                        diseaseInfo = infoResponse.result
                    }
                } else {
                    diseaseInfo = DiseaseInfo(
                        description = "The plant appears to be healthy.",
                        symptoms = "No symptoms detected.",
                        causes = "N/A",
                        treatment = "Continue regular maintenance and monitoring.",
                        prevention = "Maintain good cultural practices."
                    )
                    _diseaseInfo.value = Response.Success(diseaseInfo)
                }
                
                // Save TFLite result to Supabase
                if (!mlResult.disease.contains("Background", ignoreCase = true)) {
                    _saveState.value = repository.saveScanResult(bitmap, mlResult, diseaseInfo)
                }
            }
        }
    }
}
