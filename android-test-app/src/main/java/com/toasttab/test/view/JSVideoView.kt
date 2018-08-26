package com.toasttab.test.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.AttributeSet
import android.widget.ImageView

import com.parrot.arsdk.arcontroller.ARFrame

class JSVideoView : ImageView {

    private val mHandler: Handler

    private var mBmp: Bitmap? = null

    constructor(context: Context) : super(context) {
        // needed because setImageBitmap should be called on the main thread
        mHandler = Handler(context.mainLooper)
        customInit()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        // needed because setImageBitmap should be called on the main thread
        mHandler = Handler(context.mainLooper)
        customInit()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // needed because setImageBitmap should be called on the main thread
        mHandler = Handler(context.mainLooper)
        customInit()
    }

    private fun customInit() {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    fun displayFrame(frame: ARFrame) {
        val data = frame.byteData
        synchronized(this) {
            mBmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        mHandler.post(object : Runnable {
            override fun run() {
                synchronized(this) {
                    setImageBitmap(mBmp)
                }
            }
        })
    }

    companion object {
        private const val TAG = "JSVideoView"
    }
}
