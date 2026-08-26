package com.example.pawalert.ui.report

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.pawalert.data.ProblemType
import com.example.pawalert.ui.theme.Amber40
import com.example.pawalert.ui.theme.Brown40
import com.example.pawalert.ui.theme.StatusOpen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    onReportSubmitted: () -> Unit,
    viewModel: ReportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri)
        }
    }

    // Camera Capture launcher
    var cameraUriTarget by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            viewModel.onCameraPhotoCaptured()
        }
    }

    // Trigger initial location fetch if permission granted
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted && uiState.latitude == null) {
            viewModel.fetchCurrentLocation()
        }
    }

    // Handle Submission State changes
    LaunchedEffect(uiState.submissionState) {
        when (val state = uiState.submissionState) {
            is ReportSubmissionState.Success -> {
                onReportSubmitted()
            }
            is ReportSubmissionState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.dismissError()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Report Dog Alert",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Photo Capture / Picker
            Text(
                text = "1. Dog Photo *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brown40
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (uiState.selectedPhotoUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = uiState.selectedPhotoUri,
                            contentDescription = "Selected dog photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (cameraPermissionState.status == PermissionStatus.Granted) {
                                        val uri = viewModel.prepareCameraUri()
                                        cameraUriTarget = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                },
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Retake", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = Amber40,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add a photo to help feeders find the dog",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (cameraPermissionState.status == PermissionStatus.Granted) {
                                        val uri = viewModel.prepareCameraUri()
                                        cameraUriTarget = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Amber40)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera")
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }
            }

            // Section 2: Problem Category
            Text(
                text = "2. Category of Need *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brown40
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ProblemType.values()) { category ->
                    val isSelected = uiState.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.label) },
                        leadingIcon = {
                            val icon = when (category) {
                                ProblemType.HUNGRY -> Icons.Default.Restaurant
                                ProblemType.INJURED -> Icons.Default.Healing
                                ProblemType.SICK -> Icons.Default.MedicalServices
                                ProblemType.STUCK -> Icons.Default.Warning
                                ProblemType.AGGRESSIVE -> Icons.Default.Report
                                ProblemType.LOST -> Icons.Default.Search
                                ProblemType.NEWBORN_LITTER -> Icons.Default.Favorite
                                ProblemType.OTHER -> Icons.Default.Info
                            }
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Amber40,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Section 3: Description
            Text(
                text = "3. Description *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brown40
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                placeholder = {
                    Text("Describe color, collar, condition, landmarks, or immediate dangers...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Amber40,
                    cursorColor = Amber40
                )
            )

            // Section 4: Location Capture
            Text(
                text = "4. Dog's Location *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brown40
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StatusOpen
                        )
                        Text(
                            text = if (uiState.address.isNotBlank()) uiState.address else "No location captured yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.latitude != null && uiState.longitude != null) {
                        Text(
                            text = "GPS: %.5f, %.5f".format(uiState.latitude, uiState.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (locationPermissionsState.allPermissionsGranted) {
                                viewModel.fetchCurrentLocation()
                            } else {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Brown40),
                        enabled = !uiState.isFetchingLocation
                    ) {
                        if (uiState.isFetchingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting GPS...")
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.latitude != null) "Refresh Location" else "Get Current Location")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Submit Button
            val isSubmitting = uiState.submissionState is ReportSubmissionState.UploadingPhoto ||
                    uiState.submissionState is ReportSubmissionState.Submitting

            Button(
                onClick = viewModel::submitReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber40,
                    disabledContainerColor = Amber40.copy(alpha = 0.5f)
                ),
                enabled = uiState.isFormValid && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.submissionState is ReportSubmissionState.UploadingPhoto)
                            "Uploading Photo..." else "Broadcasting Alert...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Alert", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
