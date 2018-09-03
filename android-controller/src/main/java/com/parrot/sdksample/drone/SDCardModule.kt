package com.parrot.sdksample.drone

import android.os.Environment
import android.util.Log
import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_ERROR_ENUM
import com.parrot.arsdk.ardatatransfer.ARDataTransferException
import com.parrot.arsdk.ardatatransfer.ARDataTransferManager
import com.parrot.arsdk.ardatatransfer.ARDataTransferMedia
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloader
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloaderCompletionListener
import com.parrot.arsdk.ardatatransfer.ARDataTransferMediasDownloaderProgressListener
import com.parrot.arsdk.arutils.ARUtilsManager
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class SDCardModule(mFtpList: ARUtilsManager, mFtpQueue: ARUtilsManager) {

    private val mListeners: MutableList<Listener>

    private var mDataTransferManager: ARDataTransferManager? = null

    private var mThreadIsRunning: Boolean = false
    private var mIsCancelled: Boolean = false

    private var mNbMediasToDownload: Int = 0
    private var mCurrentDownloadIndex: Int = 0

    private val mediaList: ArrayList<ARDataTransferMedia>?
        get() {
            var mediaList: ArrayList<ARDataTransferMedia>? = null

            var mediasDownloader: ARDataTransferMediasDownloader? = null
            if (mDataTransferManager != null) {
                mediasDownloader = mDataTransferManager!!.arDataTransferMediasDownloader
            }

            if (mediasDownloader != null) {
                try {
                    val mediaListCount = mediasDownloader.getAvailableMediasSync(false)
                    mediaList = ArrayList(mediaListCount)
                    var i = 0
                    while (i < mediaListCount && !mIsCancelled) {
                        val currentMedia = mediasDownloader.getAvailableMediaAtIndex(i)
                        mediaList.add(currentMedia)
                        i++
                    }
                } catch (e: ARDataTransferException) {
                    Log.e(TAG, "Exception", e)
                    mediaList = null
                }

            }
            return mediaList
        }
    //endregion notify listener block

    private val mDLProgressListener = object : ARDataTransferMediasDownloaderProgressListener {
        private var mLastProgressSent = -1
        override fun didMediaProgress(arg: Any, media: ARDataTransferMedia, percent: Float) {
            val progressInt = Math.floor(percent.toDouble()).toInt()
            if (mLastProgressSent != progressInt) {
                mLastProgressSent = progressInt
                notifyDownloadProgressed(media.name, progressInt)
            }
        }
    }

    private val mDLCompletionListener = ARDataTransferMediasDownloaderCompletionListener { _, media, _ ->
        notifyDownloadComplete(media.name)

        // when all download are finished, stop the download runnable
        // in order to get out of the downloadMedias function
        mCurrentDownloadIndex++
        if (mCurrentDownloadIndex > mNbMediasToDownload) {
            var mediasDownloader: ARDataTransferMediasDownloader? = null
            if (mDataTransferManager != null) {
                mediasDownloader = mDataTransferManager!!.arDataTransferMediasDownloader
            }

            mediasDownloader?.cancelQueueThread()
        }
    }

    interface Listener {
        /**
         * Called before medias will be downloaded
         * Called on a separate thread
         * @param nbMedias the number of medias that will be downloaded
         */
        fun onMatchingMediasFound(nbMedias: Int)

        /**
         * Called each time the progress of a download changes
         * Called on a separate thread
         * @param mediaName the name of the media
         * @param progress the progress of its download (from 0 to 100)
         */
        fun onDownloadProgressed(mediaName: String, progress: Int)

        /**
         * Called when a media download has ended
         * Called on a separate thread
         * @param mediaName the name of the media
         */
        fun onDownloadComplete(mediaName: String)
    }

    init {

        mThreadIsRunning = false
        mListeners = ArrayList()

        var result = ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK
        try {
            mDataTransferManager = ARDataTransferManager()
        } catch (e: ARDataTransferException) {
            Log.e(TAG, "Exception", e)
            result = ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_ERROR
        }

        if (result == ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
            // direct to external directory
            val externalDirectory = Environment.getExternalStorageDirectory().toString() + MOBILE_MEDIA_FOLDER

            // if the directory doesn't exist, create it
            val f = File(externalDirectory)
            if (!(f.exists() && f.isDirectory)) {
                val success = f.mkdir()
                if (!success) {
                    Log.e(TAG, "Failed to create the folder $externalDirectory")
                }
            }
            try {
                mDataTransferManager!!.arDataTransferMediasDownloader.createMediasDownloader(mFtpList, mFtpQueue, DRONE_MEDIA_FOLDER, externalDirectory)
            } catch (e: ARDataTransferException) {
                Log.e(TAG, "Exception", e)
                result = e.error
            }

        }

        if (result != ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
            // clean up here because an error happened
            mDataTransferManager!!.dispose()
            mDataTransferManager = null
        }
    }

    //region DroneDiscoveryListener functions
    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }
    //endregion DroneDiscoveryListener

    fun getFlightMedias(runId: String) {
        if (!mThreadIsRunning) {
            mThreadIsRunning = true
            Thread(Runnable {
                val mediaList = mediaList

                var mediasFromRun: ArrayList<ARDataTransferMedia>? = null
                mNbMediasToDownload = 0
                if (mediaList != null && !mIsCancelled) {
                    mediasFromRun = getRunIdMatchingMedias(mediaList, runId)
                    mNbMediasToDownload = mediasFromRun.size
                }

                notifyMatchingMediasFound(mNbMediasToDownload)

                if (mediasFromRun != null && mNbMediasToDownload != 0 && !mIsCancelled) {
                    downloadMedias(mediasFromRun)
                }

                mThreadIsRunning = false
                mIsCancelled = false
            }).start()
        }
    }

    fun getTodaysFlightMedias() {
        if (!mThreadIsRunning) {
            mThreadIsRunning = true
            Thread(Runnable {
                val mediaList = mediaList

                var mediasFromDate: ArrayList<ARDataTransferMedia>? = null
                mNbMediasToDownload = 0
                if (mediaList != null && !mIsCancelled) {
                    val today = GregorianCalendar()
                    mediasFromDate = getDateMatchingMedias(mediaList, today)
                    mNbMediasToDownload = mediasFromDate.size
                }

                notifyMatchingMediasFound(mNbMediasToDownload)

                if (mediasFromDate != null && mNbMediasToDownload != 0 && !mIsCancelled) {
                    downloadMedias(mediasFromDate)
                }

                mThreadIsRunning = false
                mIsCancelled = false
            }).start()
        }
    }

    fun cancelGetFlightMedias() {
        if (mThreadIsRunning) {
            mIsCancelled = true
            var mediasDownloader: ARDataTransferMediasDownloader? = null
            if (mDataTransferManager != null) {
                mediasDownloader = mDataTransferManager!!.arDataTransferMediasDownloader
            }

            mediasDownloader?.cancelQueueThread()
        }
    }

    private fun getRunIdMatchingMedias(
            mediaList: ArrayList<ARDataTransferMedia>,
            runId: String): ArrayList<ARDataTransferMedia> {
        val matchingMedias = ArrayList<ARDataTransferMedia>()
        for (media in mediaList) {
            if (media.name.contains(runId)) {
                matchingMedias.add(media)
            }

            // exit if the async task is cancelled
            if (mIsCancelled) {
                break
            }
        }

        return matchingMedias
    }

    private fun getDateMatchingMedias(mediaList: ArrayList<ARDataTransferMedia>,
                                      matchingCal: GregorianCalendar): ArrayList<ARDataTransferMedia> {
        val matchingMedias = ArrayList<ARDataTransferMedia>()
        val mediaCal = GregorianCalendar()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.getDefault())
        for (media in mediaList) {
            // convert date in string to calendar
            val dateStr = media.date
            try {
                val mediaDate = dateFormatter.parse(dateStr)
                mediaCal.time = mediaDate

                // if the date are the same day
                if (mediaCal.get(Calendar.DAY_OF_MONTH) == matchingCal.get(Calendar.DAY_OF_MONTH) &&
                        mediaCal.get(Calendar.MONTH) == matchingCal.get(Calendar.MONTH) &&
                        mediaCal.get(Calendar.YEAR) == matchingCal.get(Calendar.YEAR)) {
                    matchingMedias.add(media)
                }
            } catch (e: ParseException) {
                Log.e(TAG, "Exception", e)
            }

            // exit if the async task is cancelled
            if (mIsCancelled) {
                break
            }
        }

        return matchingMedias
    }

    private fun downloadMedias(matchingMedias: ArrayList<ARDataTransferMedia>) {
        mCurrentDownloadIndex = 1

        var mediasDownloader: ARDataTransferMediasDownloader? = null
        if (mDataTransferManager != null) {
            mediasDownloader = mDataTransferManager!!.arDataTransferMediasDownloader
        }

        if (mediasDownloader != null) {
            for (media in matchingMedias) {
                try {
                    mediasDownloader.addMediaToQueue(media, mDLProgressListener, null, mDLCompletionListener, null)
                } catch (e: ARDataTransferException) {
                    Log.e(TAG, "Exception", e)
                }

                // exit if the async task is cancelled
                if (mIsCancelled) {
                    break
                }
            }

            if (!mIsCancelled) {
                mediasDownloader.downloaderQueueRunnable.run()
            }
        }
    }

    //region notify listener block
    private fun notifyMatchingMediasFound(nbMedias: Int) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onMatchingMediasFound(nbMedias)
        }
    }

    private fun notifyDownloadProgressed(mediaName: String, progress: Int) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDownloadProgressed(mediaName, progress)
        }
    }

    private fun notifyDownloadComplete(mediaName: String) {
        val listenersCpy = ArrayList(mListeners)
        for (listener in listenersCpy) {
            listener.onDownloadComplete(mediaName)
        }
    }

    companion object {

        private val TAG = "SDCardModule"

        private val DRONE_MEDIA_FOLDER = "internal_000"
        private val MOBILE_MEDIA_FOLDER = "/ARSDKMedias/"
    }
}
