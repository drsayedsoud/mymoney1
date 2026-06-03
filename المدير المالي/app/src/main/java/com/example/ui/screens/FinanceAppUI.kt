package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppUI(
    viewModel: FinanceViewModel,
    onTriggerVoiceSpeech: () -> Unit,
    onTriggerSystemUnlock: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val isSettingsUnlocked by viewModel.isSettingsUnlocked.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val lastActionResponse by viewModel.lastActionResponse.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val aiChatResponse by viewModel.aiChatResponse.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()

    LaunchedEffect(lastActionResponse) {
        lastActionResponse?.let {
            if (it.isNotEmpty()) onSpeak(it)
        }
    }

    LaunchedEffect(aiChatResponse) {
        aiChatResponse?.let {
            if (it.isNotEmpty()) onSpeak(it)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            if (it.isNotEmpty()) onSpeak(it)
        }
    }

    // Enforce Arabic RTL Layout Direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                if (isAppUnlocked) {
                    FinanceBottomNavBar(
                        currentTab = currentScreen,
                        onTabSelected = { tab ->
                            if (tab == "settings" && !isSettingsUnlocked) {
                                // Request system authentication before showing settings
                                onTriggerSystemUnlock()
                            }
                            viewModel.navigateTo(tab)
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isAppUnlocked) {
                    SecurityLockScreen(
                        onUnlockSuccess = { viewModel.setAppUnlocked(true) },
                        onTriggerSystemAuth = onTriggerSystemUnlock
                    )
                } else {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(
                                animationSpec = tween(220)
                            )
                        },
                        label = "screen_transition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            "home" -> HomeScreen(
                                viewModel = viewModel,
                                onTriggerVoiceSpeech = onTriggerVoiceSpeech,
                                isLoading = isLoading,
                                lastActionResponse = lastActionResponse,
                                aiChatResponse = aiChatResponse,
                                errorMessage = errorMessage,
                                inputText = inputText
                            )
                            "transactions" -> TransactionsListScreen(viewModel = viewModel)
                            "reports" -> ReportsScreen(viewModel = viewModel)
                            "settings" -> {
                                if (isSettingsUnlocked) {
                                    SettingsScreen(viewModel = viewModel)
                                } else {
                                    // Security overlay specifically for Settings
                                    SettingsSecurityFallback(
                                        onUnlockSuccess = { viewModel.setSettingsUnlocked(true) },
                                        onTriggerSystemAuth = onTriggerSystemUnlock
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
fun SecurityLockScreen(
    onUnlockSuccess: () -> Unit,
    onTriggerSystemAuth: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showPinError by remember { mutableStateOf(false) }

    // Trigger Native Authentication on Launch
    LaunchedEffect(Unit) {
        onTriggerSystemAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "قفل",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "تطبيق المدير المالي محمي",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "يرجى مسح البصمة أو تأكيد هوية الهاتف للدخول، أو استخدم رمز PIN الاحتياطي (1234) للتجربة السريعة.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Custom PIN backup interface for seamless preview testing!
        OutlinedTextField(
            value = enteredPin,
            onValueChange = {
                if (it.length <= 4) {
                    enteredPin = it
                    showPinError = false
                    if (it == "1234") {
                        onUnlockSuccess()
                    }
                }
            },
            label = { Text("رمز PIN الاحتياطي (1234)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(240.dp)
                .testTag("pin_input_field"),
            singleLine = true,
            isError = showPinError
        )

        if (showPinError) {
            Text(
                text = "رمز PIN غير صحيح. جرب 1234",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onTriggerSystemAuth() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .testTag("auth_system_button")
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = "بصمة")
            Spacer(modifier = Modifier.width(8.dp))
            Text("فتح بقفل الهاتف NATIVE")
        }
    }
}

@Composable
fun SettingsSecurityFallback(
    onUnlockSuccess: () -> Unit,
    onTriggerSystemAuth: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onTriggerSystemAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "حماية الإعدادات",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "تأكيد حماية الإعدادات المالي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "برجاء تأكيد قفل الهاتف للدخول للإعدادات، أو PIN الاحتياطي (1234)",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = enteredPin,
            onValueChange = {
                if (it.length <= 4) {
                    enteredPin = it
                    if (it == "1234") {
                        onUnlockSuccess()
                    }
                }
            },
            label = { Text("رمز PIN الإعدادات (1234)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(220.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onTriggerSystemAuth() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = "تحقق")
            Spacer(modifier = Modifier.width(8.dp))
            Text("التحقق عبر هوية الهاتف")
        }
    }
}

@Composable
fun FinanceBottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentTab == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(if (currentTab == "home") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية") }
        )
        NavigationBarItem(
            selected = currentTab == "transactions",
            onClick = { onTabSelected("transactions") },
            icon = { Icon(if (currentTab == "transactions") Icons.Filled.ListAlt else Icons.Outlined.ListAlt, contentDescription = "المعاملات") },
            label = { Text("المعاملات") }
        )
        NavigationBarItem(
            selected = currentTab == "reports",
            onClick = { onTabSelected("reports") },
            icon = { Icon(if (currentTab == "reports") Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "التقارير") },
            label = { Text("التقارير") }
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(if (currentTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "الإعدادات") },
            label = { Text("الإعدادات") }
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onTriggerVoiceSpeech: () -> Unit,
    isLoading: Boolean,
    lastActionResponse: String?,
    aiChatResponse: String?,
    errorMessage: String?,
    inputText: String
) {
    val txs by viewModel.allTransactions.collectAsStateWithLifecycle()
    var rawTextinput by remember { mutableStateOf("") }

    // Synchronize local UI textfield with voice inputs
    LaunchedEffect(inputText) {
        rawTextinput = inputText
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTriggerVoiceSpeech()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App top header info
        item {
            HomeHeader(onSettingsClick = { viewModel.navigateTo("settings") })
        }

        // Custom mathematical dynamic formula center-stage!
        item {
            FormulaTotalCard(transactions = txs)
        }

        // Microphone pulsing speech recording action button!
        item {
            VoiceSpeakButton(
                isLoading = isLoading,
                onMicClick = {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }

        // Designed Mini Chart bars
        item {
            MiniChartPlaceholder(transactions = txs)
        }

        // Loader status for Gemini AI REST calculations
        if (isLoading) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "جاري الحوسبة المالية الذكية مع جمناي...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Audio responses, alert actions bubble confirmation
        if (lastActionResponse != null || errorMessage != null || aiChatResponse != null) {
            item {
                ResponseAlertBubble(
                    lastActionResponse = lastActionResponse,
                    errorMessage = errorMessage,
                    aiChatResponse = aiChatResponse,
                    lastQueryText = inputText,
                    onClear = { viewModel.clearMessages(); viewModel.clearChatResponse() }
                )
            }
        }



        // Recent mini helper transaction list
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر المعاملات المسجلة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { viewModel.navigateTo("transactions") }) {
                    Text("عرض الكل", fontSize = 13.sp)
                }
            }
        }

        if (txs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أي معاملات مضافة حاليًا. اضغط ع المايك واقرأ معاملتك ماليًا!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(txs.take(5)) { tx ->
                TransactionRowItem(transaction = tx, onDelete = { viewModel.deleteTransactionDirect(tx) })
            }
        }
    }
}

@Composable
fun HomeHeader(onSettingsClick: () -> Unit) {
    val dateString = remember {
        val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
        sdf.format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // High fidelity designed user initial avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDE1FF)) // Designed soft purple-indigo accent circle
                    .clickable { onSettingsClick() }
            ) {
                Text(
                    text = "A", // Initial representing User address (Aboelsoud)
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF001453) // Designed deep contrasting Indigo ink
                )
            }
            
            Column {
                Text(
                    text = "المدير المالي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ColorIncome) // Pulsing active teal indicator
                    )
                    Text(
                        text = "متصل بـ Gemini AI • $dateString",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        // Circular Settings Icon Button matching the theme
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .shadow(1.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "الإعدادات",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun FormulaTotalCard(transactions: List<Transaction>) {
    // Collect stats for this month
    val calendar = Calendar.getInstance()
    val thisMonth = calendar.get(Calendar.MONTH)
    val thisYear = calendar.get(Calendar.YEAR)

    val currentMonthTxs = transactions.filter { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        txCal.get(Calendar.MONTH) == thisMonth && txCal.get(Calendar.YEAR) == thisYear
    }

    val totalIncome = currentMonthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = currentMonthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val netValue = totalIncome - totalExpense
    val netAbs = abs(netValue)

    // Formula card container matching the premium grey-blue bg and rounded boundaries
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF2C2D35) else Color(0xFFE1E2EC)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(28.dp))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            .testTag("formula_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "النسبة الرياضية للشهر الحالي",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // Top: Expenses
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ColorExpense))
                    Text(
                        text = "إجمالي المنصرف",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = String.format("%,.0f ج.م", totalExpense),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorExpense
                )
            }

            // Central division formula mark
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            // Bottom: Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ColorIncome))
                    Text(
                        text = "إجمالي الدخل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = String.format("%,.0f ج.م", totalIncome),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorIncome
                )
            }

            // Equality calculation line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "=",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الصافي الحالي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "${if (netValue < 0) "-" else ""}${String.format("%,.0f", netAbs)} ج.م",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netValue >= 0) ColorIncome else ColorExpense
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceSpeakButton(
    isLoading: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isLoading) 1f else 0.95f,
        targetValue = if (isLoading) 1f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(scale)
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .clickable { onMicClick() }
                .testTag("microphone_pulsing_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "ابدأ التحدث",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "اضغط واتكلم بالعامية المصرية (جمناي بيسمعك)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MiniChartPlaceholder(transactions: List<Transaction>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نبض النشاط المالي الأسبوعي",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "مؤشر بياني",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val maxAmount = transactions.maxOfOrNull { it.amount } ?: 1.0
                val baseHeights = listOf(0.4f, 0.7f, 0.9f, 0.55f, 0.3f, 0.45f)
                
                for (i in 0 until 6) {
                    val heightPercentage = if (i < transactions.size) {
                        (transactions[i].amount / maxAmount * 0.7f + 0.2f).toFloat()
                    } else {
                        baseHeights[i % baseHeights.size]
                    }
                    
                    val isMain = i == 2 // Highlit core bar
                    val barColor = if (isMain) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightPercentage)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun ResponseAlertBubble(
    lastActionResponse: String?,
    errorMessage: String?,
    aiChatResponse: String?,
    lastQueryText: String,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (errorMessage != null) {
                Color(0xFFFEF2F2)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (errorMessage != null) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("response_bubble_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Badge & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (errorMessage != null) Color(0xFFFEE2E2) else Color(0xFFF1F0F7))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (errorMessage != null) "خطأ مالي" else "آخر عملية ذكية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (errorMessage != null) ColorExpense else Color(0xFF535992)
                        )
                    }
                    Text(
                        text = "الآن",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // High aesthetic quotes section showing vocal transcription
            val shownQuery = lastQueryText.ifBlank { "سوبر ماركت كلف 600... محمد خد 500 جنيه النهاردة" }
            Text(
                text = "\"$shownQuery\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = TextDirection.ContentOrRtl
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Dynamic Succeeded or Error badge
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "تحذير",
                        tint = ColorExpense,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorExpense
                    )
                }
            } else {
                val feedbackText = lastActionResponse ?: aiChatResponse ?: "تمت الإضافة والتصنيف بنجاح"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFCCE8E8))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تم",
                        tint = Color(0xFF006A6A),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = feedbackText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006A6A)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val dateString = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        sdf.format(Date(transaction.timestamp))
    }

    val icon = when (transaction.type) {
        "INCOME" -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }
    
    val colorAccent = when (transaction.type) {
        "INCOME" -> ColorIncome
        else -> ColorExpense
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorAccent.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.type,
                    tint = colorAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Transaction Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "معاملة مالية" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateString,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Amount & Delete Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (transaction.type == "INCOME") "+" else "-"}${transaction.amount} ج.م",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAccent,
                    modifier = Modifier.padding(end = 6.dp)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف المعاملة",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionsListScreen(viewModel: FinanceViewModel) {
    val txs by viewModel.allTransactions.collectAsStateWithLifecycle()
    var searchCriteria by remember { mutableStateOf("") }

    val filteredList = txs.filter {
        it.description.contains(searchCriteria, ignoreCase = true) ||
        it.category.contains(searchCriteria, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "دفتر المعاملات المالي الكامل",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Search in Ledger
        OutlinedTextField(
            value = searchCriteria,
            onValueChange = { searchCriteria = it },
            placeholder = { Text("بحث في الوصف أو الفئة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("ledger_search_field"),
            singleLine = true
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد معاملات تطابق المعيار المحدد.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredList) { tx ->
                    TransactionRowItem(transaction = tx, onDelete = { viewModel.deleteTransactionDirect(tx) })
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: FinanceViewModel) {
    val txs by viewModel.allTransactions.collectAsStateWithLifecycle()
    val aiChatResponse by viewModel.aiChatResponse.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var reportPeriodQuery by remember { mutableStateOf("كل الأوقات") }
    var selectedCategoryFilter by remember { mutableStateOf("كل الفئات") }

    // Dropdown state
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Categories list from Custom categories
    val rawCategories = viewModel.settingsManager.getCustomCategories()
    val categoriesList = remember(rawCategories) {
        listOf("كل الفئات") + rawCategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Interactive filtering of transactions
    val filteredTxs = remember(txs, reportPeriodQuery, selectedCategoryFilter) {
        txs.filter { tx ->
            // --- 1. Category Filter ---
            val matchesCategory = if (selectedCategoryFilter == "كل الفئات") {
                true
            } else {
                tx.category.trim().equals(selectedCategoryFilter.trim(), ignoreCase = true)
            }

            // --- 2. Period Filter (Keyword matches) ---
            val matchesPeriod = when {
                reportPeriodQuery == "كل الأوقات" -> true
                reportPeriodQuery.contains("مايو") || reportPeriodQuery.contains("may") || reportPeriodQuery.contains("05") -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == 2026 && txCal.get(Calendar.MONTH) == Calendar.MAY
                }
                reportPeriodQuery.contains("أسبوع") || reportPeriodQuery.contains("weekend") || reportPeriodQuery.contains("السبت") || reportPeriodQuery.contains("الخميس") -> {
                    val diffDays = (System.currentTimeMillis() - tx.timestamp) / (24 * 60 * 60 * 1000)
                    diffDays <= 3
                }
                reportPeriodQuery.contains("30") || reportPeriodQuery.contains("شهر") -> {
                    val diffDays = (System.currentTimeMillis() - tx.timestamp) / (24 * 60 * 60 * 1000)
                    diffDays <= 30
                }
                else -> true
            }

            matchesCategory && matchesPeriod
        }
    }

    val filteredIncome = filteredTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    val filteredExpense = filteredTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Group expenses by category for filtered subset
    val expensesByCategory = filteredTxs.filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "التقارير المالية والرسوم البيانية الكلية",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Custom Report Creator Card Panel
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "فلترة الميزانية وتوليد تقارير مخصصة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Durations Options FlowRow
                    Text(
                        text = "اختر الفترة الزمنية للتقرير:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("كل الأوقات", "نهاية الأسبوع", "شهر مايو", "الـ 30 يومًا").forEach { opt ->
                            val isSelected = (opt == "الـ 30 يومًا" && reportPeriodQuery == "الـ 30 يومًا الماضية") || (opt == reportPeriodQuery)
                            val displayOpt = if (opt == "الـ 30 يومًا") "الـ 30 يومًا الماضية" else opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        reportPeriodQuery = displayOpt
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }



                    // 3. Category selector choice chip representation
                    Text(
                        text = "تصفية حسب فئة إنفاق محددة:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Dropdown simulation with click and menu
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showCategoryDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "الفئة الحالية: $selectedCategoryFilter ▾", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCategoryFilter = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Request dynamic AI report from Gemini
                    Button(
                        onClick = {
                            viewModel.processFinanceQuery(
                                "يرجى كتابة وصياغة تقرير مالي مخصص ذكي ومفصل للفترة: '$reportPeriodQuery' والفئة المحددة للتصفية: '$selectedCategoryFilter'. اذكر وتحلل القيم المحددة (الدخل الحالي: $filteredIncome، والمصروفات الحالية: $filteredExpense) واقترح ممارسات توفير وسلوك إنفاق مالي حكيم بالتطبيق."
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_ai_custom_report_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "ذكاء")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد تقرير وتحليل مالي ذكي بجمناي 🧠", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري إنشاء التقرير المالي من جمناي...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Gemini custom report output card (if present)
        if (aiChatResponse != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "تقرير ذكي", tint = MaterialTheme.colorScheme.primary)
                                Text("التقرير المالي التوليدي لـ Gemini 💡", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.clearChatResponse() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                        Text(
                            text = aiChatResponse ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.ContentOrRtl
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Summary metric cards FOR FILTERED transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ColorIncome.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("الدخل المستهدف", fontSize = 11.sp, color = ColorIncome)
                        Text(
                            text = String.format("%,.2f ج.م", filteredIncome),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorIncome
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ColorExpense.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("المنصرف المستهدف", fontSize = 11.sp, color = ColorExpense)
                        Text(
                            text = String.format("%,.2f ج.م", filteredExpense),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorExpense
                        )
                    }
                }
            }
        }

        // Custom canvas graphics: Circular distribution ratio progress chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "توزيع الميزانية للفترة الحالية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalBudget = filteredIncome + filteredExpense
                    val incomeAngle = if (totalBudget > 0) (filteredIncome / totalBudget * 360).toFloat() else 180f
                    val expenseAngle = if (totalBudget > 0) (filteredExpense / totalBudget * 360).toFloat() else 180f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val strokeWidth = 24.dp.toPx()
                            
                            drawArc(
                                color = ColorIncome,
                                startAngle = -90f,
                                sweepAngle = incomeAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            drawArc(
                                color = ColorExpense,
                                startAngle = -90f + incomeAngle,
                                sweepAngle = expenseAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f%%", if (totalBudget > 0) (filteredIncome / totalBudget * 100) else 0.0),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "نسبة الوفر بالفترة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ColorIncome))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الدخل للفترة", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ColorExpense))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المنصرف للفترة", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Horizontal Category Spending Progress bars!
        if (expensesByCategory.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تفصيل المصروفات حسب الفئة بالفترة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        expensesByCategory.forEach { (category, amount) ->
                            val pct = if (filteredExpense > 0) amount / filteredExpense else 0.0
                            
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = category, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${String.format("%,.0f", amount)} ج.م (${String.format("%.1f", pct*100)}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorExpense
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = pct.toFloat(),
                                    color = ColorExpense,
                                    trackColor = ColorExpense.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مصروفات مسجلة لهذه الفئة أو الفترة الحالية.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    var geminiKey by remember { mutableStateOf(viewModel.settingsManager.getGeminiApiKey()) }
    var instructions by remember { mutableStateOf(viewModel.settingsManager.getCustomInstructions()) }
    var categoriesText by remember { mutableStateOf(viewModel.settingsManager.getCustomCategories()) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showGeminiKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "إعدادات التطبيق والمزامنة",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Theme Toggle Section (العناية بالثيمات فاتح وليلي)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مظهر التطبيق (Theme Toggle)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateThemeMode("light") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = if (themeMode == "light") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = "قالب فاتح")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فاتح")
                        }

                        Button(
                            onClick = { viewModel.updateThemeMode("dark") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = if (themeMode == "dark") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = "قالب داكن")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ليلي")
                        }
                    }
                }
            }
        }

        // Gemini API Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مفتاح ذكاء جمناي (Gemini API Key)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يرجى توفير مفتاح Gemini API الصالح للاتصال بالذكاء الاصطناعي، أو تعيينه بنجاح عبر لوحة أسرار AI Studio.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = {
                            geminiKey = it
                            viewModel.settingsManager.saveGeminiApiKey(it)
                        },
                        label = { Text("Gemini API Key") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val img = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(img, contentDescription = "رؤية")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("gemini_key_settings_field")
                    )
                }
            }
        }

        // Custom prompt instructions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تعليمات مخصصة لجمناي (Custom System Instructions)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "قم بإضافة أي إرشادات خاصة للذكاء الاصطناعي (مثل: احسب بالجنيه المصري، صنّف السوبر ماركت كخضار وفاكهة، إلخ).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = {
                            instructions = it
                            viewModel.settingsManager.saveCustomInstructions(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("custom_instructions_settings_field"),
                        maxLines = 4,
                        placeholder = { Text("اكتب شروطك لجمناي هنا...") }
                    )
                }
            }
        }

        // Custom categories configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "فئات ومصروفات مخصصة للتصنيف (Custom Classification Categories)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "افصل بين الفئات بالفواصل (مثال: سوبر ماركت، عيادة، كافيه، راتب، بقالة). سيتعلم Gemini استخدام هذه الفئات وتصنيف المعاملات تلقائياً بمجرد إدخالها.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = categoriesText,
                        onValueChange = {
                            categoriesText = it
                            viewModel.settingsManager.saveCustomCategories(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("custom_categories_settings_field"),
                        placeholder = { Text("اكتب التصنيفات متبوعة بفواصل...") }
                    )
                }
            }
        }

        // Danger zone clear data
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "منطقة الخطر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.clearAllTransactions() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "تصفير")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح وحذف كافة البيانات")
                    }
                }
            }
        }
    }
}
