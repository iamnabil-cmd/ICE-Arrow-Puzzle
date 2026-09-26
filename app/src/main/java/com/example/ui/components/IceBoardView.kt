package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArrowDirection
import com.example.data.ArrowModel
import com.example.game.ActiveArrowState
import com.example.game.LevelPlayState
import com.example.ui.theme.AuroraGold
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.CrystalFrostBevel
import com.example.ui.theme.ErrorFracture
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialBlueDark
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Middle Section:
 * 1. Trapped transparent achievement element completely behind the 3D ice block
 * 2. Translucent 3D crystal ice block that cracks bit-by-bit with every arrow, and cracks open completely on the last arrow
 * 3. Thick arrow design with sharp directional arrowhead pointer (matching arrow-design.jpg)
 * 4. Thread unspooling exit animation (thread getting out of the box from curved lines to straight line, matching Arrow-going-I-want.mp4)
 * 5. Full Pinch-to-Zoom & Pan support with tooltip on Level 10+
 */
@Composable
fun IceBoardView(
    levelState: LevelPlayState,
    onArrowTapped: (String) -> Unit,
    onArrowExitCompleted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val def = levelState.definition
    val isRevealed = levelState.isAllCleared || levelState.isShattering

    // Pinch-to-zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when level changes
    LaunchedEffect(def.levelNumber) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    // Zoom tip visibility (shown on Level 10+ or complex levels)
    var showZoomTip by remember { mutableStateOf(def.levelNumber >= 10) }
    LaunchedEffect(def.levelNumber) {
        if (def.levelNumber >= 10) {
            showZoomTip = true
            delay(5000)
            showZoomTip = false
        }
    }

    // Pulse animation for hints
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    // Touch ring ripple feedback
    val tapRipplePos = remember { mutableStateOf<Offset?>(null) }
    val tapRippleProgress = remember { Animatable(0f) }

    // Reveal animations
    val revealScale = remember { Animatable(0.7f) }
    val rayRotation = remember { Animatable(0f) }
    val sparklePulse = remember { Animatable(0.8f) }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            revealScale.animateTo(1.15f, tween(400, easing = FastOutSlowInEasing))
            revealScale.animateTo(1.05f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            rayRotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            sparklePulse.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Pinch to Zoom & Pan
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.85f, 2.5f)
                    val maxPanX = (zoomScale - 1f).coerceAtLeast(0f) * 220f
                    val maxPanY = (zoomScale - 1f).coerceAtLeast(0f) * 220f
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val blockSize = min(maxWidth.value, maxHeight.value * 0.90f).dp.coerceIn(280.dp, 360.dp)

        // Pinch to Zoom Tip Tooltip
        AnimatedVisibility(
            visible = showZoomTip && !isRevealed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GlacialBlueDark.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pinch to zoom in & out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Reset Zoom Button (appears if zoomed in)
        if (zoomScale > 1.05f) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .border(1.dp, CrystalFrostBevel, CircleShape)
            ) {
                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Zoom",
                        tint = GlacialBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Zoomable & Pannable Container
        Box(
            modifier = Modifier
                .size(blockSize)
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
                .testTag("ice_puzzle_container"),
            contentAlignment = Alignment.Center
        ) {
            val rewardRes = def.rewardDrawableRes ?: com.example.R.drawable.img_reward_hammer

            // ==========================================
            // LAYER 1: ACHIEVEMENT ELEMENT BEHIND THE ICE
            // ==========================================
            if (isRevealed) {
                // REVEALED STATE (Image 2):
                // Radiant golden rays + glowing sparkle stars + fully revealed floating tool!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    rotate(rayRotation.value) {
                        drawRadiantLightRays(size)
                    }
                    drawSparkleStars(size, sparklePulse.value)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = rewardRes),
                        contentDescription = def.rewardItemName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(blockSize * 0.65f)
                            .scale(revealScale.value)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEF3C7),
                        shadowElevation = 4.dp,
                        modifier = Modifier.border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "✨ ${def.rewardItemName} Unlocked! ✨",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                // TRAPPED STATE (Image 1):
                // Clean transparent PNG positioned completely behind the translucent ice block
                Image(
                    painter = painterResource(id = rewardRes),
                    contentDescription = def.rewardItemName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(blockSize * 0.62f)
                        .alpha(0.55f)
                )

                // ==========================================
                // LAYER 2: 3D TRANSLUCENT ICE BLOCK & CRACKS
                // ==========================================
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0x332D8FE8),
                            ambientColor = Color(0x1A2D8FE8)
                        )
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    // Translucent 3D crystal ice cube
                    drawIceCubeBlock(size)

                    // Progressive cracks that expand bit by bit with every arrow!
                    if (levelState.crackStage >= 1) {
                        drawDynamicIceCracks(levelState.crackStage, size)
                    }

                    // Touch ripple ring
                    tapRipplePos.value?.let { pos ->
                        if (tapRippleProgress.value in 0.01f..0.99f) {
                            val radius = 10f + tapRippleProgress.value * 34f
                            val alpha = (1f - tapRippleProgress.value) * 0.45f
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = alpha),
                                radius = radius,
                                center = pos,
                                style = Stroke(width = 3.5f)
                            )
                        }
                    }
                }

                // ==========================================
                // LAYER 3: PUZZLE ARROWS IN FRONT (THREAD UNSPOOLING)
                // ==========================================
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    val boardWidth = constraints.maxWidth.toFloat()
                    val boardHeight = constraints.maxHeight.toFloat()
                    val colStep = boardWidth / (def.cols + 1)
                    val rowStep = boardHeight / (def.rows + 1)

                    // Background Dot Grid (matching reference video Arrow-sort-reference.mp4)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dotColor = Color(0x35142038)
                        for (c in 0 until def.cols) {
                            for (r in 0 until def.rows) {
                                val cx = (c + 1) * colStep
                                val cy = (r + 1) * rowStep
                                drawCircle(
                                    color = dotColor,
                                    radius = 2.5f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }

                    // Alignment Grid Lines (matching Grid.mp4 [#] toggle feature)
                    if (levelState.isGridActive) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridLineColor = Color(0x77818CF8) // Soft lilac/blue guide line
                            val activeArrows = levelState.arrows.values.filter { !it.isRemoved }

                            // 1. Column and Row guide lines passing through active arrows
                            val activeCols = activeArrows.flatMap { it.arrow.points.map { pt -> pt.col } }.toSet()
                            val activeRows = activeArrows.flatMap { it.arrow.points.map { pt -> pt.row } }.toSet()

                            activeCols.forEach { col ->
                                val x = (col + 1) * colStep
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1.5f
                                )
                            }

                            activeRows.forEach { row ->
                                val y = (row + 1) * rowStep
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.5f
                                )
                            }

                            // 2. Alignment projection line in direction of each arrow
                            activeArrows.forEach { aState ->
                                val head = aState.arrow.head
                                val headOffset = Offset((head.col + 1) * colStep, (head.row + 1) * rowStep)
                                val forward = Offset(aState.arrow.direction.dx.toFloat(), aState.arrow.direction.dy.toFloat())
                                val projectEnd = headOffset + forward * maxOf(size.width, size.height)

                                drawLine(
                                    color = if (aState.isBlocked) Color(0x55F43F5E) else Color(0x882563EB),
                                    start = headOffset,
                                    end = projectEnd,
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(levelState, zoomScale, panOffset) {
                                detectTapGestures { tapOffset ->
                                    val activeArrows = levelState.arrows.values.filter { !it.isRemoved && !it.isExiting }
                                    var closestArrowId: String? = null
                                    var minDistance = Float.MAX_VALUE
                                    val tapThreshold = 52.dp.toPx()

                                    activeArrows.forEach { aState ->
                                        val pts = aState.arrow.points.map { pt ->
                                            Offset((pt.col + 1) * colStep, (pt.row + 1) * rowStep)
                                        }
                                        val dist = distanceToArrowPath(tapOffset, pts)
                                        if (dist <= tapThreshold && dist < minDistance) {
                                            minDistance = dist
                                            closestArrowId = aState.arrow.id
                                        }
                                    }

                                    closestArrowId?.let { id ->
                                        tapRipplePos.value = tapOffset
                                        onArrowTapped(id)
                                    }
                                }
                            }
                    ) {
                        // Animate touch ripple
                        LaunchedEffect(tapRipplePos.value) {
                            tapRipplePos.value?.let {
                                tapRippleProgress.snapTo(0f)
                                tapRippleProgress.animateTo(1f, tween(260, easing = LinearEasing))
                                tapRipplePos.value = null
                            }
                        }

                        // Render each arrow with thread unspooling animation
                        levelState.arrows.values.forEach { arrowState ->
                            if (!arrowState.isRemoved || arrowState.isExiting) {
                                ArrowItem(
                                    arrowState = arrowState,
                                    cols = def.cols,
                                    rows = def.rows,
                                    boardWidth = boardWidth,
                                    boardHeight = boardHeight,
                                    pulseScale = pulseAnim.value,
                                    onExitCompleted = { onArrowExitCompleted(arrowState.arrow.id) }
                                )
                            }
                        }

                        // Tutorial callout for Level 1
                        if (def.levelNumber == 1 && levelState.removedArrowsCount == 0) {
                            val centerArrow = levelState.arrows.values.firstOrNull { it.arrow.id == "l1_2" }
                            if (centerArrow != null && !centerArrow.isRemoved) {
                                TutorialBubble(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(y = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an arrow with THREAD UNSPOOLING animation:
 * - When tapped, the arrow unspools like a thread getting out of the box from curved lines to a straight line!
 * - Head extends forward along the exit direction
 * - Tail follows along the segments of the polyline, turning corners until the whole arrow straightens out and exits!
 * - Thick line with sharp directional arrowhead pointer on the side it will go (matching arrow-design.jpg)
 */
@Composable
private fun ArrowItem(
    arrowState: ActiveArrowState,
    cols: Int,
    rows: Int,
    boardWidth: Float,
    boardHeight: Float,
    pulseScale: Float,
    onExitCompleted: () -> Unit
) {
    val arrow = arrowState.arrow
    val isAvailable = !arrowState.isBlocked && !arrowState.isRemoved
    val isHinted = arrowState.isHighlighted

    val colStep = boardWidth / (cols + 1)
    val rowStep = boardHeight / (rows + 1)

    // Shake animation when blocked arrow is tapped
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(arrowState.shakeTrigger) {
        if (arrowState.shakeTrigger > 0) {
            shakeOffset.animateTo(12f, tween(30))
            shakeOffset.animateTo(-12f, tween(30))
            shakeOffset.animateTo(8f, tween(30))
            shakeOffset.animateTo(-8f, tween(30))
            shakeOffset.animateTo(0f, tween(30))
        }
    }

    // Continuous 60fps/120fps thread unspooling exit animation (matching Arrow-going-I-want.mp4)
    val exitAnim = remember { Animatable(0f) }
    LaunchedEffect(arrowState.isExiting) {
        if (arrowState.isExiting) {
            exitAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = LinearEasing)
            )
            onExitCompleted()
        }
    }

    // Base coordinates
    val originalPoints = remember(arrow.points, colStep, rowStep) {
        arrow.points.map { pt ->
            Offset((pt.col + 1) * colStep, (pt.row + 1) * rowStep)
        }
    }

    // Calculate unspooled thread polyline
    val boardExitSpan = maxOf(boardWidth, boardHeight) * 1.6f
    val unspooled = remember(originalPoints, arrow.direction, exitAnim.value) {
        calculateUnspooledThread(
            originalPoints = originalPoints,
            direction = arrow.direction,
            progress = exitAnim.value,
            boardExitDistance = boardExitSpan
        )
    }

    if (unspooled.isCompletelyExited) return

    val threadPoints = unspooled.points
    if (threadPoints.size < 2) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val arrowColor = when {
            arrowState.isExiting -> Color(0xFF2563EB) // Electric blue when unspooling (matching Arrow-going-I-want.mp4)
            arrowState.isBlocked && arrowState.shakeTrigger > 0 -> ErrorFracture
            isHinted -> AuroraGold
            else -> Color(0xFF142038) // Deep midnight navy matching arrow-design.jpg
        }

        val angleRad = arrow.direction.angleDegrees * PI / 180.0
        val forward = Offset(sin(angleRad).toFloat(), -cos(angleRad).toFloat())
        val normal = Offset(-forward.y, forward.x)

        // Arrowhead pointer dimensions: bolder pointer matching Arrow-going-I-want.mp4
        val headLength = 26f * (if (isHinted) pulseScale else 1f)
        val halfWingWidth = 14f * (if (isHinted) pulseScale else 1f)

        val tip = unspooled.headTip + forward * 4f
        val baseCenter = tip - forward * headLength
        val leftWing = baseCenter + normal * halfWingWidth
        val rightWing = baseCenter - normal * halfWingWidth

        // The shaft ends at baseCenter so it connects cleanly into the pointer
        val shaftPath = Path().apply {
            moveTo(threadPoints.first().x + shakeOffset.value, threadPoints.first().y)
            for (i in 1 until threadPoints.size - 1) {
                lineTo(threadPoints[i].x + shakeOffset.value, threadPoints[i].y)
            }
            lineTo(baseCenter.x + shakeOffset.value, baseCenter.y)
        }

        // Frosted slot groove behind the arrow in the ice block
        drawPath(
            path = shaftPath,
            color = if (arrowState.isExiting) Color(0x332563EB) else if (isAvailable) Color(0x33BCE7FF) else Color(0x15142038),
            style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Glow effect when hinted or moving
        if (isHinted) {
            drawPath(
                path = shaftPath,
                color = Color(0x88F3C75F),
                style = Stroke(width = 22f * pulseScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else if (arrowState.isExiting) {
            drawPath(
                path = shaftPath,
                color = Color(0x442563EB),
                style = Stroke(width = 22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Thinner, crisp arrow line (width 13f, rounded cap at tail, NO dots, matching user prompt)
        drawPath(
            path = shaftPath,
            color = arrowColor,
            style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // SHARP DIRECTIONAL ARROWHEAD POINTER AT THE FRONT (matching arrow-design.jpg)
        val arrowPointerPath = Path().apply {
            moveTo(tip.x + shakeOffset.value, tip.y)
            lineTo(leftWing.x + shakeOffset.value, leftWing.y)
            lineTo(baseCenter.x + forward.x * 4f + shakeOffset.value, baseCenter.y + forward.y * 4f)
            lineTo(rightWing.x + shakeOffset.value, rightWing.y)
            close()
        }

        // Pointer fill
        drawPath(path = arrowPointerPath, color = arrowColor)

        // Specular highlight outline
        drawPath(
            path = arrowPointerPath,
            color = if (arrowState.isExiting) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Thread unspooling math:
 * Models the arrow as a physical thread being pulled forward through bends/turns.
 * As the thread moves:
 * - The head moves in exitDirection
 * - The tail follows the polyline around corners
 * - Once the tail turns a corner, that bend disappears from the thread
 * - Once the tail passes the original head, the whole thread straightens into a single line!
 */
private data class UnspooledThread(
    val points: List<Offset>,
    val headTip: Offset,
    val isCompletelyExited: Boolean
)

private fun calculateUnspooledThread(
    originalPoints: List<Offset>,
    direction: ArrowDirection,
    progress: Float,
    boardExitDistance: Float
): UnspooledThread {
    if (originalPoints.isEmpty()) {
        return UnspooledThread(emptyList(), Offset.Zero, true)
    }
    val f = Offset(direction.dx.toFloat(), direction.dy.toFloat())

    if (originalPoints.size == 1) {
        val travel = progress * boardExitDistance
        val tip = originalPoints[0] + f * travel
        return UnspooledThread(listOf(originalPoints[0], tip), tip, progress >= 1f)
    }

    // Cumulative segment lengths
    val segLengths = FloatArray(originalPoints.size - 1)
    val cumDist = FloatArray(originalPoints.size)
    cumDist[0] = 0f
    for (i in 0 until originalPoints.size - 1) {
        val d = (originalPoints[i + 1] - originalPoints[i]).getDistance()
        segLengths[i] = d
        cumDist[i + 1] = cumDist[i] + d
    }
    val totalLength = cumDist.last()

    val totalTravel = totalLength + boardExitDistance
    val currentDistance = progress * totalTravel

    // The head is at originalPoints.last() + f * currentDistance
    val headTip = originalPoints.last() + f * currentDistance

    // Check if the entire thread has completely cleared the original polyline
    if (currentDistance >= totalLength) {
        val tailDistPastHead = currentDistance - totalLength
        val tailPoint = originalPoints.last() + f * tailDistPastHead

        // The thread has completely straightened out into a straight line!
        return UnspooledThread(
            points = listOf(tailPoint, headTip),
            headTip = headTip,
            isCompletelyExited = progress >= 0.98f
        )
    }

    // The tail is still traversing the original polyline at distance currentDistance
    var tailSegIdx = 0
    while (tailSegIdx < segLengths.size - 1 && cumDist[tailSegIdx + 1] < currentDistance) {
        tailSegIdx++
    }

    val segStartDist = cumDist[tailSegIdx]
    val segLen = segLengths[tailSegIdx]
    val t = if (segLen > 0f) ((currentDistance - segStartDist) / segLen).coerceIn(0f, 1f) else 0f
    val tailPos = originalPoints[tailSegIdx] + (originalPoints[tailSegIdx + 1] - originalPoints[tailSegIdx]) * t

    val threadPoints = mutableListOf<Offset>()
    threadPoints.add(tailPos)

    // Add remaining intermediate original vertices between tailSegIdx + 1 and the end
    for (i in (tailSegIdx + 1) until originalPoints.size) {
        threadPoints.add(originalPoints[i])
    }

    // Extend forward to current headTip
    if (currentDistance > 0.001f) {
        threadPoints.add(headTip)
    }

    return UnspooledThread(
        points = threadPoints,
        headTip = headTip,
        isCompletelyExited = false
    )
}

/**
 * Draws the 3D crystal ice cube block with chamfered crystal bevels and glossy light sheen.
 */
private fun DrawScope.drawIceCubeBlock(size: Size) {
    val cornerRadius = CornerRadius(30f, 30f)

    // 1. Crystal clear ice gradient volume
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xCCE8F8FF), // Top glossy ice sheen
                Color(0xB3D0EFFF), // Mid crystalline frost
                Color(0xCCBCE7FF)  // Base icy depth
            )
        ),
        size = size,
        cornerRadius = cornerRadius
    )

    // 2. Translucent diagonal reflection facets
    val facet1 = Path().apply {
        moveTo(0f, size.height * 0.25f)
        lineTo(size.width * 0.40f, 0f)
        lineTo(size.width * 0.58f, 0f)
        lineTo(0f, size.height * 0.50f)
        close()
    }
    drawPath(facet1, Color.White.copy(alpha = 0.40f))

    val facet2 = Path().apply {
        moveTo(size.width * 0.35f, size.height)
        lineTo(size.width, size.height * 0.38f)
        lineTo(size.width, size.height * 0.52f)
        lineTo(size.width * 0.50f, size.height)
        close()
    }
    drawPath(facet2, Color(0xFF9BE7FF).copy(alpha = 0.25f))

    // 3. 3D Crystal Bevel: specular white highlight on top/left, deeper cyan frost on bottom/right
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFBCE7FF),
                Color(0xFF38BDF8).copy(alpha = 0.65f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        ),
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 3.5f)
    )

    // 4. Subtle frosted ice grid dots
    val dotSpacing = 26f
    var x = dotSpacing
    while (x < size.width) {
        var y = dotSpacing
        while (y < size.height) {
            drawCircle(
                color = Color(0x1F2D8FE8),
                radius = 1.4f,
                center = Offset(x, y)
            )
            y += dotSpacing
        }
        x += dotSpacing
    }
}

/**
 * Draws dynamic ice cracks that expand with every arrow that leaves,
 * and completely cracks open the ice block on the last arrow!
 */
private fun DrawScope.drawDynamicIceCracks(stage: Int, size: Size) {
    val crackCoreColor = Color.White
    val crackGlowColor = Color(0xFF67E8F9)
    val w = size.width
    val h = size.height

    fun drawCrackLine(points: List<Offset>, strokeW: Float) {
        val p = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(p, crackGlowColor.copy(alpha = 0.65f), style = Stroke(strokeW + 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, crackCoreColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    if (stage >= 1) {
        drawCrackLine(
            listOf(
                Offset(w * 0.12f, h * 0.18f),
                Offset(w * 0.32f, h * 0.28f),
                Offset(w * 0.44f, h * 0.24f),
                Offset(w * 0.58f, h * 0.34f)
            ),
            2.5f
        )
    }

    if (stage >= 2) {
        drawCrackLine(
            listOf(
                Offset(w * 0.44f, h * 0.24f),
                Offset(w * 0.52f, h * 0.14f),
                Offset(w * 0.72f, h * 0.10f)
            ),
            2f
        )
        drawCrackLine(
            listOf(
                Offset(w * 0.88f, h * 0.68f),
                Offset(w * 0.68f, h * 0.60f),
                Offset(w * 0.54f, h * 0.74f)
            ),
            2.5f
        )
    }

    if (stage >= 3) {
        drawCrackLine(
            listOf(
                Offset(w * 0.08f, h * 0.88f),
                Offset(w * 0.28f, h * 0.72f),
                Offset(w * 0.42f, h * 0.56f),
                Offset(w * 0.62f, h * 0.50f),
                Offset(w * 0.80f, h * 0.32f),
                Offset(w * 0.92f, h * 0.18f)
            ),
            3.5f
        )
    }

    if (stage >= 4) {
        drawCrackLine(
            listOf(
                Offset(w * 0.28f, h * 0.72f),
                Offset(w * 0.18f, h * 0.62f),
                Offset(w * 0.14f, h * 0.46f)
            ),
            2.2f
        )
        drawCrackLine(
            listOf(
                Offset(w * 0.62f, h * 0.50f),
                Offset(w * 0.70f, h * 0.66f),
                Offset(w * 0.84f, h * 0.82f)
            ),
            2.8f
        )
    }

    if (stage >= 5) {
        drawCrackLine(
            listOf(
                Offset(w * 0.50f, 0f),
                Offset(w * 0.46f, h * 0.25f),
                Offset(w * 0.54f, h * 0.50f),
                Offset(w * 0.48f, h * 0.75f),
                Offset(w * 0.50f, h)
            ),
            5.5f
        )
        drawCrackLine(
            listOf(
                Offset(0f, h * 0.52f),
                Offset(w * 0.26f, h * 0.48f),
                Offset(w * 0.54f, h * 0.50f),
                Offset(w * 0.74f, h * 0.53f),
                Offset(w, h * 0.51f)
            ),
            5.5f
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = 50f,
            center = Offset(w * 0.54f, h * 0.50f)
        )
    }
}

/**
 * Draws radiant golden light rays bursting outward from the revealed tool (Image 2).
 */
private fun DrawScope.drawRadiantLightRays(size: Size) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.width * 0.75f
    val rayColor = Color(0xFFFDE68A).copy(alpha = 0.40f)

    for (i in 0 until 12) {
        val angleDeg = i * 30f
        val rad = angleDeg * PI / 180f
        val spread = 0.12f

        val path = Path().apply {
            moveTo(center.x, center.y)
            lineTo(
                center.x + (cos(rad - spread) * maxRadius).toFloat(),
                center.y + (sin(rad - spread) * maxRadius).toFloat()
            )
            lineTo(
                center.x + (cos(rad + spread) * maxRadius).toFloat(),
                center.y + (sin(rad + spread) * maxRadius).toFloat()
            )
            close()
        }
        drawPath(path, rayColor)
    }
}

/**
 * Draws golden sparkle stars (✦ ✦ ✦) around the revealed achievement tool (Image 2).
 */
private fun DrawScope.drawSparkleStars(size: Size, pulse: Float) {
    val starColor = Color(0xFFFBBF24)
    val cx = size.width / 2f
    val cy = size.height / 2f

    val starPositions = listOf(
        Offset(cx - 75f, cy - 65f) to 14f * pulse,
        Offset(cx + 80f, cy - 60f) to 16f * pulse,
        Offset(cx - 85f, cy + 30f) to 12f * pulse,
        Offset(cx + 70f, cy + 45f) to 15f * pulse,
        Offset(cx - 30f, cy - 90f) to 10f * pulse,
        Offset(cx + 40f, cy + 85f) to 11f * pulse
    )

    starPositions.forEach { (pos, starSize) ->
        val p = Path().apply {
            moveTo(pos.x, pos.y - starSize)
            quadraticTo(pos.x, pos.y, pos.x + starSize, pos.y)
            quadraticTo(pos.x, pos.y, pos.x, pos.y + starSize)
            quadraticTo(pos.x, pos.y, pos.x - starSize, pos.y)
            quadraticTo(pos.x, pos.y, pos.x, pos.y - starSize)
            close()
        }
        drawPath(p, starColor)
        drawPath(p, Color.White.copy(alpha = 0.8f), style = Stroke(width = 1f))
    }
}

private fun distanceToArrowPath(tap: Offset, points: List<Offset>): Float {
    if (points.isEmpty()) return Float.MAX_VALUE
    if (points.size == 1) return (tap - points.first()).getDistance()
    var minDist = Float.MAX_VALUE
    for (i in 0 until points.size - 1) {
        val dist = distanceToSegment(tap, points[i], points[i + 1])
        if (dist < minDist) minDist = dist
    }
    return minDist
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
    if (l2 == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
    val projection = Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
    return (p - projection).getDistance()
}

@Composable
private fun TutorialBubble(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = "Tap Arrow",
            tint = GlacialBlue,
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlacialBlueDark,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to remove",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
