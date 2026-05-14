package com.justplay.meterlog.ui.screens

import android.content.Context
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.justplay.meterlog.R
import com.justplay.meterlog.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
internal fun LoginScreen(
    authViewModel: AuthViewModel,
    onOpenRegister: () -> Unit
) {
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val webClientId = stringResource(R.string.local_default_web_client_id)
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var emailWasTouched by remember { mutableStateOf(false) }
    var emailHasFocus by remember { mutableStateOf(false) }
    val fieldErrors = AuthFieldErrors.from(state.error, state.emailError)

    AuthScaffold(
        subtitle = "登入後同步整棟表具與讀數",
        loading = state.loading
    ) {
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
                        authViewModel.validateLoginEmail(email)
                    }
                }
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "密碼",
            enabled = !state.loading,
            error = fieldErrors.password,
            isPassword = true
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        signInWithGoogleCredentialManager(
                            credentialManager = credentialManager,
                            context = context,
                            webClientId = webClientId,
                            authViewModel = authViewModel
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(AuthButtonHeight),
                enabled = !state.loading
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Google 登入")
            }
            Button(
                onClick = { authViewModel.signIn(email, password) },
                modifier = Modifier
                    .weight(1f)
                    .height(AuthButtonHeight),
                enabled = !state.loading
            ) {
                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("登入")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onOpenRegister,
            enabled = !state.loading
        ) {
            Text(
                text = "建立帳號",
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline
            )
        }
        AuthLoading(state.loading)
    }
}

private suspend fun signInWithGoogleCredentialManager(
    credentialManager: CredentialManager,
    context: Context,
    webClientId: String,
    authViewModel: AuthViewModel
) {
    val googleOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()

    runCatching {
        val credential = credentialManager.getCredential(
            context = context,
            request = request
        ).credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "Credential type ${credential.type} is not a Google ID token."
        }
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    }.onSuccess { idToken ->
        authViewModel.signInWithGoogle(idToken)
    }.onFailure { error ->
        authViewModel.showGoogleSignInError(error.toGoogleSignInMessage())
    }
}

private fun Throwable.toGoogleSignInMessage(): String {
    return when (this) {
        is NoCredentialException -> "找不到可用的 Google 登入憑證，請確認裝置已加入 Google 帳號。"
        is GetCredentialProviderConfigurationException -> "Google 登入服務目前不可用，請更新 Google Play 服務後再試。"
        is GetCredentialUnsupportedException -> "此裝置不支援目前的 Google 登入方式。"
        is GetCredentialCancellationException -> "Google 登入已取消。"
        is GetCredentialInterruptedException -> "Google 登入流程被中斷，請再試一次。"
        is GetCredentialUnknownException -> "Google 登入發生未知錯誤，請稍後再試。"
        is GetCredentialException -> "Google 登入未完成，請稍後再試。"
        is GoogleIdTokenParsingException -> "Google 登入憑證解析失敗，請確認 Firebase 的 Web client ID 設定正確。"
        is IllegalArgumentException -> "Google 登入沒有取得有效的 ID Token，請確認 Firebase Console 已啟用 Google 登入。"
        else -> "Google 登入失敗，請稍後再試。"
    }
}
