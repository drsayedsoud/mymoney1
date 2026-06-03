package com.example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.FinanceAppUI
import com.example.ui.theme.MyApplicationTheme
import android.speech.tts.TextToSpeech
import com.example.util.DailyReminderScheduler
import com.example.util.DeviceAuthenticator
import com.example.viewmodel.FinanceViewModel
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var viewModel: FinanceViewModel
    private val deviceAuthenticator = DeviceAuthenticator()
    private var textToSpeech: TextToSpeech? = null

    // Activity launcher for notification permissions on Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        DailyReminderScheduler.scheduleDailyReminder(this)
    }

    // Activity launcher for lock authentication
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (viewModel.currentScreen.value == "settings") {
                viewModel.setSettingsUnlocked(true)
            } else {
                viewModel.setAppUnlocked(true)
            }
        }
        // If cancelled/failed, we will default to beautiful fallback in-app PIN lock
    }

    // Activity launcher for Voice-to-Text Transcription in Arabic
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                viewModel.onSpeechRecognized(spokenText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate ViewModel
        viewModel = FinanceViewModel(application)

        // Initialize state based on device security
        if (!deviceAuthenticator.isDeviceSecure(this)) {
            // If the emulator or device does not have system lock screen configured,
            // unlock by default or let custom PIN overlay fallback
            viewModel.setAppUnlocked(true)
            viewModel.setSettingsUnlocked(true)
        }

        // Initialize daily alarms reminded at 11 PM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            DailyReminderScheduler.scheduleDailyReminder(this)
        }

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            val themeMode = viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode.value) {
                FinanceAppUI(
                    viewModel = viewModel,
                    onTriggerVoiceSpeech = { triggerVoiceRecognizer() },
                    onTriggerSystemUnlock = { triggerSystemUnlockPrompt() },
                    onSpeak = { text -> speakOut(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ar", "EG"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale("ar"))
            }
        }
    }

    private fun speakOut(text: String) {
        if (text.isNotEmpty()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FinanceTTS")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun triggerSystemUnlockPrompt() {
        val authIntent = deviceAuthenticator.createAuthenticationIntent(this)
        if (authIntent != null) {
            authLauncher.launch(authIntent)
        } else {
            // Fallback: system is secure but no keys set or failed to load.
            // Let them type backup PIN
        }
    }

    private fun triggerVoiceRecognizer() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "اتكلم بالعامية المصرية... (مثال: صرفت 50 جنيه مواصلات)")
        }
        try {
            speechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            // Handle missing voice package or speech intent crash gracefully
            viewModel.updateInputText("")
        }
    }
}
