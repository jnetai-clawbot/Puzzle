package com.jnetai.puzzle.utils

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ErrorLogger - persistent error tracking and debugging system.
 *
 * Generates a unique error code, logs stack traces and stores diagnostic
 * information for every failure/exception in the app. Codes stay permanently
 * integrated so future bugs can be traced quickly.
 *
 * Code format: E-<MODULE>-NNN
 */
object ErrorLogger {

    private const val TAG = "Puzzle-DEBUG"
    private val errorLog = mutableListOf<ErrorRecord>()
    private val lock = Any()

    data class ErrorRecord(
        val errorCode: String,
        val message: String,
        val exception: Throwable?,
        val timestamp: Long,
        val threadName: String,
        val stackTrace: String
    )

    /** Module error code constants. */
    object Codes {
        const val SYS_START = "E-SYS-001"
        const val SYS_UI_THREAD = "E-SYS-002"
        const val SYS_UNEXPECTED = "E-SYS-999"

        const val IMG_ASSET_LIST = "E-IMG-001"
        const val IMG_ASSET_LOAD = "E-IMG-002"
        const val IMG_URI_DECODE = "E-IMG-003"
        const val IMG_DECODE_MEMORY = "E-IMG-004"
        const val IMG_SAMPLE = "E-IMG-005"
        const val IMG_SAVE_CACHE = "E-IMG-006"

        const val PZL_NEW = "E-PZL-001"
        const val PZL_SCRAMBLE = "E-PZL-002"
        const val PZL_MOVE_BAD = "E-PZL-003"
        const val PZL_STATE = "E-PZL-004"
        const val PZL_GRID_INVALID = "E-PZL-005"
        const val PZL_IMAGE_NULL = "E-PZL-006"

        const val SET_LOAD_FAILED = "E-SET-001"
        const val SET_SAVE_FAILED = "E-SET-002"
        const val SET_INVALID_VALUE = "E-SET-003"

        const val UPD_CHECK_FAILED = "E-UPD-001"
        const val UPD_PARSE_FAILED = "E-UPD-002"
        const val UPD_NETWORK = "E-UPD-003"

        const val UI_VIEW_BINDING = "E-UI-001"
        const val UI_BOARD_DRAW = "E-UI-002"
        const val UI_LAYOUT = "E-UI-003"
    }

    /**
     * Record and log an error. Always safe to call from any thread.
     */
    fun log(errorCode: String, message: String, exception: Throwable? = null) {
        val stackInfo = if (exception != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            exception.printStackTrace(pw)
            sw.toString()
        } else {
            Throwable().stackTraceToString()
        }

        val record = ErrorRecord(
            errorCode = errorCode,
            message = message,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            threadName = Thread.currentThread().name,
            stackTrace = stackInfo
        )

        synchronized(lock) {
            errorLog.add(record)
            if (errorLog.size > 1000) {
                errorLog.removeAt(0)
            }
        }

        Log.e(TAG, "[$errorCode] $message")
        if (exception != null) {
            Log.e(TAG, "[$errorCode] Exception:", exception)
        }
        Log.e(TAG, "[$errorCode] Stack:\n$stackInfo")
    }

    /** Formatted-message overload. */
    fun logf(errorCode: String, format: String, vararg args: Any?) {
        log(errorCode, String.format(Locale.US, format, *args))
    }

    /** Formatted-message overload with an exception attached. */
    fun logf(errorCode: String, format: String, exception: Throwable?, vararg args: Any?) {
        log(errorCode, String.format(Locale.US, format, *args), exception)
    }

    fun getErrorLog(): List<ErrorRecord> = synchronized(lock) { errorLog.toList() }

    fun getRecentErrors(count: Int = 10): List<ErrorRecord> =
        synchronized(lock) { errorLog.takeLast(count).reversed() }

    fun clearLog() {
        synchronized(lock) { errorLog.clear() }
    }

    /** Format a human readable diagnostic report. */
    fun formatErrorReport(errorCode: String, message: String, exception: Throwable?): String {
        val sb = StringBuilder()
        sb.appendLine("════════════════════════════════════════")
        sb.appendLine("ERROR REPORT")
        sb.appendLine("════════════════════════════════════════")
        sb.appendLine("Error Code: $errorCode")
        sb.appendLine("Message:    $message")
        sb.appendLine("Timestamp:  ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
        sb.appendLine("Thread:     ${Thread.currentThread().name}")
        if (exception != null) {
            sb.appendLine("Exception:  ${exception.javaClass.name}: ${exception.message}")
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            exception.printStackTrace(pw)
            sb.appendLine("Stack Trace:\n${sw.toString().take(2000)}")
        }
        sb.appendLine("════════════════════════════════════════")
        return sb.toString()
    }

    /** Try/catch wrapper that logs and returns null when the block throws. */
    inline fun <T> tryOrNull(errorCode: String, message: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            log(errorCode, message, e)
            null
        }
    }
}