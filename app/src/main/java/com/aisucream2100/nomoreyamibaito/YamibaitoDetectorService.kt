package com.aisucream2100.nomoreyamibaito

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button

class YamibaitoDetectorService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false

    // 検知対象のブラックリストキーワード
    private val targetKeywords = listOf("即日", "高額", "簡単", "裏バイト", "闇バイト", "シグナル", "テレグラム")

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isOverlayShowing) return

        // ルートノードを取得して画面全体を走査
        val rootNode = rootInActiveWindow ?: return
        if (scanNodeForKeywords(rootNode)) {
            showWarningOverlay()
        }
        rootNode.recycle()
    }

    // 再帰的にすべてのテキストノードをチェックする関数
    private fun scanNodeForKeywords(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // ノードのテキストを確認
        val text = node.text?.toString()
        if (!text.isNullOrEmpty()) {
            for (keyword in targetKeywords) {
                if (text.contains(keyword)) {
                    return true // キーワードが見つかった
                }
            }
        }

        // 子ノードをループ処理
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (scanNodeForKeywords(child)) {
                child?.recycle()
                return true
            }
            child?.recycle()
        }
        return false
    }

    // 警告用の全画面赤いオーバーレイを表示する
    private fun showWarningOverlay() {
        if (isOverlayShowing) return

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_warning, null)

        // 小さな閉じるボタンのイベントを設定
        overlayView?.findViewById<Button>(R.id.btnCloseOverlay)?.setOnClickListener {
            removeWarningOverlay()
        }

        windowManager?.addView(overlayView, params)
        isOverlayShowing = true
    }

    // オーバーレイを消去する
    private fun removeWarningOverlay() {
        if (isOverlayShowing && overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
            isOverlayShowing = false
        }
    }

    override fun onInterrupt() {
        // サービスが中断された時の処理
        removeWarningOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeWarningOverlay()
    }
}