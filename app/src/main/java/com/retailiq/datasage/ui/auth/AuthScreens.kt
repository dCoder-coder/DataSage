package com.retailiq.datasage.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthNavHost(navController: NavHostController, onFinish: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(viewModel) { hasToken, setupComplete -> navController.navigate(if (!hasToken) "login" else if (setupComplete) "done" else "setup") } }
        composable("login") { LoginScreen(viewModel, onRegister = { navController.navigate("register") }, onForgot = { navController.navigate("forgot") }, onLoginSuccess = { setupComplete -> navController.navigate(if (setupComplete) "done" else "setup") }) }
        composable("register") { RegisterScreen(viewModel) { mobile -> navController.navigate("otp/$mobile") } }
        composable("otp/{mobile}") { backStack -> OTPVerifyScreen(viewModel, backStack.arguments?.getString("mobile").orEmpty()) { setupComplete -> navController.navigate(if (setupComplete) "done" else "setup") } }
        composable("forgot") { ForgotPasswordScreen(viewModel) { mobile -> navController.navigate("reset/$mobile") } }
        composable("reset/{mobile}") { backStack -> ResetPasswordScreen(viewModel, backStack.arguments?.getString("mobile").orEmpty()) { navController.navigate("login") } }
        composable("setup") { SetupWizardScreen(viewModel) { navController.navigate("done") } }
        composable("done") { LaunchedEffect(Unit) { onFinish() } }
    }
}

@Composable
fun SplashScreen(viewModel: AuthViewModel, onRoute: (Boolean, Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onRoute(viewModel.hasToken(), viewModel.isSetupComplete())
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading DataSage...")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AuthViewModel, onRegister: () -> Unit, onForgot: () -> Unit, onLoginSuccess: (Boolean) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onForgot) { Text("Forgot Password?") } }
            Button(onClick = {
                if (!AuthValidation.isValidMobile(mobile)) {
                    scope.launch { snackbar.showSnackbar("Enter a valid 10-digit mobile number") }
                } else {
                    viewModel.login(mobile, password) { _ ->
                        onLoginSuccess(viewModel.isSetupComplete())
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
            TextButton(onClick = onRegister) { Text("Create account") }
            if (state is AuthUiState.Error) {
                LaunchedEffect(state) { scope.launch { snackbar.showSnackbar((state as AuthUiState.Error).message) } }
            }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onOtp: (String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(store, { store = it }, label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password (8+ chars, 1+ digit)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (AuthValidation.isValidMobile(mobile) && AuthValidation.isStrongPassword(password)) {
                viewModel.register(fullName, mobile, store, password) { onOtp(mobile) }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
    }
}

@Composable
fun OTPVerifyScreen(viewModel: AuthViewModel, mobile: String, onDone: (Boolean) -> Unit) {
    var otp by remember { mutableStateOf("") }
    val seconds by viewModel.otpSecondsRemaining.collectAsState()
    val resendCount by viewModel.resendCount.collectAsState()
    LaunchedEffect(Unit) { viewModel.startOtpCountdown() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verify OTP")
        OutlinedTextField(otp, { if (it.length <= 6) otp = it }, label = { Text("6-digit OTP") })
        Text("Time remaining: ${seconds}s")
        Button(onClick = { viewModel.verifyOtp(mobile, otp) { _, setupComplete -> onDone(setupComplete) } }, enabled = otp.length == 6) { Text("Verify") }
        TextButton(onClick = { viewModel.resendOtp(mobile) }, enabled = viewModel.canResendOtp()) { Text("Resend OTP (${3 - resendCount} left)") }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onOtpSent: (String) -> Unit) {
    var mobile by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Forgot Password")
        OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number") })
        Button(onClick = { viewModel.forgotPassword(mobile); onOtpSent(mobile) }) { Text("Send OTP") }
    }
}

@Composable
fun ResetPasswordScreen(viewModel: AuthViewModel, mobile: String, onDone: () -> Unit) {
    var otp by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Reset Password")
        OutlinedTextField(otp, { otp = it }, label = { Text("OTP") })
        OutlinedTextField(pwd, { pwd = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation())
        Button(onClick = { viewModel.resetPassword(mobile, otp, pwd) { onDone() } }) { Text("Reset") }
    }
}

@Composable
fun SetupWizardScreen(viewModel: AuthViewModel, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Setup Wizard (Step $step/4)")
        Text(
            when (step) {
                1 -> "Confirm store information"
                2 -> "Review categories"
                3 -> "Add first product (optional)"
                else -> "You're ready to go"
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step < 4) Button(onClick = { step += 1 }) { Text(if (step == 3) "Skip" else "Next") }
            if (step == 4) Button(onClick = { viewModel.completeSetup(); onComplete() }) { Text("Finish") }
        }
    }
}
