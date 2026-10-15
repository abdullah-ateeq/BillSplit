package com.example.billsplit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billsplit.model.PersonShareResult
import com.example.billsplit.ui.theme.SegmentColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProportionalSplitBar(
    people: List<PersonShareResult>,
    totalMinorUnits: Long,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty() || totalMinorUnits <= 0L) return

    val animProgress = remember { Animatable(if (animationsEnabled) 0f else 1f) }

    LaunchedEffect(people, totalMinorUnits) {
        if (animationsEnabled) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // The Proportional Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(animProgress.value)
                    .fillMaxHeight()
            ) {
                people.forEachIndexed { index, person ->
                    val weight = (person.allocatedMinorUnits.toFloat() / totalMinorUnits.toFloat()).coerceAtLeast(0.001f)
                    val color = SegmentColors[index % SegmentColors.size]

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend with percentage and name indicators (accessible: not color alone)
        FlowRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            people.forEachIndexed { index, person ->
                val color = SegmentColors[index % SegmentColors.size]
                val pctFormatted = if (person.effectivePercentage % 1.0 == 0.0) {
                    "${person.effectivePercentage.toInt()}%"
                } else {
                    String.format(Locale.US, "%.1f%%", person.effectivePercentage)
                }

                Row(
                    modifier = Modifier.padding(end = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${person.name} ($pctFormatted)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
