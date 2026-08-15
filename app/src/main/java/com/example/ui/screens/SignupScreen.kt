package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudihubViewModel

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}$".toRegex()
    if (!trimmed.matches(emailRegex)) return false
    val domainParts = trimmed.split("@").lastOrNull()?.split(".")
    if (domainParts == null || domainParts.size < 2) return false
    val tld = domainParts.last().lowercase()
    val commonTlds = setOf("com", "net", "org", "edu", "gov", "io", "co", "bd", "uk", "ca", "in", "app", "dev", "me", "info", "xyz", "live")
    return tld in commonTlds || tld.length >= 2
}

fun checkPasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    val hasLetters = password.any { it.isLetter() }
    val hasDigits = password.any { it.isDigit() }
    val hasSpecialOrUpper = password.any { !it.isLetterOrDigit() || it.isUpperCase() }
    
    return when {
        password.length >= 8 && hasLetters && hasDigits && hasSpecialOrUpper -> PasswordStrength.STRONG
        password.length >= 6 && hasLetters && hasDigits -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

enum class SignupScreenMode {
    YOUTUBE_BIND,
    CREATE_ACCOUNT,
    OTP_VERIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentMode by remember { mutableStateOf(SignupScreenMode.YOUTUBE_BIND) }

    // Account creation state fields
    var emailText by remember { mutableStateOf("") }
    var usernameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Date of birth
    var dobDay by remember { mutableStateOf("") }
    var dobMonth by remember { mutableStateOf("") }
    var dobYear by remember { mutableStateOf("") }

    // OTP
    var otpCode by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    BackHandler {
        when (currentMode) {
            SignupScreenMode.YOUTUBE_BIND -> viewModel.showSignupScreen = false
            SignupScreenMode.CREATE_ACCOUNT -> currentMode = SignupScreenMode.YOUTUBE_BIND
            SignupScreenMode.OTP_VERIFY -> currentMode = SignupScreenMode.CREATE_ACCOUNT
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentMode) {
                            SignupScreenMode.YOUTUBE_BIND -> "YouTube Bind"
                            SignupScreenMode.CREATE_ACCOUNT -> "Create Account"
                            SignupScreenMode.OTP_VERIFY -> "Verify Email OTP"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentMode) {
                                SignupScreenMode.YOUTUBE_BIND -> viewModel.showSignupScreen = false
                                SignupScreenMode.CREATE_ACCOUNT -> currentMode = SignupScreenMode.YOUTUBE_BIND
                                SignupScreenMode.OTP_VERIFY -> currentMode = SignupScreenMode.CREATE_ACCOUNT
                            }
                        },
                        modifier = Modifier.testTag("signup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentMode) {
                    // ==========================================
                    // MODE 1: YOUTUBE BIND SCREEN
                    // ==========================================
                    SignupScreenMode.YOUTUBE_BIND -> {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hero Bind Connection (CloudeHub <--> YouTube)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CloudeHub Circular Logo
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFF0284C7), CircleShape)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter("https://i.postimg.cc/c4jdZGk4/Cloude-Hub.png"),
                                    contentDescription = "CloudeHub Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Binding",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(34.dp)
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            // YouTube Circular Logo
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFFFF0000), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter("https://i.postimg.cc/V6d2VyZH/Snapchat.jpg"),
                                    contentDescription = "YouTube Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Sign in with YouTube",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Bind your YouTube account to unlock seamless cloud syncing, region-free proxy backups, and super-fast offline download utilities!",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Huge Red YouTube Bind Button
                        Button(
                            onClick = {
                                viewModel.signInWithGoogle(
                                    name = "YT Bound User",
                                    email = "ytuser@cloudihub.app",
                                    photo = "https://i.postimg.cc/V6d2VyZH/Snapchat.jpg",
                                    token = "yt_oauth_token_bound_2026"
                                )
                                Toast.makeText(context, "YouTube Account Bound Successfully!", Toast.LENGTH_SHORT).show()
                                viewModel.showSignupScreen = false
                                viewModel.showPrimeBadgesDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "YT Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bind CloudeHub YT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slim full width Create Account Option Button
                        OutlinedButton(
                            onClick = { currentMode = SignupScreenMode.CREATE_ACCOUNT },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFE0F2FE).copy(alpha = 0.5f),
                                contentColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Create Account",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Don't have YouTube? Create Account",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }

                    // ==========================================
                    // MODE 2: CREATE ACCOUNT SCREEN
                    // ==========================================
                    SignupScreenMode.CREATE_ACCOUNT -> {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Top Header Branding: Small CloudeHub Logo + "Joined CloudeHub Today"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFF0284C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter("https://i.postimg.cc/c4jdZGk4/Cloude-Hub.png"),
                                    contentDescription = "CloudeHub Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Joined CloudeHub Today",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Slim & Long Email Input Box (Full width)
                        Text(
                            text = "Email Address",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )
                        SlimTextField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            placeholder = "Enter your email address",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        // Dynamic iOS-style fields when email is typed
                        AnimatedVisibility(
                            visible = emailText.trim().isNotEmpty(),
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Slim Username Box
                                Column {
                                    Text(
                                        text = "Username",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    SlimTextField(
                                        value = usernameText,
                                        onValueChange = { usernameText = it },
                                        placeholder = "Choose a username",
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Username",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }

                                // Slim Password Box
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Password",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        if (passwordText.isNotEmpty()) {
                                            val strength = checkPasswordStrength(passwordText)
                                            val (strengthText, strengthColor) = when (strength) {
                                                PasswordStrength.WEAK -> "Weak" to Color(0xFFEF4444)
                                                PasswordStrength.MEDIUM -> "Medium" to Color(0xFFF59E0B)
                                                PasswordStrength.STRONG -> "Strong" to Color(0xFF10B981)
                                            }
                                            Text(
                                                text = "Strength: $strengthText",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = strengthColor,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                    SlimTextField(
                                        value = passwordText,
                                        onValueChange = { passwordText = it },
                                        placeholder = "Enter password (min 6 chars, letters & numbers)",
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { isPasswordVisible = !isPasswordVisible },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle Password",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    )
                                    Text(
                                        text = "Must contain at least 6 characters with both letters and numbers",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                    )
                                }

                                // Date of Birth (3 Small Boxes side-by-side)
                                Column {
                                    Text(
                                        text = "Date of Birth",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SlimTextField(
                                            value = dobDay,
                                            onValueChange = { if (it.length <= 2) dobDay = it },
                                            placeholder = "DD",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SlimTextField(
                                            value = dobMonth,
                                            onValueChange = { if (it.length <= 2) dobMonth = it },
                                            placeholder = "MM",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SlimTextField(
                                            value = dobYear,
                                            onValueChange = { if (it.length <= 4) dobYear = it },
                                            placeholder = "YYYY",
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Sign Up Button
                                Button(
                                    onClick = {
                                        // 1. Strict Email Verification
                                        if (!isValidEmail(emailText)) {
                                            Toast.makeText(
                                                context,
                                                "Invalid email address! Please enter a real email (e.g. name@gmail.com)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }

                                        // 2. Medium or Strong Password Verification
                                        val pStrength = checkPasswordStrength(passwordText)
                                        if (pStrength == PasswordStrength.WEAK) {
                                            Toast.makeText(
                                                context,
                                                "Weak password! Must be at least 6 characters with letters and numbers.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }

                                        // 3. Date of birth check
                                        if (dobDay.isBlank() || dobMonth.isBlank() || dobYear.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Please enter your complete Date of Birth (DD/MM/YYYY)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }

                                        otpCode = "749201" // Simulated auto OTP code
                                        currentMode = SignupScreenMode.OTP_VERIFY
                                        Toast.makeText(context, "OTP sent to ${emailText.trim()}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "Sign Up",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // "or" divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "or",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2 Slim & Long Social Buttons (Google & Facebook)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Continue with Google
                            OutlinedButton(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = "Google User",
                                        email = "google.user@gmail.com",
                                        photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        token = "google_oauth_token_2026"
                                    )
                                    Toast.makeText(context, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                                    viewModel.showSignupScreen = false
                                    viewModel.showPrimeBadgesDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFEA4335)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continue with Google",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            // 2. Continue with Facebook
                            Button(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = "Facebook User",
                                        email = "fb.user@facebook.com",
                                        photo = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                                        token = "fb_token_2026"
                                    )
                                    Toast.makeText(context, "Signed in with Facebook!", Toast.LENGTH_SHORT).show()
                                    viewModel.showSignupScreen = false
                                    viewModel.showPrimeBadgesDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1877F2),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "f",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continue with Facebook",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // MODE 3: OTP VERIFICATION
                    // ==========================================
                    SignupScreenMode.OTP_VERIFY -> {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "OTP Email",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Enter OTP Code",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "We sent a 6-digit verification code to ${emailText.ifBlank { "your email" }}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Slim OTP Input Box
                        SlimTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            placeholder = "Enter 6-digit OTP code",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Text(
                                    text = "Paste",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val clipStr = clipboardManager.getText()?.text
                                            if (!clipStr.isNullOrEmpty()) {
                                                otpCode = clipStr
                                            } else {
                                                otpCode = "749201"
                                            }
                                            Toast.makeText(context, "OTP Code Pasted!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Verify & Create Account Button
                        Button(
                            onClick = {
                                if (otpCode.isBlank()) {
                                    Toast.makeText(context, "Please enter OTP code", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val finalName = usernameText.ifBlank { emailText.substringBefore("@").ifBlank { "New User" } }
                                viewModel.signInWithGoogle(
                                    name = finalName,
                                    email = emailText.ifBlank { "user@cloudihub.app" },
                                    photo = "https://i.postimg.cc/c4jdZGk4/Cloude-Hub.png",
                                    token = "manual_signup_token_2026"
                                )
                                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                viewModel.showSignupScreen = false
                                viewModel.showPrimeBadgesDialog = true // Trigger paid service / badge shop popup
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Verify & Create Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Slim iOS-style TextField Composable
@Composable
fun SlimTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        textStyle = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    )
}

@Composable
private fun BindFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
