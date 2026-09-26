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
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArrowDirection
import com.example.data.ArrowModel
import com.example.data.GridPoint
import com.example.data.LevelGenerator
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
import kotlin.random.Random

/**
 * Middle Section:
 * 1. Reward item frozen inside a chunky, beveled 3D ice cube (frosted corners, crackle veins)
 * 2. The ice block that cracks bit-by-bit with every arrow, and cracks open completely on the last arrow
 * 3. Thin arrow line with a bold, sharp directional arrowhead pointer
 * 4. Snake-style exit: the arrow slides out along its own path at a constant speed and leaves the screen
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
    LaunchedEffect(levelState.sessionId) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    // Zoom tip visibility (shown on Level 10+ or complex levels)
    var showZoomTip by remember { mutableStateOf(def.levelNumber >= 10) }
    LaunchedEffect(levelState.sessionId) {
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

    // Level intro: arrows draw themselves in when a level starts
    val introAnim = remember { Animatable(0f) }
    LaunchedEffect(levelState.sessionId) {
        introAnim.snapTo(0f)
        introAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
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
                // TRAPPED STATE: the reward is frozen inside a chunky 3D ice cube.
                // Layer order: ice body (back) -> reward item -> frosted ice face (front) -> arrows.
                val iceShape = RoundedCornerShape(blockSize * IceCornerFraction)
                val rimInset = blockSize * IceRimFraction
                val arrowPadding = rimInset + 4.dp

                // ==========================================
                // LAYER 2a: ICE CUBE BODY (BEHIND THE REWARD)
                // ==========================================
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 18.dp,
                            shape = iceShape,
                            spotColor = Color(0x552D8FE8),
                            ambientColor = Color(0x332D8FE8)
                        )
                        .clip(iceShape)
                ) {
                    drawIceCubeBody(size, rimInset.toPx())
                }

                // ==========================================
                // LAYER 2b: REWARD ITEM FROZEN INSIDE THE ICE
                // ==========================================
                Image(
                    painter = painterResource(id = rewardRes),
                    contentDescription = def.rewardItemName,
                    contentScale = ContentScale.Fit,
                    // Multiply with a pale ice blue so the item looks tinted by the ice around it
                    colorFilter = ColorFilter.tint(Color(0xFFD6F0FF), BlendMode.Modulate),
                    modifier = Modifier
                        .size(blockSize * 0.70f)
                        .alpha(0.92f)
                )

                // ==========================================
                // LAYER 2c: FROSTED ICE FACE, VEINS & CRACKS (IN FRONT OF THE REWARD)
                // ==========================================
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(iceShape)
                ) {
                    drawIceCubeFront(size, rimInset.toPx())

                    // Progressive cracks that expand bit by bit with every arrow!
                    if (levelState.crackStage >= 1) {
                        drawDynamicIceCracks(levelState.crackStage, size)
                    }

                    // Touch ripple ring (tap position is relative to the padded arrow layer)
                    tapRipplePos.value?.let { pos ->
                        if (tapRippleProgress.value in 0.01f..0.99f) {
                            val radius = 10f + tapRippleProgress.value * 34f
                            val alpha = (1f - tapRippleProgress.value) * 0.45f
                            val pad = arrowPadding.toPx()
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = alpha),
                                radius = radius,
                                center = pos + Offset(pad, pad),
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
                        .padding(arrowPadding)
                ) {
                    val boardWidth = constraints.maxWidth.toFloat()
                    val boardHeight = constraints.maxHeight.toFloat()
                    val density = LocalDensity.current
                    val grid = remember(def.cols, def.rows, boardWidth, boardHeight, density) {
                        BoardGrid.fit(def.cols, def.rows, boardWidth, boardHeight, with(density) { MaxDotSpacing.toPx() })
                    }

                    // Which dot belongs to which arrow (arrows already leaving don't block anyone)
                    val occupancy = remember(levelState.arrows) {
                        val map = HashMap<GridPoint, String>()
                        levelState.arrows.values
                            .filter { !it.isRemoved && !it.isExiting }
                            .forEach { s -> LevelGenerator.cellsOf(s.arrow).forEach { map[it] = s.arrow.id } }
                        map
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
                                val x = grid.x(col)
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1.5f
                                )
                            }

                            activeRows.forEach { row ->
                                val y = grid.y(row)
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.5f
                                )
                            }

                            // 2. Alignment projection line in direction of each arrow
                            activeArrows.forEach { aState ->
                                val headOffset = grid.offset(aState.arrow.head)
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
                            .pointerInput(levelState, zoomScale, panOffset, grid) {
                                detectTapGestures { tapOffset ->
                                    val activeArrows = levelState.arrows.values.filter { !it.isRemoved && !it.isExiting }
                                    var closestArrowId: String? = null
                                    var minDistance = Float.MAX_VALUE
                                    // Generous on sparse boards, tighter on dense ones so the nearest arrow wins
                                    val tapThreshold = (grid.step * 0.9f).coerceIn(24.dp.toPx(), 52.dp.toPx())

                                    activeArrows.forEach { aState ->
                                        val pts = aState.arrow.points.map { grid.offset(it) }
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

                        levelState.arrows.values.forEach { arrowState ->
                            if (!arrowState.isRemoved || arrowState.isExiting) {
                                key(levelState.sessionId, arrowState.arrow.id) {
                                    ArrowItem(
                                        arrowState = arrowState,
                                        grid = grid,
                                        boardWidth = boardWidth,
                                        boardHeight = boardHeight,
                                        freeCellsAhead = freeCellsAhead(arrowState.arrow, occupancy, def.cols, def.rows),
                                        introProgress = introAnim.value,
                                        pulseScale = pulseAnim.value,
                                        onExitCompleted = { onArrowExitCompleted(arrowState.arrow.id) }
                                    )
                                }
                            }
                        }

                        // Tutorial callout for Level 1
                        if (def.levelNumber == 1 && !def.isDaily && levelState.removedArrowsCount == 0) {
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

/** Largest gap between neighbouring dots, so small early levels stay compact instead of stretching out. */
private val MaxDotSpacing = 34.dp

