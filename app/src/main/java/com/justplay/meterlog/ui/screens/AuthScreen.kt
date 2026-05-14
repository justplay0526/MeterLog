package com.justplay.meterlog.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.justplay.meterlog.R
import com.justplay.meterlog.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val transitionSpec = tween<IntOffset>(
        durationMillis = 260,
        easing = FastOutSlowInEasing
    )

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = transitionSpec
            ) + fadeIn(animationSpec = tween(180))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = transitionSpec
            ) + fadeOut(animationSpec = tween(140))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = transitionSpec
            ) + fadeIn(animationSpec = tween(180))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = transitionSpec
            ) + fadeOut(animationSpec = tween(140))
        }
    ) {
        composable(
            route = AuthRoute.Login,
            enterTransition = { fadeIn(animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(140)) },
            popEnterTransition = { fadeIn(animationSpec = tween(180)) },
            popExitTransition = { fadeOut(animationSpec = tween(140)) }
        ) {
            LoginScreen(
                authViewModel = authViewModel,
                onOpenRegister = {
                    authViewModel.clearError()
                    navController.navigate(AuthRoute.Register)
                }
            )
        }
        composable(
            route = AuthRoute.Register,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = tween(180))
            },
            exitTransition = {
                slideOutVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it }
                ) + fadeOut(animationSpec = tween(140))
            },
            popEnterTransition = {
                slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = tween(180))
            },
            popExitTransition = {
                slideOutVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it }
                ) + fadeOut(animationSpec = tween(140))
            }
        ) {
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
    loading: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            AuthCard(subtitle = subtitle, loading = loading, content = content)
        }
    }
}

@Composable
private fun BoxScope.AuthCard(
    subtitle: String,
    loading: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 }
            ),
        exit = fadeOut(animationSpec = tween(120)) +
            slideOutVertically(targetOffsetY = { it / 16 })
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthLogo(loading = loading)
                Spacer(Modifier.height(14.dp))
                Text("水電表記錄", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))
                content()
            }
        }
    }
}

@Composable
private fun AuthLogo(loading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "auth-logo-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (loading) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth-logo-scale"
    )
    val restingScale by animateFloatAsState(
        targetValue = if (loading) pulseScale else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "auth-logo-rest"
    )

    Icon(
        painter = painterResource(R.drawable.ic_launcher_round),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = restingScale
                scaleY = restingScale
            }
    )
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
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(animationSpec = tween(140)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun AuthLoading(loading: Boolean) {
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(animationSpec = tween(140)) +
            slideInVertically(initialOffsetY = { -it / 3 }),
        exit = fadeOut(animationSpec = tween(100)) +
            slideOutVertically(targetOffsetY = { -it / 3 })
    ) {
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator()
    }
}

internal val AuthButtonHeight = 48.dp

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
