package com.jnetai.puzzle.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.jnetai.puzzle.game.PuzzleEngine
import com.jnetai.puzzle.utils.ErrorLogger

/**
 * PuzzleBoardView - renders the sliding-puzzle grid and supports:
 *
 *   - Auto-fitting the source image inside the grid area maintaining the
 *     image aspect ratio (letterboxed with padding around the edges, so the
 *     image is never stretched).
 *   - Pinch-to-zoom: two finger pinch re-scales (resizes) the puzzle board and
 *     image together between MIN_ZOOM and MAX_ZOOM.
 *   - Tap to slide any tile adjacent to the empty space.
 */
class PuzzleBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var sourceBitmap: Bitmap? = null
    private var engine: PuzzleEngine? = null

    // Board geometry in view space.
    private var boardRect = RectF()
    private var tilePx = 0f

    // Zoom (scale factor) applied to the board size.
    private var zoom = 1.0f

    private val gestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
            invalidate()
            return true
        }
    })

    /** Optional callback fired when a tile slides successfully. */
    var onTileMoved: ((moves: Int) -> Unit)? = null
    /** Optional callback fired when the puzzle is solved. */
    var onPuzzleSolved: (() -> Unit)? = null

    init {
        gridPaint.color = 0xFF000000.toInt()
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 2f
        emptyPaint.color = 0xFF15151A.toInt()
        emptyPaint.style = Paint.Style.FILL
    }

    fun attach(bitmap: Bitmap, puzzleEngine: PuzzleEngine) {
        sourceBitmap = bitmap
        engine = puzzleEngine
        zoom = 1.0f
        requestLayout()
        invalidate()
        ErrorLogger.logf(ErrorLogger.Codes.UI_BOARD_DRAW,
            "PuzzleBoardView attached with %dx%d bitmap", bitmap.width, bitmap.height)
    }

    fun setZoom(value: Float) {
        zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        invalidate()
    }

    fun getZoom(): Float = zoom

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ensure the view is always square-ish: take the smaller of the two.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val side = minOf(w, h)
        val sideSpec = MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY)
        super.onMeasure(sideSpec, sideSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeGeometry()
    }

    private fun recomputeGeometry() {
        val eng = engine ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Base board size: fit inside the view with PADDING_FRACTION padding
        // around the edges, then apply the user pinch zoom.
        val base = minOf(w, h) * (1f - 2f * PADDING_FRACTION)
        val side = base * zoom
        val left = (w - side) / 2f
        val top = (h - side) / 2f
        boardRect = RectF(left, top, left + side, top + side)
        tilePx = side / eng.gridSize
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = sourceBitmap ?: return
        val eng = engine ?: return

        try {
            // Background frame.
            canvas.drawRect(boardRect, paint.apply { color = 0xFF3D3D46.toInt(); style = Paint.Style.FILL })

            val n = eng.gridSize
            val source = eng.getBoard()

            // Compute the letterboxed "image display rect" inside the board so
            // the image fills as much space as possible without stretching.
            val imageRect = computeImageRect(bmp, boardRect)

            for (i in source.indices) {
                val piece = source[i]
                val row = i / n
                val col = i % n
                val cell = RectF(
                    boardRect.left + col * tilePx,
                    boardRect.top + row * tilePx,
                    boardRect.left + (col + 1) * tilePx,
                    boardRect.top + (row + 1) * tilePx
                )

                if (piece == n * n - 1) {
                    // Empty tile.
                    canvas.drawRoundRect(cell, 4f, 4f, emptyPaint)
                    continue
                }

                // Map this tile back to the source image.
                val srcPieceRow = piece / n
                val srcPieceCol = piece % n
                val srcCol = imageRect.left + srcPieceCol * (imageRect.width() / n)
                val srcRow = imageRect.top + srcPieceRow * (imageRect.height() / n)
                val srcRect = android.graphics.Rect(
                    srcCol.toInt(),
                    srcRow.toInt(),
                    (srcCol + imageRect.width() / n).toInt(),
                    (srcRow + imageRect.height() / n).toInt()
                )

                // Intersect with the actual bitmap bounds to avoid sampling out of range.
                val isect = srcRect.setIntersect(
                    srcRect,
                    android.graphics.Rect(0, 0, bmp.width, bmp.height)
                )
                if (isect) {
                    canvas.save()
                    canvas.clipRect(cell)
                    canvas.drawBitmap(bmp, srcRect, cell, paint.apply { color = 0xFFFFFFFF.toInt() })
                    canvas.restore()
                } else {
                    canvas.save()
                    canvas.clipRect(cell)
                    canvas.drawColor(0xFF15151A.toInt())
                    canvas.restore()
                }

                // Tile border.
                canvas.drawRoundRect(cell, 4f, 4f, gridPaint)
            }

            // Outer board border.
            canvas.drawRect(boardRect, gridPaint)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.UI_BOARD_DRAW, "Failed to draw puzzle board", e)
        }
    }

    /** Letterbox the bitmap into the board rect preserving aspect ratio. */
    private fun computeImageRect(bmp: Bitmap, board: RectF): RectF {
        val srcAspect = bmp.width.toFloat() / bmp.height.toFloat()
        val boardAspect = board.width() / board.height()
        return if (srcAspect > boardAspect) {
            // Image is wider: fit width, pad top/bottom.
            val h = board.width() / srcAspect
            RectF(board.left, board.centerY() - h / 2f, board.right, board.centerY() + h / 2f)
        } else {
            // Image is taller: fit height, pad left/right.
            val w = board.height() * srcAspect
            RectF(board.centerX() - w / 2f, board.top, board.centerX() + w / 2f, board.bottom)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        val eng = engine ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val isTap = dx * dx + dy * dy <= TAP_SLOP_DP * TAP_SLOP_DP * 4 &&
                    System.currentTimeMillis() - downTime < 300

                // Only treat as a tap if the gesture scale didn't change much.
                if (isTap && !gestureDetector.isInProgress) {
                    handleTap(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    private fun handleTap(x: Float, y: Float) {
        val eng = engine ?: return
        if (!boardRect.contains(x, y)) return
        if (!eng.started) return

        val col = ((x - boardRect.left) / tilePx).toInt()
        val row = ((y - boardRect.top) / tilePx).toInt()
        val n = eng.gridSize
        if (col !in 0 until n || row !in 0 until n) return

        val tileIndex = row * n + col
        if (eng.moveTile(tileIndex)) {
            onTileMoved?.invoke(eng.moves)
            invalidate()
            if (eng.isSolved()) {
                onPuzzleSolved?.invoke()
            }
        }
    }

    companion object {
        private const val PADDING_FRACTION = 0.04f
        private const val MIN_ZOOM = 0.6f
        private const val MAX_ZOOM = 1.8f
        private const val TAP_SLOP_DP = 20f
    }
}