/** Speed arrows travel at when they slide out, like the reference game. */
private const val ExitSpeedDpPerMs = 1.25f

/** Maps grid dots to pixel positions: one uniform spacing, centred in the board. */
private data class BoardGrid(val step: Float, val originX: Float, val originY: Float) {
    fun x(col: Int) = originX + col * step
    fun y(row: Int) = originY + row * step
    fun offset(pt: GridPoint) = Offset(x(pt.col), y(pt.row))

    companion object {
        fun fit(cols: Int, rows: Int, width: Float, height: Float, maxStep: Float): BoardGrid {
            val step = minOf(width / (cols + 1), height / (rows + 1), maxStep)
            return BoardGrid(
                step = step,
                originX = (width - step * (cols - 1)) / 2f,
                originY = (height - step * (rows - 1)) / 2f
            )
        }
    }
}

/** Number of empty dots between an arrow's head and the first arrow in its way (or the edge). */
private fun freeCellsAhead(arrow: ArrowModel, occupancy: Map<GridPoint, String>, cols: Int, rows: Int): Int {
    var c = arrow.head.col + arrow.direction.dx
    var r = arrow.head.row + arrow.direction.dy
    var free = 0
    while (c in 0 until cols && r in 0 until rows) {
        val who = occupancy[GridPoint(c, r)]
        if (who != null && who != arrow.id) return free
        free++
        c += arrow.direction.dx
        r += arrow.direction.dy
    }
    return free
}

/**
 * Renders one arrow and its animations:
 * - Intro: the line draws itself in from the tail when the level starts.
 * - Exit: it turns blue and slides out snake-style along its own path at a constant speed,
 *   then keeps going until it has left the screen (like the reference game).
 * - Wrong tap: it turns red, bumps forward into the arrow that blocks it and springs back.
 *   It stays red, so the player can see which arrow already cost a life.
 */
