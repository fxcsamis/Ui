package com.example.ui.screens

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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.CloudihubViewModel
import com.example.ui.components.CloudShape

enum class SignupStep {
    CHOOSE_METHOD,
    ENTER_EMAIL,
    VERIFY_CODE,
    USER_DETAILS,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    viewModel: CloudihubViewModel,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(SignupStep.CHOOSE_METHOD) }
    var emailInput by remember { mutableStateOf("") }
    var verificationCodeInput by remember { mutableStateOf("") }
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var fullNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var detailsError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val annotatedText = remember {
        buildAnnotatedString {
            append("If you do not wish to bind an account, you can ")
            pushStringAnnotation(tag = "SIGNUP", annotation = "signup")
            withStyle(style = SpanStyle(color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                append("sign up")
            }
            pop()
            append(", or ")
            pushStringAnnotation(tag = "LOGIN", annotation = "login")
            withStyle(style = SpanStyle(color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                append("log in")
            }
            pop()
            append(" if you already have an account.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isLoginMode) "Sign In" else when (currentStep) {
                            SignupStep.CHOOSE_METHOD -> "YouTube Bind"
                            SignupStep.ENTER_EMAIL -> "Create Account"
                            SignupStep.VERIFY_CODE -> "Enter OTP Code"
                            SignupStep.USER_DETAILS -> "Complete Profile"
                            SignupStep.SUCCESS -> "Registration Successful"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isLoginMode) {
                                isLoginMode = false
                                currentStep = SignupStep.CHOOSE_METHOD
                            } else {
                                when (currentStep) {
                                    SignupStep.CHOOSE_METHOD -> viewModel.showSignupScreen = false
                                    SignupStep.ENTER_EMAIL -> currentStep = SignupStep.CHOOSE_METHOD
                                    SignupStep.VERIFY_CODE -> currentStep = SignupStep.ENTER_EMAIL
                                    SignupStep.USER_DETAILS -> currentStep = SignupStep.VERIFY_CODE
                                    SignupStep.SUCCESS -> viewModel.showSignupScreen = false
                                }
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
        containerColor = Color.Transparent,
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
                .padding(horizontal = 24.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "SignupStepTransition"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoginMode) {
                        // ==========================================
                        // SIGN IN SCREEN LAYOUT (Mockup 1)
                        // ==========================================
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFFBAE6FD), CircleShape)
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
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Column {
                                Text(
                                    text = "CloudeHub",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Welcome Back",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Sign In",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Hi! Welcome back, you've been missed",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CustomLabeledTextField(
                                label = "Email",
                                value = emailInput,
                                onValueChange = { emailInput = it; detailsError = null },
                                placeholder = "example@gmail.com",
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            CustomLabeledTextField(
                                label = "Password",
                                value = passwordInput,
                                onValueChange = { passwordInput = it; detailsError = null },
                                placeholder = "••••••••••••",
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { /* Simulate recovery */ }
                            )
                        }

                        if (detailsError != null) {
                            Text(
                                text = detailsError ?: "",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sign In button
                        Button(
                            onClick = {
                                if (emailInput.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                    detailsError = "Please enter a valid email address"
                                } else if (passwordInput.isEmpty()) {
                                    detailsError = "Please enter your password"
                                } else {
                                    detailsError = null
                                    
                                    // Save password for secure biometric verification fallback
                                    val sp = viewModel.getApplication<android.app.Application>().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                    sp.edit().putString("local_account_password", passwordInput).apply()

                                    viewModel.signInWithGoogle(
                                        name = "Alex Cloudborn",
                                        email = emailInput,
                                        photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                        token = "manual_signin_token_simulated_success"
                                    )
                                    currentStep = SignupStep.SUCCESS
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Sign In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Or separator line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text(
                                text = "Or sign in with",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Circular Social logins (Google, Facebook, Apple)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularSocialButton(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = fullNameInput.ifEmpty { "Alex Cloudborn" },
                                        email = emailInput.ifEmpty { "google.user@gmail.com" },
                                        photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                        token = "google-simulated-success"
                                    )
                                    currentStep = SignupStep.SUCCESS
                                },
                                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/Google_%22G%22_logo.svg/512px-Google_%22G%22_logo.svg.png",
                                borderColor = Color(0xFFF1F5F9)
                            )
                            CircularSocialButton(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = fullNameInput.ifEmpty { "Facebook Friend" },
                                        email = emailInput.ifEmpty { "fb.user@facebook.com" },
                                        photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                        token = "fb-simulated-success"
                                    )
                                    currentStep = SignupStep.SUCCESS
                                },
                                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Facebook_f_logo_%282019%29.svg/512px-Facebook_f_logo_%282019%29.svg.png",
                                borderColor = Color(0xFFF1F5F9)
                            )
                            CircularSocialButton(
                                onClick = {
                                    viewModel.signInWithGoogle(
                                        name = fullNameInput.ifEmpty { "Apple User" },
                                        email = emailInput.ifEmpty { "apple.user@icloud.com" },
                                        photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                        token = "apple-simulated-success"
                                    )
                                    currentStep = SignupStep.SUCCESS
                                },
                                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Apple_logo_black.svg/512px-Apple_logo_black.svg.png",
                                borderColor = Color(0xFFF1F5F9)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Switch to sign up link (points back to YouTube Bind screen first!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Sign Up",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable {
                                    isLoginMode = false
                                    currentStep = SignupStep.CHOOSE_METHOD
                                    detailsError = null
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Continue as Guest link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue as a guest",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable {
                                    viewModel.showSignupScreen = false
                                }
                            )
                        }
                    } else {
                        when (step) {
                            SignupStep.CHOOSE_METHOD -> {
                                // ==========================================
                                // YOUTUBE BIND SCREEN (Initial Signup Screen)
                                // ==========================================
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Hero Bind Connection (CloudeHub <--> YouTube)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // CloudeHub Circular Logo
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .shadow(6.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(2.5.dp, Color(0xFF0284C7), CircleShape)
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

                                    Spacer(modifier = Modifier.width(24.dp))

                                    // Double-headed Sync Arrow
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Binding",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(38.dp)
                                    )

                                    Spacer(modifier = Modifier.width(24.dp))

                                    // YouTube Circular Logo
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .shadow(6.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(2.5.dp, Color(0xFFFF0000), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter("https://i.postimg.cc/V6d2VyZH/Snapchat.jpg"),
                                            contentDescription = "YouTube Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Sign in with YouTube",
                                    fontSize = 24.sp,
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
                                        // Simulate YT bind success, go directly to Details or Verify
                                        currentStep = SignupStep.ENTER_EMAIL
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
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

                                Spacer(modifier = Modifier.height(20.dp))

                                // The custom annotated footer text
                                ClickableText(
                                    text = annotatedText,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    onClick = { offset ->
                                        annotatedText.getStringAnnotations(tag = "SIGNUP", start = offset, end = offset)
                                            .firstOrNull()?.let {
                                                currentStep = SignupStep.ENTER_EMAIL
                                            }
                                        annotatedText.getStringAnnotations(tag = "LOGIN", start = offset, end = offset)
                                            .firstOrNull()?.let {
                                                isLoginMode = true
                                            }
                                    }
                                )

                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            SignupStep.ENTER_EMAIL -> {
                                // ==========================================
                                // CREATE ACCOUNT SCREEN LAYOUT (Mockup 2)
                                // ==========================================
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .shadow(16.dp, RoundedCornerShape(28.dp))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.93f)
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(2.dp, Color(0xFFBAE6FD), CircleShape)
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
                                            
                                            Spacer(modifier = Modifier.width(10.dp))
                                            
                                            Column {
                                                Text(
                                                    text = "CloudeHub",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "Joined today",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0284C7)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Create Account",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )

                                        Text(
                                            text = "Fill your information below or register with your social account.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                        )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CustomLabeledTextField(
                                        label = "Name",
                                        value = fullNameInput,
                                        onValueChange = { fullNameInput = it; detailsError = null },
                                        placeholder = "John Doe",
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8))
                                        }
                                    )

                                    CustomLabeledTextField(
                                        label = "Email",
                                        value = emailInput,
                                        onValueChange = { emailInput = it; detailsError = null },
                                        placeholder = "example@gmail.com",
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8))
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    CustomLabeledTextField(
                                        label = "Password",
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it; detailsError = null },
                                        placeholder = "••••••••••••",
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                    tint = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Agree to terms checkbox
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { agreeToTerms = !agreeToTerms },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = agreeToTerms,
                                        onCheckedChange = { agreeToTerms = it; detailsError = null },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                                    )
                                    Text(
                                        text = "Agree with Terms & Condition",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }

                                if (detailsError != null) {
                                    Text(
                                        text = detailsError ?: "",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (fullNameInput.isEmpty()) {
                                            detailsError = "Please enter your Name"
                                        } else if (emailInput.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                            detailsError = "Please enter a valid email address"
                                        } else if (passwordInput.length < 6) {
                                            detailsError = "Password must be at least 6 characters"
                                        } else if (!agreeToTerms) {
                                            detailsError = "You must agree to the Terms & Conditions"
                                        } else {
                                            detailsError = null
                                            currentStep = SignupStep.VERIFY_CODE
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Sign Up",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                    Text(
                                        text = "Or sign up with",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularSocialButton(
                                        onClick = {
                                            viewModel.signInWithGoogle(
                                                name = fullNameInput.ifEmpty { "Alex Cloudborn" },
                                                email = emailInput.ifEmpty { "google.user@gmail.com" },
                                                photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                                token = "google-simulated-success"
                                            )
                                            currentStep = SignupStep.SUCCESS
                                        },
                                        iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/Google_%22G%22_logo.svg/512px-Google_%22G%22_logo.svg.png",
                                        borderColor = Color(0xFFF1F5F9)
                                    )
                                    CircularSocialButton(
                                        onClick = {
                                            viewModel.signInWithGoogle(
                                                name = fullNameInput.ifEmpty { "Facebook Friend" },
                                                email = emailInput.ifEmpty { "fb.user@facebook.com" },
                                                photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                                token = "fb-simulated-success"
                                            )
                                            currentStep = SignupStep.SUCCESS
                                        },
                                        iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Facebook_f_logo_%282019%29.svg/512px-Facebook_f_logo_%282019%29.svg.png",
                                        borderColor = Color(0xFFF1F5F9)
                                    )
                                    CircularSocialButton(
                                        onClick = {
                                            viewModel.signInWithGoogle(
                                                name = fullNameInput.ifEmpty { "Apple User" },
                                                email = emailInput.ifEmpty { "apple.user@icloud.com" },
                                                photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                                token = "apple-simulated-success"
                                            )
                                            currentStep = SignupStep.SUCCESS
                                        },
                                        iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Apple_logo_black.svg/512px-Apple_logo_black.svg.png",
                                        borderColor = Color(0xFFF1F5F9)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Already have an account? ",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Sign In",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.clickable {
                                            isLoginMode = true
                                            detailsError = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                            SignupStep.VERIFY_CODE -> {
                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "OTP Verification",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Verification Code Sent!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Please enter the 6-digit validation OTP code sent to $emailInput",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                            )

                            OutlinedTextField(
                                value = verificationCodeInput,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        verificationCodeInput = it
                                        codeError = null
                                    }
                                },
                                label = { Text("6-Digit Code") },
                                placeholder = { Text("E.g. 123456") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                isError = codeError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0284C7),
                                    focusedLabelColor = Color(0xFF0284C7)
                                )
                            )

                            if (codeError != null) {
                                Text(
                                    text = codeError ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Didn't receive the email? Tap to resend.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier
                                    .clickable {
                                        verificationCodeInput = ""
                                        codeError = null
                                    }
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (verificationCodeInput.length != 6) {
                                        codeError = "Verification code must be exactly 6 digits"
                                    } else {
                                        // Go to details step
                                        currentStep = SignupStep.USER_DETAILS
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Verify Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        SignupStep.USER_DETAILS -> {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tell us about yourself",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Set up your credentials to secure your cloud backups.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // First Name
                                OutlinedTextField(
                                    value = firstNameInput,
                                    onValueChange = { firstNameInput = it; detailsError = null },
                                    label = { Text("First Name") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        focusedLabelColor = Color(0xFF0284C7)
                                    )
                                )

                                // Last Name
                                OutlinedTextField(
                                    value = lastNameInput,
                                    onValueChange = { lastNameInput = it; detailsError = null },
                                    label = { Text("Last Name") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF64748B))
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        focusedLabelColor = Color(0xFF0284C7)
                                    )
                                )

                                // Username
                                OutlinedTextField(
                                    value = usernameInput,
                                    onValueChange = { usernameInput = it; detailsError = null },
                                    label = { Text("Username") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF64748B))
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        focusedLabelColor = Color(0xFF0284C7)
                                    )
                                )

                                // Password
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it; detailsError = null },
                                    label = { Text("Password") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Password, contentDescription = null, tint = Color(0xFF64748B))
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        focusedLabelColor = Color(0xFF0284C7)
                                    )
                                )
                            }

                            if (detailsError != null) {
                                Text(
                                    text = detailsError ?: "",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (firstNameInput.isEmpty() || lastNameInput.isEmpty() || usernameInput.isEmpty() || passwordInput.isEmpty()) {
                                        detailsError = "Please fill in all requested profile fields"
                                    } else if (passwordInput.length < 6) {
                                        detailsError = "Password must be at least 6 characters"
                                    } else {
                                        // Complete signup registration flow!
                                        val sp = viewModel.getApplication<android.app.Application>().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putString("local_account_password", passwordInput).apply()

                                        viewModel.signInWithGoogle(
                                            name = "$firstNameInput $lastNameInput",
                                            email = emailInput,
                                            photo = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100",
                                            token = "manual_signup_token_simulated_success"
                                        )
                                        currentStep = SignupStep.SUCCESS
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Complete Sign Up",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        SignupStep.SUCCESS -> {
                            Spacer(modifier = Modifier.height(32.dp))

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CloudShape())
                                    .background(Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Welcome to Cloudihub!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "Account registered successfully and bound safely with all YouTube and cloud features active.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.showSignupScreen = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Launch My Cloudihub Feed",
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
}
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155)
        )
    }
}

@Composable
fun SocialSignupButton(
    onClick: () -> Unit,
    logoText: String,
    logoColor: Color,
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (borderColor != Color.Transparent) 1.dp else 0.dp, RoundedCornerShape(16.dp))
            .border(
                width = if (borderColor != Color.Transparent) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (containerColor == Color.White) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = logoText,
                    color = logoColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(28.dp)) // Offset the left circle logo for perfect centering of text
        }
    }
}

@Composable
fun CustomLabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                focusedLabelColor = Color(0xFF0284C7),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            singleLine = true
        )
    }
}

@Composable
fun CircularSocialButton(
    onClick: () -> Unit,
    iconUrl: String,
    borderColor: Color,
    containerColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(iconUrl),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}
