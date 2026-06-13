package com.novacraft.launcher.service.launch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileReader

/**
 * PerformanceOverlay
 *
 * Transparent HUD drawn over the game surface showing live metrics:
 * - FPS counter (frames per second)
 * - RAM usage (used / total MB)
 * - CPU usage %
 * - Network ping (ms) — placeholder, wired to server connection
 *
 * Updates on a coroutine at configurable interval (default 1s).
 */
class PerformanceOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Config ────────────────────────────────────────────────────────────────

    var showFps     = true
    var showRam     = true
    var showCpu     = false
    var showPing    = true
    var updateIntervalMs = 1000L
    var textSizeSp  = 12f
    var position    = OverlayPosition.TOP_LEFT

    // ── Live values ───────────────────────────────────────────────────────────

    private var fps     = 0
    private var ramUsedMb  = 0L
    private var ramTotalMb = 0L
    private var cpuPercent = 0f
    private var pingMs     = 0L

    // Frame counting
    private var frameCount   = 0
    private var lastFpsTime  = System.currentTimeMillis()

    // Coroutine
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    // Paint
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(2f, 1f, 1f, Color.argb(160, 0, 0, 0))
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isRunning) {
                updateMetrics()
                withContext(Dispatchers.Main) { invalidate() }
                delay(updateIntervalMs)
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    /** Called every rendered game frame to increment the FPS counter. */
    fun onFrame() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000L) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }
    }

    /** Update from network layer. */
    fun setPing(ms: Long) { pingMs = ms }

    // ── Rendering ────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lines = buildMetricLines()
        if (lines.isEmpty()) return

        val sp   = resources.displayMetrics.scaledDensity
        val ts   = textSizeSp * sp
        textPaint.textSize = ts

        val lineH = ts * 1.3f
        val padH  = ts * 0.4f
        val padV  = ts * 0.3f
        val maxW  = lines.maxOf { textPaint.measureText(it) }
        val bgH   = lines.size * lineH + padV * 2
        val bgW   = maxW + padH * 2

        val (bx, by) = when (position) {
            OverlayPosition.TOP_LEFT     -> Pair(16f, 16f)
            OverlayPosition.TOP_RIGHT    -> Pair(width - bgW - 16f, 16f)
            OverlayPosition.BOTTOM_LEFT  -> Pair(16f, height - bgH - 16f)
            OverlayPosition.BOTTOM_RIGHT -> Pair(width - bgW - 16f, height - bgH - 16f)
        }

        // Background
        canvas.drawRoundRect(bx, by, bx + bgW, by + bgH, 8f, 8f, bgPaint)

        // Text lines
        lines.forEachIndexed { i, line ->
            val color = when {
                line.startsWith("FPS") && fps < 20  -> Color.RED
                line.startsWith("FPS") && fps < 40  -> Color.YELLOW
                line.startsWith("FPS")              -> Color.argb(255, 100, 255, 120)
                line.startsWith("RAM") && ramUsedMb > ramTotalMb * 0.8 -> Color.YELLOW
                else -> Color.WHITE
            }
            textPaint.color = color
            canvas.drawText(
                line,
                bx + padH,
                by + padV + ts + i * lineH,
                textPaint
            )
        }
    }

    private fun buildMetricLines(): List<String> = buildList {
        if (showFps)  add("FPS: $fps")
        if (showRam)  add("RAM: ${ramUsedMb}/${ramTotalMb} MB")
        if (showCpu && cpuPercent > 0)  add("CPU: ${"%.1f".format(cpuPercent)}%")
        if (showPing && pingMs > 0) add("Ping: ${pingMs}ms")
    }

    // ── Metric collection ─────────────────────────────────────────────────────

    private fun updateMetrics() {
        updateRam()
        if (showCpu) updateCpu()
    }

    private fun updateRam() {
        val rt = Runtime.getRuntime()
        ramUsedMb  = (rt.totalMemory() - rt.freeMemory()) / 1_048_576
        ramTotalMb = rt.maxMemory() / 1_048_576
    }

    private fun updateCpu() {
        // Read /proc/stat for overall CPU time delta
        runCatching {
            val line = BufferedReader(FileReader("/proc/stat")).use { it.readLine() }
            val parts = line.split(" ").drop(2).mapNotNull { it.toLongOrNull() }
            if (parts.size >= 4) {
                val idle  = parts[3]
                val total = parts.sum()
                cpuPercent = (1f - idle.toFloat() / total) * 100f
            }
        }.onFailure { /* /proc/stat may not be readable on all devices */ }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}

enum class OverlayPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
