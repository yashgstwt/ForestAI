package com.theo.forest.ui.screens

import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.theo.forest.R
import com.theo.forest.data.Constant
import com.theo.forest.data.modal.Response
import com.theo.forest.ui.viewmodals.HomeViewModal
import com.theo.forest.ui.theme.ForestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModal: HomeViewModal = hiltViewModel(),
    navToDetail: () -> Unit = {},
    navToWeather: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val diseaseInfoState by viewModal.diseaseInfo.collectAsState()
    val saveState by viewModal.saveState.collectAsState()
    val useGemini by viewModal.useGemini

    LaunchedEffect(saveState) {
        when (saveState) {
            is Response.Success -> {
                Toast.makeText(context, "Data successfully saved to Supabase!", Toast.LENGTH_SHORT).show()
            }
            is Response.Error -> {
                Toast.makeText(context, "Error saving to Supabase: ${(saveState as Response.Error).error}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModal.pickedImage.value = bitmap
                viewModal.getPrediction(bitmap!!)
            }
        }

    val stroke = Stroke(
        width = 8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Forest Disease Detection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModal.logout()
                        onLogout()
                    }) {
                        Icon(painter = painterResource(R.drawable.home), contentDescription = "Logout") // Reusing home icon for logout
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (useGemini) "Gemini AI" else "Local TFLite",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (useGemini) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useGemini,
                            onCheckedChange = { viewModal.toggleModal(it) },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (useGemini) R.drawable.chat else R.drawable.leaf),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ForestBottomBar(
                currentScreen = "home",
                onHomeClick = { /* Already on Home */ },
                onWeatherClick = navToWeather
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .clickable {
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clickable {
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                .drawBehind {
                                    drawRoundRect(
                                        secondaryColor,
                                        style = stroke,
                                        cornerRadius = CornerRadius(40.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.leaf),
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (diseaseInfoState) {
                        is Response.Loading -> {
                            if (bitmap != null) {
                                LoadingUI("Analyzing Leaf...")
                            } else {
                                Text(
                                    "Please Select Diseased Leaf Image",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is Response.Success -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = viewModal.result.value.disease.uppercase(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Confidence: ${(viewModal.result.value.confidence * 100).toInt()}%",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if(!viewModal.result.value.disease.contains("background" , true)){
                                    Button(
                                        onClick = navToDetail,
                                        modifier = Modifier.padding(top = 16.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("View Details")
                                    }
                                }
                            }
                        }
                        is Response.Error -> {
                            ErrorUI(
                                message = (diseaseInfoState as Response.Error).error,
                                onRetry = { bitmap?.let { viewModal.getPrediction(it) } }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(
                    "Supported Crops",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                CropLabel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                )
            }
        }
    }
}

@Composable
fun ForestBottomBar(
    currentScreen: String,
    onHomeClick: () -> Unit,
    onWeatherClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = onHomeClick,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.home),
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentScreen == "weather",
            onClick = onWeatherClick,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.chat), // Using chat icon as weather/forecast for now
                    contentDescription = "Weather",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Weather") }
        )
    }
}

@Composable
fun LoadingUI(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.leaf), // Reusing leaf icon for loading
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .rotate(angle),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorUI(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Oops! Something went wrong",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}

@Composable
fun CropLabel(modifier: Modifier = Modifier) {
    LazyRow(modifier) {
        items(Constant.list) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    it, 
                    fontSize = 14.sp, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
