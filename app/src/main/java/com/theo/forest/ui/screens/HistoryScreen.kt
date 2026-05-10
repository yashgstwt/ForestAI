package com.theo.forest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.theo.forest.R
import com.theo.forest.data.modal.Response
import com.theo.forest.data.modal.ScanRecord
import com.theo.forest.ui.viewmodals.HomeViewModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModal: HomeViewModal,
    navToDetail: () -> Unit,
    navToWeather: () -> Unit,
    navToHome: () -> Unit
) {
    val userScansState by viewModal.userScans.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan History", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            ForestBottomBar(
                currentScreen = "history",
                onHomeClick = navToHome,
                onWeatherClick = navToWeather,
                onHistoryClick = {} // Already here
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = userScansState) {
                is Response.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Response.Success -> {
                    val scans = state.result
                    if (scans.isEmpty()) {
                        Text(
                            "No history found",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(scans) { record ->
                                HistoryItem(
                                    record = record,
                                    onClick = {
                                        viewModal.selectScan(record)
                                        navToDetail()
                                    },
                                    onDelete = { record.id?.let { viewModal.deleteScan(it) } }
                                )
                            }
                        }
                    }
                }
                is Response.Error -> {
                    Text(
                        "Error: ${state.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: ScanRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = record.image_url,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.leaf),
                error = painterResource(R.drawable.leaf)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.disease,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Confidence: ${"%.2f".format(record.confidence * 100)}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                record.created_at?.let {
                    Text(
                        text = it.take(10), // Simple date display
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
