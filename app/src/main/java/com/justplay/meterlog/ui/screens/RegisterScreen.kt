package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justplay.meterlog.ui.viewmodel.AuthViewModel

@Composable
internal fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var emailWasTouched by remember { mutableStateOf(false) }
    var passwordWasTouched by remember { mutableStateOf(false) }
    var confirmPasswordWasTouched by remember { mutableStateOf(false) }
    var emailHasFocus by remember { mutableStateOf(false) }
    var passwordHasFocus by remember { mutableStateOf(false) }
    var confirmPasswordHasFocus by remember { mutableStateOf(false) }
    val fieldErrors = AuthFieldErrors.from(
        error = state.error,
        emailError = state.emailError,
        passwordError = state.passwordError,
        confirmPasswordError = state.confirmPasswordError
    )

    AuthScaffold(subtitle = "建立帳號後開始同步資料") {
        AuthTextField(
            value = email,
            onValueChange = {
                email = it
                authViewModel.clearEmailError()
            },
            label = "Email",
            enabled = !state.loading,
            error = fieldErrors.email.takeUnless { emailHasFocus },
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        emailWasTouched = true
                    }
                }
                .onFocusChanged { focusState ->
                    emailHasFocus = focusState.isFocused
                    if (!focusState.isFocused && emailWasTouched) {
                        authViewModel.validateRegisterEmail(email)
                    }
                }
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = {
                password = it
                authViewModel.clearPasswordError()
                if (confirmPasswordWasTouched) {
                    authViewModel.clearConfirmPasswordError()
                }
            },
            label = "密碼",
            enabled = !state.loading,
            error = fieldErrors.password.takeUnless { passwordHasFocus },
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        passwordWasTouched = true
                    }
                }
                .onFocusChanged { focusState ->
                    passwordHasFocus = focusState.isFocused
                    if (!focusState.isFocused && passwordWasTouched) {
                        authViewModel.validateRegisterPassword(password)
                        if (confirmPasswordWasTouched) {
                            authViewModel.validateConfirmPassword(password, confirmPassword)
                        }
                    }
                },
            isPassword = true
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                authViewModel.clearConfirmPasswordError()
            },
            label = "再次確認密碼",
            enabled = !state.loading,
            error = fieldErrors.confirmPassword.takeUnless { confirmPasswordHasFocus },
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        confirmPasswordWasTouched = true
                    }
                }
                .onFocusChanged { focusState ->
                    confirmPasswordHasFocus = focusState.isFocused
                    if (!focusState.isFocused && confirmPasswordWasTouched) {
                        authViewModel.validateConfirmPassword(password, confirmPassword)
                    }
                },
            isPassword = true
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackToLogin,
                modifier = Modifier.weight(1f),
                enabled = !state.loading
            ) {
                Text("返回登入")
            }
            Button(
                onClick = { authViewModel.register(email, password, confirmPassword) },
                modifier = Modifier.weight(1f),
                enabled = !state.loading
            ) {
                Text("建立帳號")
            }
        }
        AuthLoading(state.loading)
    }
}
