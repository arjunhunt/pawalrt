package com.example.pawalert.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pawalert.data.ReportStatus
import com.example.pawalert.ui.theme.*
import com.example.pawalert.util.LocationHelper
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    reportId: String,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(reportId) {
        viewModel.loadReport(reportId)
    }

    LaunchedEffect(uiState.actionState) {
        if (uiState.actionState is DetailActionState.Error) {
            snackbarHostState.showSnackbar(
                (uiState.actionState as DetailActionState.Error).message
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Alert Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val report = uiState.report
                    if (report != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🚨 Stray Dog Alert (${report.problemTypeEnum().label}) near ${report.address}:\n${report.description}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Alert"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Amber40)
            }
        } else {
            val report = uiState.report
            if (report == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Report not found or has been removed.")
                }
            } else {
                val statusEnum = report.statusEnum()
                val statusColor = when (statusEnum) {
                    ReportStatus.OPEN -> StatusOpen
                    ReportStatus.IN_PROGRESS -> StatusInProgress
                    ReportStatus.RESOLVED -> StatusResolved
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero Image with Badges
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        AsyncImage(
                            model = report.photoUrl,
                            contentDescription = "Photo of reported dog",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Status Badge
                        Surface(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart),
                            color = statusColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusEnum.label.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category & Timestamp Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Amber80,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = report.problemTypeEnum().label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = Brown40,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Reported ${LocationHelper.formatTimeAgo(report.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Description
                        Text(
                            text = "Situation Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Brown40
                        )
                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Reporter Info
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Brown40)
                                Text(
                                    text = "Reported by ${report.reporterName.ifBlank { "Anonymous Reporter" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Helper Banner (if in progress)
                        if (statusEnum == ReportStatus.IN_PROGRESS) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = StatusInProgress.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VolunteerActivism,
                                        contentDescription = null,
                                        tint = StatusInProgress,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Being Handled",
                                            fontWeight = FontWeight.Bold,
                                            color = StatusInProgress
                                        )
                                        Text(
                                            text = "${report.helperName ?: "A feeder"} is currently assisting this dog.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Map & Location Section
                        Text(
                            text = "Dog's Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Brown40
                        )

                        Text(
                            text = report.address.ifBlank { "GPS Coordinates: ${report.location.latitude}, ${report.location.longitude}" },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Interactive Google Map Card
                        val dogPosition = LatLng(report.location.latitude, report.location.longitude)
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(dogPosition, 16f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = true,
                                    myLocationButtonEnabled = false
                                )
                            ) {
                                Marker(
                                    state = MarkerState(position = dogPosition),
                                    title = report.problemTypeEnum().label,
                                    snippet = report.address
                                )
                            }
                        }

                        // Open in Google Maps Button
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("geo:${report.location.latitude},${report.location.longitude}?q=${report.location.latitude},${report.location.longitude}(PawAlert+Dog)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback to browser map
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${report.location.latitude},${report.location.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Google Maps / Navigate")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions Area
                        val isActionLoading = uiState.actionState is DetailActionState.Loading

                        when (statusEnum) {
                            ReportStatus.OPEN -> {
                                Button(
                                    onClick = { viewModel.claimReport(report.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber40),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isActionLoading
                                ) {
                                    if (isActionLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("I'll help this dog", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            ReportStatus.IN_PROGRESS -> {
                                if (uiState.isClaimedByCurrentUser) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.markResolved(report.id) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusResolved),
                                            shape = RoundedCornerShape(14.dp),
                                            enabled = !isActionLoading
                                        ) {
                                            if (isActionLoading) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                            } else {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Mark as Resolved", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.unclaimReport(report.id) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            enabled = !isActionLoading
                                        ) {
                                            Text("Can't help anymore (Release Alert)")
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Amber80.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "A volunteer is currently attending to this dog. Thank you for checking!",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Brown40
                                        )
                                    }
                                }
                            }
                            ReportStatus.RESOLVED -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = StatusResolved.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusResolved,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Alert Resolved",
                                                fontWeight = FontWeight.Bold,
                                                color = StatusResolved,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "This dog received assistance and this case is closed.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
