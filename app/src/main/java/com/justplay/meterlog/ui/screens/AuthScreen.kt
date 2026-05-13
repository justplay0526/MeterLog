package com.justplay.meterlog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.justplay.meterlog.R
import com.justplay.meterlog.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRoute.Login) {
        composable(AuthRoute.Login) {
            LoginScreen(
                authViewModel = authViewModel,
                onOpenRegister = {
                    authViewModel.clearError()
                    navController.navigate(AuthRoute.Register)
                }
            )
        }
        composable(AuthRoute.Register) {
            RegisterScreen(
                authViewModel = authViewModel,
                onBackToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
internal fun AuthScaffold(
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_round),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.height(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("水電表記錄", style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
    FieldErrorText(error)
}

@Composable
private fun FieldErrorText(error: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun AuthLoading(loading: Boolean) {
    if (loading) {
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator()
    }
}

internal data class AuthFieldErrors(
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
) {
    companion object {
        fun from(
            error: String?,
            emailError: String?,
            passwordError: String? = null,
            confirmPasswordError: String? = null
        ): AuthFieldErrors {
            val fieldErrors = when {
                error == null -> AuthFieldErrors()
                error.contains("Email", ignoreCase = true) -> AuthFieldErrors(email = error)
                error.contains("確認密碼") -> AuthFieldErrors(confirmPassword = error)
                error.contains("密碼") ||
                    error.contains("Google 登入") ||
                    error.contains("登入", ignoreCase = true) -> AuthFieldErrors(password = error)
                else -> AuthFieldErrors(password = error)
            }
            return fieldErrors.copy(
                email = emailError ?: fieldErrors.email,
                password = passwordError ?: fieldErrors.password,
                confirmPassword = confirmPasswordError ?: fieldErrors.confirmPassword
            )
        }
    }
}

private object AuthRoute {
    const val Login = "auth-login"
    const val Register = "auth-register"
}
