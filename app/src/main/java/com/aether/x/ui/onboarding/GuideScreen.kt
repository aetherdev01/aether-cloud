package com.aether.x.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.aether.x.R
import kotlinx.coroutines.launch

private data class GuidePage(val title: Int, val body: Int)

private val guidePages = listOf(
    GuidePage(R.string.guide_title_1, R.string.guide_body_1),
    GuidePage(R.string.guide_title_2, R.string.guide_body_2),
    GuidePage(R.string.guide_title_3, R.string.guide_body_3),
    GuidePage(R.string.guide_title_4, R.string.guide_body_4),
)

@Composable
fun GuideScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { guidePages.size })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.guide_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val item = guidePages[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).let { kotlin.math.abs(it) }.coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .graphicsLayer {
                                val scale = lerp(0.9f, 1f, 1f - pageOffset)
                                scaleX = scale
                                scaleY = scale
                                alpha = lerp(0.4f, 1f, 1f - pageOffset)
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                ),
                            ),
                    ) {
                        GuideIllustration(page = page, modifier = Modifier.fillMaxSize())
                    }
                    Text(
                        text = stringResource(item.title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                    Text(
                        text = stringResource(item.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(guidePages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 22.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    )
                }
            }

            val isLastPage = pagerState.currentPage == guidePages.lastIndex
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isLastPage) {
                        stringResource(R.string.guide_finish)
                    } else {
                        stringResource(R.string.guide_next)
                    },
                )
            }
        }
    }
}
