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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.theo.forest.R
import com.theo.forest.data.Constant
import com.theo.forest.data.modal.Response
import com.theo.forest.ui.viewmodals.HomeViewModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModal: HomeViewModal = hiltViewModel(),
    navToDetail: () -> Unit = {},
    navToWeather: () -> Unit = {},
    navToHistory: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val diseaseInfoState by viewModal.diseaseInfo.collectAsState()
    val saveState by viewModal.saveState.collectAsState()
    val useGemini by viewModal.useGemini
    val result by viewModal.result

    LaunchedEffect(saveState) {
        when (saveState) {
            is Response.Success -> {
                Toast.makeText(context, "Data successfully saved!", Toast.LENGTH_SHORT).show()
            }

            is Response.Error -> {
                Log.e("Supabase", "Save error: ${(saveState as Response.Error).error}")
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

    Scaffold(
        containerColor = Color(0xFFE8F0E0),
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Forest", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),
            )
        },
        bottomBar = {
            ForestBottomBar(
                currentScreen = "home",
                onHomeClick = { /* Already on Home */ },
                onHistoryClick = navToHistory,
                onWeatherClick = navToWeather
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Custom Mode Toggle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable { viewModal.toggleModal(!useGemini) },
                    contentAlignment = if (useGemini) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (useGemini) R.drawable.chat else R.drawable.leaf),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = if (useGemini) "Mode:Online" else "Mode:Offline",
                    color = Color(0xFF1B3113),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .border(BorderStroke(1.dp, Color.Black),RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {

                    Icon(
                        painter = painterResource(R.drawable.leaf),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF1B3113).copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Prediction Text
            when (diseaseInfoState) {
                is Response.Loading -> {
                    if (bitmap != null) {
                        LoadingUI("Analyzing...")
                    } else {
                        Text(
                            "Please Select a Leaf Image",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1B3113)
                        )
                    }
                }

                is Response.Success -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = result.disease.uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B3113),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Confidence: ${"%.2f".format(result.confidence * 100)}%",
                            fontSize = 16.sp,
                            color = Color.Black.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = navToDetail,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3113)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("View Details", color = Color.White)
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
}

@Composable
fun ForestBottomBar(
    currentScreen: String,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onWeatherClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFF2D2D2D), RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Home Icon
            BottomBarItem(
                isSelected = currentScreen == "home",
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = onHomeClick
            )

            // Weather Icon
            BottomBarItem(
                isSelected = currentScreen == "weather",
                icon = Icons.Default.Cloud,
                contentDescription = "Weather",
                onClick = onWeatherClick
            )

            // History Icon
            BottomBarItem(
                isSelected = currentScreen == "history",
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "History",
                onClick = onHistoryClick
            )
        }
    }
}

@Composable
fun BottomBarItem(
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFC5E17A) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(28.dp)
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
            painter = painterResource(R.drawable.leaf),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .rotate(angle),
            tint = Color(0xFF1B3113)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, fontSize = 16.sp, color = Color(0xFF1B3113))
    }
}

@Composable
fun ErrorUI(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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
