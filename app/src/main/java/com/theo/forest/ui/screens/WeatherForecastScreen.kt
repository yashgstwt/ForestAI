package com.theo.forest.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.theo.forest.R
import com.theo.forest.data.modal.Response
import com.theo.forest.data.modal.WeatherDay
import com.theo.forest.ui.viewmodals.HomeViewModal

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WeatherForecastScreen(
    viewModal: HomeViewModal,
    navToHome: () -> Unit,
    navToHistory: () -> Unit,
    backPress: () -> Unit
) {
    val context = LocalContext.current
    val weatherState by viewModal.weatherInfo.collectAsState()
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    var isDetectingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val requestLocationInternal = {
        isDetectingLocation = true
        locationError = null
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                val locationString = "${location.latitude},${location.longitude}"
                Log.d("WeatherLocation", "Detected Location: $locationString")
                viewModal.getWeather(locationString)
                isDetectingLocation = false
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        viewModal.getWeather("${lastLoc.latitude},${lastLoc.longitude}")
                    } else {
                        locationError = "Could not determine location even after enabling GPS. Try moving near a window."
                    }
                    isDetectingLocation = false
                }
            }
        }.addOnFailureListener {
            locationError = "Location request failed: ${it.localizedMessage}"
            isDetectingLocation = false
        }
    }

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            Log.d("WeatherLocation", "Location settings satisfied after prompt")
            requestLocationInternal()
        } else {
            locationError = "Location must be enabled to get the forecast. Please enable it and try again."
            isDetectingLocation = false
        }
    }

    val checkLocationSettingsAndRequest = {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            requestLocationInternal()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                    settingResultRequest.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    locationError = "Could not prompt for location settings."
                }
            } else {
                locationError = "Location settings are inadequate."
            }
        }
    }

    val requestLocation = {
        if (locationPermissionsState.allPermissionsGranted) {
            checkLocationSettingsAndRequest()
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            checkLocationSettingsAndRequest()
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("7-Day Forecast", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = backPress) {
                        Icon(painter = painterResource(R.drawable.back), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { requestLocation() }) {
                        Icon(painter = painterResource(R.drawable.share_24), contentDescription = "Refresh Location")
                    }
                }
            )
        },
        bottomBar = {
          ForestBottomBar(
                currentScreen = "weather",
                onHomeClick = navToHome,
                onWeatherClick = { /* Already here */ },
                onHistoryClick = navToHistory
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isDetectingLocation) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detecting your location...")
                }
            } else if (locationError != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = locationError!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Button(onClick = { requestLocation() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Try Again")
                    }
                }
            } else {
                when (val res = weatherState) {
                    is Response.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is Response.Success -> {
                        val weather = res.result
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Location: ${weather.address}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(weather.days) { day ->
                                WeatherItem(day)
                            }
                        }
                    }
                    is Response.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Error: ${res.error}", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { requestLocation() }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherItem(day: WeatherDay) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = day.datetime, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = day.conditions, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${day.tempmax}° / ${day.tempmin}°",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Humidity: ${day.humidity}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
