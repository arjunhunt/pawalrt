package com.example.pawalert.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pawalert.ui.theme.Amber40
import com.example.pawalert.ui.theme.Amber80
import com.example.pawalert.ui.theme.Brown40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var useEmailAuth by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Error) {
            snackbarHostState.showSnackbar(
                (uiState.authState as AuthState.Error).message
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = { Text("Profile & Account") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Logo & Header
            Surface(
                color = Amber80,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = Amber40,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = "PawAlert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Amber40
            )

            Text(
                text = "Connecting local dog lovers and feeders with stray dogs needing food, medicine, and rescue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check if already signed in
            if (uiState.currentUser != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Signed In Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Brown40
                        )

                        Text(
                            text = "Feeder ID: ${uiState.currentUser?.uid?.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.displayName,
                            onValueChange = viewModel::onDisplayNameChanged,
                            label = { Text("Display Name / Feeder Handle") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.updateDisplayName(uiState.displayName) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brown40)
                        ) {
                            Text("Update Name")
                        }

                        Button(
                            onClick = onAuthSuccess,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Amber40)
                        ) {
                            Text("Go to Alert Feed", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            } else {
                // Onboarding & Login Forms
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!useEmailAuth) {
                            // Quick Feeder / Reporter Setup
                            Text(
                                text = "Quick Community Sign-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Brown40
                            )

                            Text(
                                text = "Enter your name to start reporting dogs or claiming feeding alerts right away.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = viewModel::onDisplayNameChanged,
                                label = { Text("Your Name / Feeder Nickname") },
                                placeholder = { Text("e.g. Sarah J. (Dog Volunteer)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Amber40)
                                }
                            )

                            Button(
                                onClick = {
                                    viewModel.continueAsCommunityFeeder(uiState.displayName.ifBlank { "Community Feeder" })
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Amber40),
                                shape = RoundedCornerShape(12.dp),
                                enabled = uiState.authState !is AuthState.Loading
                            ) {
                                if (uiState.authState is AuthState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Pets, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter as Community Feeder", fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = { useEmailAuth = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Or sign in with Email & Password")
                            }
                        } else {
                            // Email / Password Form
                            Text(
                                text = if (uiState.isSignUp) "Create Feeder Account" else "Feeder Sign-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Brown40
                            )

                            if (uiState.isSignUp) {
                                OutlinedTextField(
                                    value = uiState.displayName,
                                    onValueChange = viewModel::onDisplayNameChanged,
                                    label = { Text("Your Full Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = viewModel::onEmailChanged,
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = viewModel::onPasswordChanged,
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Button(
                                onClick = { viewModel.authenticateWithEmail() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Amber40),
                                shape = RoundedCornerShape(12.dp),
                                enabled = uiState.authState !is AuthState.Loading
                            ) {
                                if (uiState.authState is AuthState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = if (uiState.isSignUp) "Create Account" else "Sign In",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { viewModel.toggleAuthMode() }) {
                                    Text(if (uiState.isSignUp) "Existing user? Sign In" else "New? Register")
                                }
                                TextButton(onClick = { useEmailAuth = false }) {
                                    Text("Quick Sign-in")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
