package com.theo.forest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theo.forest.R
import com.theo.forest.data.modal.DiseaseInfo
import com.theo.forest.data.modal.Response
import com.theo.forest.ui.theme.ForestTheme
import com.theo.forest.ui.viewmodals.HomeViewModal

@Composable
fun DetailScreen(
    viewModal: HomeViewModal = hiltViewModel(),
    backPress : () -> Unit
) {
    val scrollState = rememberScrollState()
    val mlResult = viewModal.result.value
    val diseaseInfoState by viewModal.diseaseInfo.collectAsState()



    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { backPress() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Image(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "back",
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                }
                Text("Scan Result", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { /* Share action */ },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share_24),
                        contentDescription = "share",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            // Image Card
            if (viewModal.pickedImage.value != null) {
                Image(
                    bitmap = viewModal.pickedImage.value!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.leaf),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disease Name & Severity Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mlResult.disease,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Confidence Score
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Accuracy: ${(mlResult.confidence * 100).toInt()}%",
                fontSize = 16.sp,
                color = Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 15.dp))

            // Information Sections from AI
            when (val res = diseaseInfoState) {
                is Response.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is Response.Success -> {
                    val info = res.result
                    if (info is DiseaseInfo) {
                        InfoSection(title = "Description", content = info.description)
                        InfoSection(title = "Symptoms", content = info.symptoms)
                        InfoSection(title = "Causes", content = info.causes)
                        InfoSection(title = "Treatment", content = info.treatment)
                        InfoSection(title = "Prevention", content = info.prevention)
                    }
                }
                is Response.Error -> {
                    Text(
                        text = "Could not load additional info: ${res.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disclaimer
            Text(
                text = "Note: AI identifications can be incorrect. Consult a specialist for critical farming decisions.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 20.dp),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 22.sp,
        )
    }
}

//@Preview(showSystemUi = true)
//@Composable
//fun DetailScreenPreview() {
//    ForestTheme(dynamicColor = false, darkTheme = false) {
//        DetailScreen()
//    }
//}
