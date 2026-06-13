package com.novacraft.launcher.service.launch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

/**
 * TouchControlOverlay
 *
 * Custom View that renders on-screen touch controls over the Minecraft surface.
 *
 * Features:
 * - Configurable button positions and sizes (loaded from JSON layout)
 * - Multi-touch support for simultaneous button presses
 * - Gyroscope aiming integration
 * - Haptic feedback on press
 * - Drag-and-drop editor mode for customising layout
 * - Preset layouts: Default, Compact, Creative, Bedrock-style
 *
 * The overlay translates touch events into Minecraft keyboard/mouse equivalents
 * via the JNI bridge (in a real implementation).
 */
class TouchControlOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    // ── Button definitions ───────────────────────────────────────────────────

    data class ControlButton(
        val id: String,
        val label: String,
        val keyCode: Int,           // Android KeyEvent or custom code
        val defaultX: Float,        // 0..1 relative to screen width
        val defaultY: Float,        // 0..1 relative to screen height
        val width: Float = 0.08f,   // relative to screen width
        val height: Float = 0.08f,  // relative to screen height
        var x: Float = defaultX,
        var y: Float = defaultY,
        var visible: Boolean = true,
        var alpha: Float = 0.65f
    )

    // ── Default layout definition ────────────────────────────────────────────

    private val defaultButtons = listOf(
        // Movement (left side)
        ControlButton("move_forward",  "▲",  android.view.KeyEvent.KEYCODE_W,           0.12f, 0.55f),
        ControlButton("move_backward", "▼",  android.view.KeyEvent.KEYCODE_S,           0.12f, 0.75f),
        ControlButton("move_left",     "◄",  android.view.KeyEvent.KEYCODE_A,           0.06f, 0.65f),
        ControlButton("move_right",    "►",  android.view.KeyEvent.KEYCODE_D,           0.18f, 0.65f),
        ControlButton("jump",          "⬆",  android.view.KeyEvent.KEYCODE_SPACE,       0.12f, 0.40f),
        ControlButton("sneak",         "⬇",  android.view.KeyEvent.KEYCODE_SHIFT_LEFT,  0.06f, 0.80f),
        // Actions (right side)
        ControlButton("attack",        "⚔",  MOUSE_LEFT,                                0.82f, 0.60f, 0.10f, 0.10f),
        ControlButton("use",           "🖐",  MOUSE_RIGHT,                               0.92f, 0.45f, 0.08f, 0.08f),
        ControlButton("inventory",     "🎒",  android.view.KeyEvent.KEYCODE_E,           0.88f, 0.80f),
        ControlButton("chat",          "💬",  android.view.KeyEvent.KEYCODE_T,           0.78f, 0.80f),
        ControlButton("drop",          "🗑",  android.view.KeyEvent.KEYCODE_Q,           0.68f, 0.80f),
        ControlButton("sprint",        "💨",  TOGGLE_SPRINT,                             0.12f, 0.30f, 0.07f, 0.05f),
        ControlButton("fly_up",        "↑",  FLY_UP,                                    0.92f, 0.65f, 0.07f, 0.07f),
        ControlButton("fly_down",      "↓",  FLY_DOWN,                                  0.92f, 0.75f, 0.07f, 0.07f),
        // Hotbar selector
        ControlButton("slot_1",        "1",   android.view.KeyEvent.KEYCODE_1,           0.25f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_2",        "2",   android.view.KeyEvent.KEYCODE_2,           0.31f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_3",        "3",   android.view.KeyEvent.KEYCODE_3,           0.37f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_4",        "4",   android.view.KeyEvent.KEYCODE_4,           0.43f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_5",        "5",   android.view.KeyEvent.KEYCODE_5,           0.49f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_6",        "6",   android.view.KeyEvent.KEYCODE_6,           0.55f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_7",        "7",   android.view.KeyEvent.KEYCODE_7,           0.61f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_8",        "8",   android.view.KeyEvent.KEYCODE_8,           0.67f, 0.92f, 0.05f, 0.06f),
        ControlButton("slot_9",        "9",   android.view.KeyEvent.KEYCODE_9,           0.73f, 0.92f, 0.05f, 0.06f),
        // Settings
        ControlButton("pause",         "⏸",  android.view.KeyEvent.KEYCODE_ESCAPE,      0.95f, 0.06f, 0.05f, 0.06f)
    )

    companion object {
        const val MOUSE_LEFT   = -1
        const val MOUSE_RIGHT  = -2
        const val TOGGLE_SPRINT = -3
        const val FLY_UP       = -4
        const val FLY_DOWN     = -5
    }

    // ── State ────────────────────────────────────────────────────────────────

    private var buttons        = defaultButtons.map { it.copy() }.toMutableList()
    private val pressedButtons = mutableSetOf<String>()
    private val paint          = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isEditorMode   = false
    private var draggedButton: ControlButton? = null
    private var dragOffsetX    = 0f
    private var dragOffsetY    = 0f

    // Gyroscope
    private var sensorManager: SensorManager? = null
    private var gyroEnabled    = false
    private var lastGyroX      = 0f
    private var lastGyroY      = 0f

    // Haptic
    private val vibrator by lazy { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    private var hapticEnabled = true

    // Callbacks
    var onKeyDown: ((keyCode: Int) -> Unit)? = null
    var onKeyUp:   ((keyCode: Int) -> Unit)? = null
    var onMouseMove: ((dx: Float, dy: Float) -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun setEditorMode(enabled: Boolean) {
        isEditorMode = enabled
        invalidate()
    }

    fun setGyroAiming(enabled: Boolean) {
        gyroEnabled = enabled
        if (enabled) registerGyro() else unregisterGyro()
    }

    fun setHapticFeedback(enabled: Boolean) { hapticEnabled = enabled }

    fun setButtonAlpha(alpha: Float) {
        buttons.forEach { it.alpha = alpha }
        invalidate()
    }

    fun loadLayout(json: String) {
        runCatching {
            val type    = object : TypeToken<List<ControlButton>>() {}.type
            val loaded  = Gson().fromJson<List<ControlButton>>(json, type)
            buttons     = loaded.toMutableList()
            invalidate()
        }.onFailure { Timber.e(it, "Failed to load control layout") }
    }

    fun saveLayout(): String = Gson().toJson(buttons)

    fun resetLayout() {
        buttons = defaultButtons.map { it.copy() }.toMutableList()
        invalidate()
    }

    fun applyPreset(preset: ControlPreset) {
        when (preset) {
            ControlPreset.DEFAULT  -> resetLayout()
            ControlPreset.COMPACT  -> applyCompactPreset()
            ControlPreset.CREATIVE -> applyCreativePreset()
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        buttons.filter { it.visible }.forEach { btn ->
            drawButton(canvas, btn)
        }
        if (isEditorMode) drawEditorGrid(canvas)
    }

    private fun drawButton(canvas: Canvas, btn: ControlButton) {
        val sw   = width.toFloat()
        val sh   = height.toFloat()
        val bx   = btn.x * sw
        val by   = btn.y * sh
        val bw   = btn.width * sw
        val bh   = btn.height * sh
        val rect = RectF(bx, by, bx + bw, by + bh)
        val isPressed = btn.id in pressedButtons

        // Background
        paint.apply {
            style = Paint.Style.FILL
            color = if (isPressed)
                Color.argb((btn.alpha * 255 * 1.5f).toInt().coerceAtMost(255), 0, 229, 255)  // Cyan pressed
            else
                Color.argb((btn.alpha * 255 * 0.35f).toInt(), 100, 100, 180)
        }
        canvas.drawRoundRect(rect, bw * 0.25f, bh * 0.25f, paint)

        // Border
        paint.apply {
            style       = Paint.Style.STROKE
            strokeWidth = if (isEditorMode) 2f else 1.5f
            color = if (isEditorMode)
                Color.argb(200, 255, 200, 0)
            else if (isPressed)
                Color.argb(220, 0, 229, 255)
            else
                Color.argb(160, 124, 77, 255)
        }
        canvas.drawRoundRect(rect, bw * 0.25f, bh * 0.25f, paint)

        // Label
        paint.apply {
            style    = Paint.Style.FILL
            textSize = (bh * 0.42f).coerceIn(14f, 36f)
            textAlign = Paint.Align.CENTER
            color = Color.argb((btn.alpha * 255 * (if (isPressed) 1.5f else 1.0f)).toInt().coerceAtMost(255), 220, 220, 255)
        }
        canvas.drawText(btn.label, bx + bw / 2, by + bh / 2 + paint.textSize * 0.35f, paint)
    }

    private fun drawEditorGrid(canvas: Canvas) {
        paint.apply {
            style       = Paint.Style.STROKE
            strokeWidth = 0.5f
            color       = Color.argb(40, 0, 229, 255)
        }
        val step = width / 20f
        var x = 0f
        while (x <= width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += step }
        var y = 0f
        while (y <= height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += step }
    }

    // ── Touch handling ───────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val px = event.getX(pointerIndex)
        val py = event.getY(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (isEditorMode) {
                    handleEditorDown(px, py)
                } else {
                    handlePress(px, py)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isEditorMode && draggedButton != null) {
                    handleEditorDrag(event.x, event.y)
                }
                // In real game: translate movement for camera look
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (isEditorMode) {
                    draggedButton = null
                } else {
                    handleRelease(px, py)
                }
            }
        }
        return true
    }

    private fun handlePress(x: Float, y: Float) {
        findButtonAt(x, y)?.let { btn ->
            if (btn.id !in pressedButtons) {
                pressedButtons.add(btn.id)
                if (hapticEnabled) vibrate()
                onKeyDown?.invoke(btn.keyCode)
                invalidate()
            }
        }
    }

    private fun handleRelease(x: Float, y: Float) {
        // Release any button whose region contains the lifted finger
        val toRelease = pressedButtons.filter { id ->
            val btn = buttons.find { it.id == id } ?: return@filter true
            !rectContains(btn, x, y)
        }
        toRelease.forEach { id ->
            pressedButtons.remove(id)
            buttons.find { it.id == id }?.let { onKeyUp?.invoke(it.keyCode) }
        }
        invalidate()
    }

    private fun handleEditorDown(x: Float, y: Float) {
        draggedButton = findButtonAt(x, y)
        draggedButton?.let {
            dragOffsetX = x - it.x * width
            dragOffsetY = y - it.y * height
        }
    }

    private fun handleEditorDrag(x: Float, y: Float) {
        draggedButton?.let { btn ->
            btn.x = ((x - dragOffsetX) / width).coerceIn(0f, 1f - btn.width)
            btn.y = ((y - dragOffsetY) / height).coerceIn(0f, 1f - btn.height)
            invalidate()
        }
    }

    private fun findButtonAt(x: Float, y: Float): ControlButton? =
        buttons.filter { it.visible }.lastOrNull { rectContains(it, x, y) }

    private fun rectContains(btn: ControlButton, x: Float, y: Float): Boolean {
        val bx = btn.x * width;  val by = btn.y * height
        val bw = btn.width * width; val bh = btn.height * height
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh
    }

    // ── Gyroscope ────────────────────────────────────────────────────────────

    private fun registerGyro() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { gyro ->
            sensorManager?.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterGyro() { sensorManager?.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!gyroEnabled || event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return
        val sensitivity = 3.0f
        val dx = event.values[1] * sensitivity  // Yaw
        val dy = event.values[0] * sensitivity  // Pitch
        if (Math.abs(dx) > 0.02f || Math.abs(dy) > 0.02f) {
            onMouseMove?.invoke(dx, dy)
        }
        lastGyroX = event.values[0]; lastGyroY = event.values[1]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Haptic ───────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun vibrate() {
        runCatching { vibrator?.vibrate(15L) }
    }

    // ── Presets ──────────────────────────────────────────────────────────────

    private fun applyCompactPreset() {
        // Scale down all buttons and rearrange for small screens
        buttons.forEach { btn ->
            btn.width  = btn.width  * 0.8f
            btn.height = btn.height * 0.8f
        }
        invalidate()
    }

    private fun applyCreativePreset() {
        // Show fly buttons, hide sneak
        buttons.find { it.id == "fly_up"   }?.visible = true
        buttons.find { it.id == "fly_down" }?.visible = true
        buttons.find { it.id == "sneak"    }?.visible = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        unregisterGyro()
        super.onDetachedFromWindow()
    }
}

enum class ControlPreset { DEFAULT, COMPACT, CREATIVE }
