package org.deafsapps.storeit.androidapp.presentation.account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.androidapp.design.Dimens

@Composable
internal fun AccountStatusAvatar(
    accountEmail: String?,
    isAuthenticated: Boolean,
    isAccountReady: Boolean,
    isRestoreInProgress: Boolean,
    hasPendingSyncWork: Boolean,
    hasAttentionState: Boolean,
    modifier: Modifier = Modifier,
) {
    val avatarLabel = when {
        isAuthenticated -> accountEmail.orEmpty().trim().firstOrNull()?.uppercase() ?: "A"
        else -> "L"
    }
    val avatarContainerColor = when {
        hasAttentionState -> MaterialTheme.colorScheme.errorContainer
        isAccountReady -> MaterialTheme.colorScheme.primaryContainer
        isAuthenticated -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val avatarContentColor = when {
        hasAttentionState -> MaterialTheme.colorScheme.onErrorContainer
        isAccountReady -> MaterialTheme.colorScheme.onPrimaryContainer
        isAuthenticated -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIndicatorColor = when {
        hasAttentionState -> MaterialTheme.colorScheme.error
        isAccountReady -> MaterialTheme.colorScheme.primary
        isRestoreInProgress || hasPendingSyncWork -> MaterialTheme.colorScheme.tertiary
        isAuthenticated -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = avatarContainerColor,
            modifier = Modifier
                .size(Dimens.progressIndicatorSizeLarge)
                .clip(CircleShape),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = avatarLabel,
                    color = avatarContentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .size(Dimens.progressIndicatorSizeSmall)
                .clip(CircleShape)
                .background(color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.progressIndicatorSizeSmall - Dimens.spacingSmall)
                    .clip(CircleShape)
                    .background(color = statusIndicatorColor, shape = CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountStatusAvatarLocalPreview() {
    MaterialTheme {
        AccountStatusAvatar(
            accountEmail = null,
            isAuthenticated = false,
            isAccountReady = false,
            isRestoreInProgress = false,
            hasPendingSyncWork = false,
            hasAttentionState = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountStatusAvatarReadyPreview() {
    MaterialTheme {
        AccountStatusAvatar(
            accountEmail = "user@example.com",
            isAuthenticated = true,
            isAccountReady = true,
            isRestoreInProgress = false,
            hasPendingSyncWork = false,
            hasAttentionState = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountStatusAvatarAttentionPreview() {
    MaterialTheme {
        AccountStatusAvatar(
            accountEmail = "user@example.com",
            isAuthenticated = true,
            isAccountReady = false,
            isRestoreInProgress = false,
            hasPendingSyncWork = false,
            hasAttentionState = true,
        )
    }
}
