package app.twoverse.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.twoverse.core.designsystem.theme.TwoverseTheme

/**
 * Shared frame for the account screens without mockups (Sign up, Reset password): the
 * Sign in layout with [top] content scrolling and [bottom] actions pinned when there is room.
 */
@Composable
internal fun AuthFormLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontalWide)
                .padding(top = spacing.xs, bottom = spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                header()
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                top()
            }
            Column(modifier = Modifier.padding(top = spacing.xl), content = bottom)
        }
    }
}

/** A status or error line under a form, announced by TalkBack when it changes. */
@Composable
internal fun FormMessage(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) TwoverseTheme.colors.error else TwoverseTheme.colors.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
