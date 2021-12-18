package com.lookaround.core.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class DisallowInterceptTouchEventFrameLayout : FrameLayout {
    var shouldRequestDisallowInterceptTouchEvent = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (shouldRequestDisallowInterceptTouchEvent) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE ->
                if (shouldRequestDisallowInterceptTouchEvent) {
                    requestDisallowInterceptTouchEvent(true)
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }
}
