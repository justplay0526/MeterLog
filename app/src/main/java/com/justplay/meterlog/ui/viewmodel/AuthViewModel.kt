package com.justplay.meterlog.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.justplay.meterlog.data.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val uid: StateFlow<String?> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = authRepository.currentUid
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private var emailValidationJob: Job? = null

    fun signIn(email: String, password: String) {
        runAuthAction {
            validateEmailPassword(email, password)
            authRepository.signIn(email.trim(), password)
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        runAuthAction {
            validateEmailPassword(email, password)
            validateRegistrationPassword(password, confirmPassword)
            authRepository.register(email.trim(), password)
        }
    }

    fun signInWithGoogle(idToken: String?) {
        runAuthAction {
            require(!idToken.isNullOrBlank()) { "Google 登入憑證無效" }
            authRepository.signInWithGoogleIdToken(idToken)
        }
    }

    fun showGoogleSignInError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    fun signOut() {
        emailValidationJob?.cancel()
        _uiState.value = AuthUiState()
        authRepository.signOut()
    }

    fun clearError() {
        emailValidationJob?.cancel()
        _uiState.value = _uiState.value.copy(error = null, emailError = null)
    }

    fun clearEmailError() {
        emailValidationJob?.cancel()
        _uiState.value = _uiState.value.copy(emailError = null)
    }

    fun clearPasswordError() {
        _uiState.value = _uiState.value.copy(passwordError = null)
    }

    fun clearConfirmPasswordError() {
        _uiState.value = _uiState.value.copy(confirmPasswordError = null)
    }

    fun validateLoginEmail(email: String) {
        emailValidationJob?.cancel()
        _uiState.value = _uiState.value.copy(emailError = validateEmailFormat(email))
    }

    fun validateRegisterEmail(email: String) {
        emailValidationJob?.cancel()

        val trimmedEmail = email.trim()
        val formatError = validateEmailFormat(trimmedEmail)
        if (formatError != null) {
            _uiState.value = _uiState.value.copy(emailError = formatError)
            return
        }

        emailValidationJob = viewModelScope.launch {
            runCatching { authRepository.isEmailRegistered(trimmedEmail) }
                .onSuccess { isRegistered ->
                    _uiState.value = _uiState.value.copy(
                        emailError = if (isRegistered) "此 Email 已被使用" else null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        emailError = "無法確認 Email 是否可用，請稍後再試。"
                    )
                }
        }
    }

    fun validateRegisterPassword(password: String) {
        _uiState.value = _uiState.value.copy(passwordError = validatePasswordRules(password))
    }

    fun validateConfirmPassword(password: String, confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPasswordError = validateConfirmPasswordRules(password, confirmPassword)
        )
    }

    private fun runAuthAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            emailValidationJob?.cancel()
            _uiState.value = AuthUiState(loading = true)
            runCatching { action() }
                .onSuccess { _uiState.value = AuthUiState() }
                .onFailure { throwable ->
                    val message = throwable.toUserMessage()
                    _uiState.value = if (throwable is FirebaseAuthUserCollisionException) {
                        AuthUiState(emailError = message)
                    } else {
                        AuthUiState(error = message)
                    }
                }
        }
    }

    private fun validateEmailPassword(email: String, password: String) {
        validateEmailFormat(email)?.let { error(it) }
        require(password.isNotBlank()) { "密碼不可空白" }
    }

    private fun validateEmailFormat(email: String): String? {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isBlank() -> "Email 不可空白"
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "Email 格式不正確"
            else -> null
        }
    }

    private fun validateRegistrationPassword(password: String, confirmPassword: String) {
        validatePasswordRules(password)?.let { error(it) }
        validateConfirmPasswordRules(password, confirmPassword)?.let { error(it) }
    }

    private fun validatePasswordRules(password: String): String? {
        return when {
            password.isBlank() -> "密碼不可空白"
            password.none { it.isUpperCase() } -> "密碼必須包含至少一個大寫英文字母"
            password.none { it.isLowerCase() } -> "密碼必須包含至少一個小寫英文字母"
            password.none { !it.isLetterOrDigit() && !it.isWhitespace() } -> "密碼必須包含至少一個特殊符號"
            else -> null
        }
    }

    private fun validateConfirmPasswordRules(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "確認密碼不可空白"
            password != confirmPassword -> "密碼與確認密碼不一致"
            else -> null
        }
    }

    private fun Throwable.toUserMessage(): String {
        val message = message.orEmpty()
        return when {
            this is FirebaseAuthUserCollisionException ||
                message.contains("email address is already in use", ignoreCase = true) ||
                message.contains("email-already-in-use", ignoreCase = true) ->
                "此 Email 已被使用"
            message.contains("Chain validation failed", ignoreCase = true) ->
                "登入連線憑證驗證失敗，請確認手機日期時間為自動設定、網路正常，並更新 Google Play 服務後再試。"
            message.contains("network", ignoreCase = true) ->
                "網路連線異常，請確認網路後再試。"
            message.isBlank() ->
                "操作失敗，請稍後再試。"
            else -> message
        }
    }
}

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)
