package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database.transactionDao())
    val settingsManager = SettingsManager(application)
    private val geminiService = GeminiService()

    // Screen navigation state: "home", "transactions", "reports", "settings"
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Unlock states
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _isSettingsUnlocked = MutableStateFlow(false)
    val isSettingsUnlocked: StateFlow<Boolean> = _isSettingsUnlocked.asStateFlow()

    // Local lists and transactions
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Loading & Message states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastActionResponse = MutableStateFlow<String?>(null)
    val lastActionResponse: StateFlow<String?> = _lastActionResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _aiChatResponse = MutableStateFlow<String?>(null)
    val aiChatResponse: StateFlow<String?> = _aiChatResponse.asStateFlow()

    // Active voice or manual transcription text state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Settings Theme state
    private val _themeMode = MutableStateFlow(settingsManager.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        // App starts locked if secure. We can let MainActivity trigger unlock
        _isAppUnlocked.value = false
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun setSettingsUnlocked(unlocked: Boolean) {
        _isSettingsUnlocked.value = unlocked
    }

    fun navigateTo(screen: String) {
        if (screen == "settings" && !_isSettingsUnlocked.value) {
            // Screen stays on settings locked prompt until authenticated
            _currentScreen.value = "settings"
        } else {
            _currentScreen.value = screen
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun updateThemeMode(mode: String) {
        settingsManager.saveThemeMode(mode)
        _themeMode.value = mode
    }

    fun clearMessages() {
        _lastActionResponse.value = null
        _errorMessage.value = null
    }

    fun clearChatResponse() {
        _aiChatResponse.value = null
    }

    fun onSpeechRecognized(text: String) {
        _inputText.value = text
        processFinanceQuery(text)
    }

    fun processFinanceQuery(prompt: String) {
        if (prompt.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null
            _lastActionResponse.value = null

            val apiKey = settingsManager.getGeminiApiKey()
            val customInstructions = settingsManager.getCustomInstructions()
            val customCategories = settingsManager.getCustomCategories()
            val txs = allTransactions.value

            val result = geminiService.parseFinancialPrompt(
                prompt = prompt,
                apiKey = apiKey,
                customInstructions = customInstructions,
                customCategories = customCategories,
                currentTransactions = txs
            )

            when (result) {
                is GeminiResult.Success -> {
                    handleGeminiJsonResult(result.response)
                }
                is GeminiResult.Error -> {
                    _errorMessage.value = result.message
                    Log.e("FinanceViewModel", "Gemini Query Failed: ${result.message}")
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun handleGeminiJsonResult(json: JSONObject) {
        try {
            val action = json.optString("action", "CHAT")
            val confirmationMessage = json.optString("confirmationMessage", "تمت معالجة طلبك بنجاح.")
            val generalResponseText = json.optString("generalResponseText", "")

            Log.d("FinanceViewModel", "Parsed action: $action")

            when (action) {
                "ADD" -> {
                    val addDetails = json.optJSONObject("transactionToAdd")
                    if (addDetails != null) {
                        val amount = addDetails.optDouble("amount", 0.0)
                        val type = addDetails.optString("type", "EXPENSE")
                        val category = addDetails.optString("category", "عام")
                        val description = addDetails.optString("description", "")

                        val tx = Transaction(
                            amount = amount,
                            type = type,
                            category = category,
                            description = description
                        )
                        repository.insert(tx)
                        _lastActionResponse.value = confirmationMessage
                    } else {
                        _errorMessage.value = "تعذر الحصول على تفاصيل المعاملة الإضافية."
                    }
                }
                "DELETE" -> {
                    val deleteId = json.optInt("transactionIdToDelete", -1)
                    if (deleteId != -1) {
                        repository.deleteById(deleteId)
                        _lastActionResponse.value = confirmationMessage
                    } else {
                        _lastActionResponse.value = "لم نتمكن من العثور على المعاملة المحددة لحذفها."
                    }
                }
                "REPORT", "CHAT" -> {
                    _aiChatResponse.value = generalResponseText
                    _lastActionResponse.value = confirmationMessage
                }
                else -> {
                    _aiChatResponse.value = generalResponseText
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "فشل في فك واستخراج بيانات المعاملة بالشكل الصحيح: ${e.localizedMessage}"
        }
    }

    fun deleteTransactionDirect(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(transaction)
            _lastActionResponse.value = "تم حذف المعاملة '${transaction.description}' بنجاح."
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            _lastActionResponse.value = "تم تصفير السجل المالي بالكامل."
        }
    }
}
