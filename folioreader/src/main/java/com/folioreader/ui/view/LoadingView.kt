package com.folioreader.ui.view

import android.content.Context
import com.folioreader.util.AppUtil.Companion.getSavedConfig
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.ProgressBar
import android.view.LayoutInflater
import com.folioreader.R
import android.content.res.TypedArray
import android.os.Handler
import android.util.AttributeSet
import com.folioreader.util.AppUtil
import com.folioreader.util.UiUtil
import androidx.core.content.ContextCompat
import android.webkit.JavascriptInterface
import com.folioreader.Config
import com.folioreader.ui.view.LoadingView

class LoadingView : ConstraintLayout {
    private var progressBar: ProgressBar? = null
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
        progressBar = findViewById(R.id.progressBar)
        isClickable = true
        isFocusable = true
        updateTheme()
        if (visibility == VISIBLE) show()
    }

    fun updateTheme() {
        var config = getSavedConfig(context)
        if (config == null) config = Config()
        progressBar?.indeterminateDrawable?.let{
            UiUtil.setColorIntToDrawable(config.themeColor, it)
        }
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