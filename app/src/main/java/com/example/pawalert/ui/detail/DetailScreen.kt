package com.example.pawalert.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
                    uiState.report?.let { report ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🚨 PawAlert: ${report.problemTypeEnum().label} dog reported at ${report.address} (${if (report.landmark.isNotBlank()) "Near " + report.landmark + ", " else ""}GPS: ${report.location.latitude}, ${report.location.longitude}). Description: ${report.description}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Dog Alert"))
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (report.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = report.photoUrl,
                                contentDescription = "Photo of reported dog",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = Amber40,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Photo not provided",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

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

                        // Description Card
                        Text(
                            text = "Situation Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Amber40
                        )

                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp
                        )

                        // Reporter Info Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Amber40)
                                Text(
                                    text = "Reported by ${report.reporterName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Location Details Card
                        Text(
                            text = "Dog's Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Amber40
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Amber40,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = report.address.ifBlank { "Location captured" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (report.landmark.isNotBlank()) {
                                            Surface(
                                                color = Amber40.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "📍 Landmark: ${report.landmark}",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Amber80,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "GPS: %.5f, %.5f".format(report.location.latitude, report.location.longitude),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Embedded Google Map
                                val dogPosition = LatLng(report.location.latitude, report.location.longitude)
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(dogPosition, 16f)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    try {
                                        GoogleMap(
                                            modifier = Modifier.fillMaxSize(),
                                            cameraPositionState = cameraPositionState,
                                            uiSettings = MapUiSettings(
                                                zoomControlsEnabled = true,
                                                myLocationButtonEnabled = false
                                            )
                                        ) {
                                            Marker(
                                                state = rememberMarkerState(position = dogPosition),
                                                title = report.problemTypeEnum().label,
                                                snippet = report.address
                                            )
                                        }
                                    } catch (_: Throwable) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Map preview unavailable")
                                        }
                                    }
                                }

                                // Open Navigation in Google Maps App
                                Button(
                                    onClick = {
                                        val uri = Uri.parse("geo:${report.location.latitude},${report.location.longitude}?q=${report.location.latitude},${report.location.longitude}(PawAlert+Dog)")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {
                                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${report.location.latitude},${report.location.longitude}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber40)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Turn-by-Turn Navigation", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Feeder Helper Action Section
                        val isActionLoading = uiState.actionState is DetailActionState.Loading

                        when (statusEnum) {
                            ReportStatus.OPEN -> {
                                Button(
                                    onClick = { viewModel.claimReport(report.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusInProgress),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isActionLoading
                                ) {
                                    if (isActionLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("🐾 I'll Help This Dog", fontWeight = FontWeight.Bold)
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
                                                .height(52.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusResolved),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isActionLoading
                                        ) {
                                            if (isActionLoading) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                            } else {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Mark as Fed / Rescued / Resolved", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.unclaimReport(report.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !isActionLoading
                                        ) {
                                            Text("Cancel / Release for another feeder")
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = StatusInProgress.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = StatusInProgress)
                                            Text(
                                                text = "Currently being handled by ${report.helperName ?: "a nearby feeder"}",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                            ReportStatus.RESOLVED -> {
                                Surface(
                                    color = StatusResolved.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusResolved)
                                        Text(
                                            text = "This dog alert has been safely resolved! 🎉",
                                            fontWeight = FontWeight.Bold,
                                            color = StatusResolved
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
