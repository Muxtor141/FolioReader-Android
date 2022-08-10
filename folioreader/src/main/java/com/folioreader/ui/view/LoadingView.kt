package com.folioreader.ui.view

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.folioreader.Config
import com.folioreader.R
import com.folioreader.ui.view.LoadingView
import com.folioreader.util.AppUtil.Companion.getSavedConfig

class LoadingView : ConstraintLayout {
    var maxVisibleDuration = -1
    private var mHandler: Handler? = null
    private val hideRunnable = Runnable { hide() }

    var callback: ((isLoading: Boolean) -> Unit)? = null

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        LayoutInflater.from(context).inflate(R.layout.view_loading, this)
        if (isInEditMode) return
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingView,
            0, 0
        )
        maxVisibleDuration = typedArray.getInt(R.styleable.LoadingView_maxVisibleDuration, -1)
        mHandler = Handler()
        isClickable = true
        isFocusable = true
        updateTheme()
        if (visibility == VISIBLE) show()
    }

    fun updateTheme() {
        var config = getSavedConfig(context)
        if (config == null) config = Config()
        if (config.isNightMode) {
            setBackgroundColor(ContextCompat.getColor(context, R.color.night_background_color))
        } else {
            setBackgroundColor(ContextCompat.getColor(context, R.color.day_background_color))
        }
    }

    @JavascriptInterface
    fun show(onHideCallback: (() -> Unit)? = null) {
        //Log.d(LOG_TAG, "-> show");
        mHandler!!.removeCallbacks(hideRunnable)
        mHandler!!.post {
            visibility = VISIBLE
            callback?.invoke(true)
        }
        if (maxVisibleDuration > -1) mHandler!!.postDelayed(
            hideRunnable,
            maxVisibleDuration.toLong()
        )
    }

    @JavascriptInterface
    fun hide() {
        //Log.d(LOG_TAG, "-> hide");
        mHandler!!.removeCallbacks(hideRunnable)
        mHandler!!.post {
            visibility = INVISIBLE
            callback?.invoke(false)
        }
    }

    @JavascriptInterface
    fun visible() {
        //Log.d(LOG_TAG, "-> visible");
        mHandler!!.post { visibility = VISIBLE }
    }

    @JavascriptInterface
    fun invisible() {
        //Log.d(LOG_TAG, "-> invisible");
        mHandler!!.post { visibility = INVISIBLE }
    }

    companion object {
        private val LOG_TAG = LoadingView::class.java.simpleName
    }
}