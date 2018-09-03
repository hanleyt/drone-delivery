package com.toasttab.test.view

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM
import com.parrot.arsdk.arcontroller.ARControllerCodec
import com.parrot.arsdk.arcontroller.ARFrame

import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class H264VideoView : SurfaceView, SurfaceHolder.Callback {

    private var mMediaCodec: MediaCodec? = null
    private lateinit var mReadyLock: Lock

    private var mIsCodecConfigured = false

    private var mSpsBuffer: ByteBuffer? = null
    private var mPpsBuffer: ByteBuffer? = null

    private var mBuffers: Array<ByteBuffer>? = null

    constructor(context: Context) : super(context) {
        customInit()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        customInit()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        customInit()
    }

    private fun customInit() {
        mReadyLock = ReentrantLock()
        holder.addCallback(this)
    }

    fun displayFrame(frame: ARFrame) {
        mReadyLock.lock()

        if (mMediaCodec != null) {
            if (mIsCodecConfigured) {
                // Here we have either a good PFrame, or an IFrame
                var index = -1

                try {
                    index = mMediaCodec!!.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT.toLong())
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error while dequeue input buffer")
                }

                if (index >= 0) {
                    val b: ByteBuffer?
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        b = mMediaCodec!!.getInputBuffer(index)
                    } else {
                        b = mBuffers!![index]
                        b.clear()
                    }

                    b?.put(frame.byteData, 0, frame.dataSize)

                    try {
                        mMediaCodec!!.queueInputBuffer(index, 0, frame.dataSize, 0, 0)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error while queue input buffer")
                    }

                }
            }

            // Try to display previous frame
            val info = MediaCodec.BufferInfo()
            var outIndex: Int
            try {
                outIndex = mMediaCodec!!.dequeueOutputBuffer(info, 0)

                while (outIndex >= 0) {
                    mMediaCodec!!.releaseOutputBuffer(outIndex, true)
                    outIndex = mMediaCodec!!.dequeueOutputBuffer(info, 0)
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error while dequeue input buffer (outIndex)")
            }

        }


        mReadyLock.unlock()
    }

    fun configureDecoder(codec: ARControllerCodec) {
        mReadyLock.lock()

        if (codec.type == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264) {
            val codecH264 = codec.asH264

            mSpsBuffer = ByteBuffer.wrap(codecH264.sps.byteData)
            mPpsBuffer = ByteBuffer.wrap(codecH264.pps.byteData)
        }

        if (mMediaCodec != null && mSpsBuffer != null) {
            configureMediaCodec()
        }

        mReadyLock.unlock()
    }

    private fun configureMediaCodec() {
        mMediaCodec!!.stop()
        val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT)
        format.setByteBuffer("csd-0", mSpsBuffer)
        format.setByteBuffer("csd-1", mPpsBuffer)

        mMediaCodec!!.configure(format, holder.surface, null, 0)
        mMediaCodec!!.start()

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBuffers = mMediaCodec!!.inputBuffers
        }

        mIsCodecConfigured = true
    }

    private fun initMediaCodec(type: String) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(type)
        } catch (e: IOException) {
            Log.e(TAG, "Exception", e)
        }

        if (mMediaCodec != null && mSpsBuffer != null) {
            configureMediaCodec()
        }
    }

    private fun releaseMediaCodec() {
        if (mMediaCodec != null) {
            if (mIsCodecConfigured) {
                mMediaCodec!!.stop()
                mMediaCodec!!.release()
            }
            mIsCodecConfigured = false
            mMediaCodec = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mReadyLock.lock()
        initMediaCodec(VIDEO_MIME_TYPE)
        mReadyLock.unlock()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mReadyLock.lock()
        releaseMediaCodec()
        mReadyLock.unlock()
    }

    companion object {

        private val TAG = "H264VideoView"
        private val VIDEO_MIME_TYPE = "video/avc"
        private val VIDEO_DEQUEUE_TIMEOUT = 33000

        private val VIDEO_WIDTH = 640
        private val VIDEO_HEIGHT = 368
    }
}