@Composable
private fun ArrowItem(
    arrowState: ActiveArrowState,
    grid: BoardGrid,
    boardWidth: Float,
    boardHeight: Float,
    freeCellsAhead: Int,
    introProgress: Float,
    pulseScale: Float,
    onExitCompleted: () -> Unit
) {
    val arrow = arrowState.arrow
    val isAvailable = !arrowState.isBlocked && !arrowState.isRemoved
    val isHinted = arrowState.isHighlighted
    val density = LocalDensity.current

    val originalPoints = remember(arrow.points, grid) { arrow.points.map { grid.offset(it) } }
    val pathLength = remember(originalPoints) {
        (0 until originalPoints.size - 1).sumOf { (originalPoints[it + 1] - originalPoints[it]).getDistance().toDouble() }.toFloat()
    }

    // How far the snake has travelled along its path (px). Drives both the bump and the exit.
    val travel = remember { Animatable(0f) }

    // Bump into the blocker and back when a blocked arrow is tapped
    LaunchedEffect(arrowState.shakeTrigger) {
        if (arrowState.shakeTrigger > 0 && !arrowState.isExiting) {
            val bump = (freeCellsAhead + 0.45f) * grid.step
            travel.snapTo(0f)
            travel.animateTo(bump, tween(durationMillis = (90 + freeCellsAhead * 25).coerceAtMost(200), easing = FastOutSlowInEasing))
            travel.animateTo(0f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
        }
    }

    // Exit: slide along the path and all the way off-screen at a constant speed
    val exitDistance = remember(originalPoints, boardWidth, boardHeight) {
        val head = originalPoints.last()
        val toEdge = when (arrow.direction) {
            ArrowDirection.UP -> head.y
            ArrowDirection.DOWN -> boardHeight - head.y
            ArrowDirection.LEFT -> head.x
            ArrowDirection.RIGHT -> boardWidth - head.x
        }
        // Past the board edge, keep going far enough to clear the rest of the screen
        pathLength + toEdge + maxOf(boardWidth, boardHeight) * 1.3f
    }
    LaunchedEffect(arrowState.isExiting) {
        if (arrowState.isExiting) {
            val remaining = exitDistance - travel.value
            val speedPxPerMs = with(density) { ExitSpeedDpPerMs.dp.toPx() }
            val duration = (remaining / speedPxPerMs).toInt().coerceIn(220, 750)
            travel.animateTo(exitDistance, tween(durationMillis = duration, easing = LinearEasing))
            onExitCompleted()
        }
    }

    val unspooled = remember(originalPoints, arrow.direction, travel.value) {
        calculateUnspooledThread(
            originalPoints = originalPoints,
            direction = arrow.direction,
            travel = travel.value
        )
    }
    if (arrowState.isExiting && travel.value >= exitDistance - 0.5f) return

    // Intro: reveal the line from the tail towards the head
    val drawn = if (introProgress < 1f) trimPolyline(unspooled.points, pathLength * introProgress) else unspooled.points
    if (drawn.size < 2) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val arrowColor = when {
            arrowState.isExiting -> Color(0xFF2563EB) // Electric blue while sliding out
            isHinted -> AuroraGold
            arrowState.isWrong -> Color(0xFFE5484D) // Stays red after costing a life
            else -> Color(0xFF0F1B3D) // Deep midnight navy
        }

        val angleRad = arrow.direction.angleDegrees * PI / 180.0
        val forward = Offset(sin(angleRad).toFloat(), -cos(angleRad).toFloat())
        val normal = Offset(-forward.y, forward.x)

        // Thin line + bold, sharp triangular pointer (sized in dp so it looks the same on every screen)
        val headScale = (if (isHinted) pulseScale else 1f) * ((introProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
        val shaftWidth = 3.2.dp.toPx()
        val headLength = 10.dp.toPx() * headScale
        val halfWingWidth = 6.5.dp.toPx() * headScale

        val tip = unspooled.headTip + forward * 3.dp.toPx() * headScale
        val baseCenter = tip - forward * headLength
        val leftWing = baseCenter + normal * halfWingWidth
        val rightWing = baseCenter - normal * halfWingWidth

        // The shaft runs slightly into the pointer so there is no visible seam
        val shaftEnd = baseCenter + forward * 1.dp.toPx() * headScale
        val shaftPath = Path().apply {
            moveTo(drawn.first().x, drawn.first().y)
            for (i in 1 until drawn.size - 1) {
                lineTo(drawn[i].x, drawn[i].y)
            }
            val last = if (introProgress < 1f) drawn.last() else shaftEnd
            lineTo(last.x, last.y)
        }

        // Glow effect when hinted or moving
        if (isHinted) {
            drawPath(
                path = shaftPath,
                color = Color(0x88F3C75F),
                style = Stroke(width = 10.dp.toPx() * pulseScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else if (arrowState.isExiting) {
            drawPath(
                path = shaftPath,
                color = Color(0x442563EB),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else if (isAvailable || arrowState.isWrong) {
            // Faint frosty halo so the thin line stays readable over the frozen reward
            drawPath(
                path = shaftPath,
                color = Color.White.copy(alpha = 0.30f),
                style = Stroke(width = shaftWidth + 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Thin, crisp arrow line
        drawPath(
            path = shaftPath,
            color = arrowColor,
            style = Stroke(width = shaftWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (headScale > 0f) {
            // Bold, sharp directional arrowhead
            val arrowPointerPath = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(leftWing.x, leftWing.y)
                lineTo(rightWing.x, rightWing.y)
                close()
            }
            drawPath(path = arrowPointerPath, color = arrowColor)
            // Thin stroke in the same color rounds off the very tips so they don't look jagged
            drawPath(
                path = arrowPointerPath,
                color = arrowColor,
                style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
            )
        }
    }
}

/** Keeps the first [length] px of a polyline. */
private fun trimPolyline(points: List<Offset>, length: Float): List<Offset> {
    if (points.size < 2 || length <= 0f) return emptyList()
    val out = mutableListOf(points.first())
    var left = length
    for (i in 0 until points.size - 1) {
        val seg = (points[i + 1] - points[i]).getDistance()
        if (seg >= left) {
            val t = if (seg > 0f) left / seg else 0f
            out.add(points[i] + (points[i + 1] - points[i]) * t)
            return out
        }
        out.add(points[i + 1])
        left -= seg
    }
    return out
}

/**
 * Snake movement: the arrow slides along its own path.
 * After travelling [travel] px, the head has moved that far forward in its exit direction,
 * and the tail has followed the path the same distance, turning the same corners.
 */
private data class UnspooledThread(
    val points: List<Offset>,
    val headTip: Offset
)

private fun calculateUnspooledThread(
    originalPoints: List<Offset>,
    direction: ArrowDirection,
    travel: Float
): UnspooledThread {
    if (originalPoints.isEmpty()) return UnspooledThread(emptyList(), Offset.Zero)
    val f = Offset(direction.dx.toFloat(), direction.dy.toFloat())
    val headTip = originalPoints.last() + f * travel
    if (originalPoints.size == 1) return UnspooledThread(listOf(originalPoints[0], headTip), headTip)

    // Cumulative segment lengths along the original path
    val cumDist = FloatArray(originalPoints.size)
    for (i in 0 until originalPoints.size - 1) {
        cumDist[i + 1] = cumDist[i] + (originalPoints[i + 1] - originalPoints[i]).getDistance()
    }
    val totalLength = cumDist.last()

    // Tail has left the original path: the arrow is now a straight line in front of the old head
    if (travel >= totalLength) {
        val tail = originalPoints.last() + f * (travel - totalLength)
        return UnspooledThread(listOf(tail, headTip), headTip)
    }

    // Tail is still on the original path
    var seg = 0
    while (seg < originalPoints.size - 2 && cumDist[seg + 1] < travel) seg++
    val segLen = cumDist[seg + 1] - cumDist[seg]
    val t = if (segLen > 0f) ((travel - cumDist[seg]) / segLen).coerceIn(0f, 1f) else 0f
    val tailPos = originalPoints[seg] + (originalPoints[seg + 1] - originalPoints[seg]) * t

    val points = mutableListOf(tailPos)
    for (i in (seg + 1) until originalPoints.size) points.add(originalPoints[i])
    if (travel > 0.001f) points.add(headTip)
    return UnspooledThread(points, headTip)
}

/** Corner radius of the ice cube, as a fraction of its size. */
private const val IceCornerFraction = 0.11f

/** Width of the beveled ice rim around the inner face, as a fraction of the cube size. */
private const val IceRimFraction = 0.075f

/**
 * Draws the solid body of the 3D ice cube (behind the frozen reward):
 * a chunky beveled rim (light on top/left, deeper blue on bottom/right) around a recessed inner face.
 */
private fun DrawScope.drawIceCubeBody(size: Size, rim: Float) {
    val w = size.width
    val h = size.height
    val outerRadius = w * IceCornerFraction

    // 1. Outer ice volume
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFD2F1FF), Color(0xFF86CBF3), Color(0xFF3E97D8)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        size = size,
        cornerRadius = CornerRadius(outerRadius, outerRadius)
    )

    // 2. Bevel faces between the outer edge and the inner face (clipped by the rounded canvas)
    fun bevel(color: Color, pts: List<Offset>) {
        val p = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(p, color)
    }
    val tl = Offset(0f, 0f)
    val tr = Offset(w, 0f)
    val br = Offset(w, h)
    val bl = Offset(0f, h)
    val itl = Offset(rim, rim)
    val itr = Offset(w - rim, rim)
    val ibr = Offset(w - rim, h - rim)
    val ibl = Offset(rim, h - rim)
    bevel(Color(0xB3F2FBFF), listOf(tl, tr, itr, itl))   // top: brightest
    bevel(Color(0x80E0F5FF), listOf(tl, itl, ibl, bl))   // left: light
    bevel(Color(0x593E9FDB), listOf(tr, br, ibr, itr))   // right: shaded
    bevel(Color(0x733489C9), listOf(bl, ibl, ibr, br))   // bottom: deepest

    // Crystal facets chipped into the rim so it reads as carved ice rather than smooth plastic
    val rnd = Random(7)
    repeat(56) { i ->
        val side = i % 4
        val t = rnd.nextFloat()
        val along = w * (0.08f + t * 0.84f)
        val depth = rim * (0.35f + rnd.nextFloat() * 0.65f)
        val spread = w * (0.03f + rnd.nextFloat() * 0.06f)
        val (a, b, c) = when (side) {
            0 -> Triple(Offset(along - spread, 0f), Offset(along + spread, 0f), Offset(along + spread * 0.3f, depth))
            1 -> Triple(Offset(w, along - spread), Offset(w, along + spread), Offset(w - depth, along - spread * 0.3f))
            2 -> Triple(Offset(along - spread, h), Offset(along + spread, h), Offset(along - spread * 0.3f, h - depth))
            else -> Triple(Offset(0f, along - spread), Offset(0f, along + spread), Offset(depth, along + spread * 0.3f))
        }
        val facetColor = if (rnd.nextBoolean()) Color.White.copy(alpha = 0.10f + rnd.nextFloat() * 0.25f)
        else Color(0xFF2F86C8).copy(alpha = 0.06f + rnd.nextFloat() * 0.14f)
        bevel(facetColor, listOf(a, b, c))
    }

    // 3. Recessed inner face: clear pale ice, brighter in the middle
    val innerSize = Size(w - rim * 2, h - rim * 2)
    val innerRadius = (outerRadius - rim * 0.5f).coerceAtLeast(4f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFC2E8FC), Color(0xFFA6DAF6), Color(0xFF8CCBF0))
        ),
        topLeft = itl,
        size = innerSize,
        cornerRadius = CornerRadius(innerRadius, innerRadius)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x99F4FBFF), Color(0x00F4FBFF)),
            center = Offset(w * 0.5f, h * 0.48f),
            radius = w * 0.42f
        ),
        radius = w * 0.42f,
        center = Offset(w * 0.5f, h * 0.48f)
    )
}

/**
 * Draws the translucent front of the ice cube (in front of the frozen reward):
 * a frosty wash, internal crackle veins, glossy reflections, tiny bubbles,
 * frosted corners and bright bevel edge highlights.
 */
private fun DrawScope.drawIceCubeFront(size: Size, rim: Float) {
    val w = size.width
    val h = size.height
    val outerRadius = w * IceCornerFraction
    val innerRadius = (outerRadius - rim * 0.5f).coerceAtLeast(4f)
    val innerTopLeft = Offset(rim, rim)
    val innerSize = Size(w - rim * 2, h - rim * 2)

    // 1. Frost wash so the reward reads as being *inside* the ice
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x4DEAF8FF), Color(0x1AEAF8FF), Color(0x40A9DCF7))
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(innerRadius, innerRadius)
    )

    // 2. Internal crackle veins (fixed seed so the pattern never flickers between frames)
    val rnd = Random(20260926)
    val veinStroke = 1.dp.toPx()
    repeat(22) {
        var x = rim + rnd.nextFloat() * innerSize.width
        var y = rim + rnd.nextFloat() * innerSize.height
        val p = Path().apply { moveTo(x, y) }
        var angle = rnd.nextFloat() * 2f * PI.toFloat()
        repeat(2 + rnd.nextInt(4)) {
            angle += (rnd.nextFloat() - 0.5f) * 1.6f
            val len = w * (0.03f + rnd.nextFloat() * 0.09f)
            x = (x + cos(angle) * len).coerceIn(rim, w - rim)
            y = (y + sin(angle) * len).coerceIn(rim, h - rim)
            p.lineTo(x, y)
        }
        val alpha = 0.25f + rnd.nextFloat() * 0.40f
        // Soft blue shadow under a bright white hairline gives the vein some depth
        drawPath(p, Color(0xFF5BB4EA).copy(alpha = alpha * 0.45f), style = Stroke(veinStroke * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Color.White.copy(alpha = alpha), style = Stroke(veinStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // 3. Glossy diagonal reflections
    val gloss1 = Path().apply {
        moveTo(rim, h * 0.34f)
        lineTo(w * 0.36f, rim)
        lineTo(w * 0.50f, rim)
        lineTo(rim, h * 0.50f)
        close()
    }
    drawPath(gloss1, Color.White.copy(alpha = 0.22f))
    val gloss2 = Path().apply {
        moveTo(w * 0.58f, h - rim)
        lineTo(w - rim, h * 0.60f)
        lineTo(w - rim, h * 0.68f)
        lineTo(w * 0.66f, h - rim)
        close()
    }
    drawPath(gloss2, Color.White.copy(alpha = 0.16f))

    // 4. Tiny trapped air bubbles
    repeat(7) {
        val c = Offset(rim + rnd.nextFloat() * innerSize.width, rim + rnd.nextFloat() * innerSize.height)
        val r = (1.5f + rnd.nextFloat() * 2.5f).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.20f), radius = r, center = c)
        drawCircle(Color.White.copy(alpha = 0.65f), radius = r, center = c, style = Stroke(0.8.dp.toPx()))
    }

    // 5. Frosted corners (snowy white glow + feathery frost crystals creeping in from each corner)
    val frostRadius = w * 0.26f
    val corners = listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h), Offset(w, h))
    corners.forEach { corner ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xF2FFFFFF), Color(0x80FFFFFF), Color(0x00FFFFFF)),
                center = corner,
                radius = frostRadius
            ),
            radius = frostRadius,
            center = corner
        )
    }
    val fernStroke = 0.9.dp.toPx()
    corners.forEach { corner ->
        // Rays fan out towards the middle of the cube
        val baseAngle = kotlin.math.atan2(h / 2f - corner.y, w / 2f - corner.x)
        repeat(7) {
            val angle = baseAngle + (rnd.nextFloat() - 0.5f) * 1.5f
            val len = frostRadius * (0.45f + rnd.nextFloat() * 0.55f)
            val dir = Offset(cos(angle), sin(angle))
            val end = corner + dir * len
            val alpha = 0.45f + rnd.nextFloat() * 0.4f
            drawLine(Color.White.copy(alpha = alpha), corner, end, fernStroke, cap = StrokeCap.Round)
            // Small side branches like frost ferns
            var d = len * 0.25f
            while (d < len * 0.9f) {
                val from = corner + dir * d
                val branch = len * (0.10f + rnd.nextFloat() * 0.12f)
                for (sign in listOf(-1f, 1f)) {
                    val ba = angle + sign * 0.7f
                    drawLine(
                        Color.White.copy(alpha = alpha * 0.8f),
                        from,
                        from + Offset(cos(ba), sin(ba)) * branch,
                        fernStroke * 0.8f,
                        cap = StrokeCap.Round
                    )
                }
                d += len * (0.14f + rnd.nextFloat() * 0.1f)
            }
        }
    }
    repeat(60) {
        // Frost speckles clustered near the corners
        val corner = corners[rnd.nextInt(4)]
        val dist = rnd.nextFloat() * frostRadius * 0.9f
        val a = rnd.nextFloat() * 2f * PI.toFloat()
        val c = Offset(
            (corner.x + cos(a) * dist).coerceIn(0f, w),
            (corner.y + sin(a) * dist).coerceIn(0f, h)
        )
        drawCircle(Color.White.copy(alpha = 0.5f + rnd.nextFloat() * 0.5f), radius = (0.6f + rnd.nextFloat()).dp.toPx(), center = c)
    }

    // 6. Bevel creases from outer corners to inner corners
    val creaseColor = Color.White.copy(alpha = 0.55f)
    val crease = 1.2.dp.toPx()
    val cornerPull = outerRadius * 0.30f
    drawLine(creaseColor, Offset(cornerPull, cornerPull), Offset(rim, rim), crease)
    drawLine(creaseColor, Offset(w - cornerPull, cornerPull), Offset(w - rim, rim), crease)
    drawLine(creaseColor, Offset(cornerPull, h - cornerPull), Offset(rim, h - rim), crease)
    drawLine(creaseColor, Offset(w - cornerPull, h - cornerPull), Offset(w - rim, h - rim), crease)

    // 7. Edge highlights: bright inner face edge and glossy outer rim
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.45f), Color(0xFF7CC8F2).copy(alpha = 0.8f)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = CornerRadius(innerRadius, innerRadius),
        style = Stroke(width = 1.6.dp.toPx())
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White, Color(0xFFBCE7FF), Color(0xFF38A3E0)),
            start = Offset.Zero,
            end = Offset(w, h)
        ),
        size = size,
        cornerRadius = CornerRadius(outerRadius, outerRadius),
        style = Stroke(width = 2.5.dp.toPx())
    )
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
