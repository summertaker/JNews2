package com.summertaker.jnews2

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_download.*

class DownloadActivity : AppCompatActivity() {

    private var mArticleId: String? = null
    private var mArticleTitle: String? = null
    private var mArticleFile: String? = null
    private var mDisplayName: String? = null

    private var downloadId: Long = -1L
    private lateinit var downloadManager: DownloadManager

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
                if (downloadId == id) {
                    val query: DownloadManager.Query = DownloadManager.Query()
                    query.setFilterById(id)
                    val cursor = downloadManager.query(query)
                    if (!cursor.moveToFirst()) {
                        return
                    }

                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(columnIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        //Toast.makeText(context, "Download succeeded", Toast.LENGTH_SHORT).show()
                        doFinish()
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        Toast.makeText(
                            context,
                            getString(R.string.download_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED == intent.action) {
                Toast.makeText(context, "Notification clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intent = intent
        mArticleId = intent.getStringExtra("id")
        mArticleTitle = intent.getStringExtra("title")
        mArticleFile = intent.getStringExtra("file")
        //Toast.makeText(this, mFile, Toast.LENGTH_SHORT).show()

        tvDownloadTitle.text = mArticleTitle

        if (mArticleFile != null) {
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val intentFilter = IntentFilter()
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
            registerReceiver(onDownloadComplete, intentFilter)

            downloadBtn.setOnClickListener {
                doDownload()
            }

            statusBtn.setOnClickListener {
                val status = getStatus(downloadId)
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            }

            cancelBtn.setOnClickListener {
                if (downloadId != -1L) {
                    downloadManager.remove(downloadId)
                }
            }

            doDownload()
        } else {
            Toast.makeText(this, "mArticleFile is null.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //onBackPressed()
        doFinish()
        return true
    }

    /*
    https://codechacha.com/ko/android-downloadmanager/
     */
    private fun doDownload() {
        mDisplayName = mArticleFile?.substring(mArticleFile!!.lastIndexOf('/') + 1)
        val destinationName = Config.localDownloadSubPath + "/" + mDisplayName

        val downloadUrl = Config.remoteBaseUrl + mArticleFile
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(mArticleTitle)
            .setDescription(mDisplayName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            //.setDestinationUri(Uri.fromFile(mStorageFile))
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, destinationName)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        downloadId = downloadManager.enqueue(request)
    }

    private fun getStatus(id: Long): String {
        val query: DownloadManager.Query = DownloadManager.Query()
        query.setFilterById(id)
        val cursor = downloadManager.query(query)
        if (!cursor.moveToFirst()) {
            Log.e(">>", "Empty row")
            return "Wrong downloadId"
        }

        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(columnIndex)
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)
        val statusText: String

        statusText = when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> "Successful"
            DownloadManager.STATUS_FAILED -> {
                "Failed: $reason"
            }
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Running"
            DownloadManager.STATUS_PAUSED -> {
                "Paused: $reason"
            }
            else -> "Unknown"
        }

        return statusText
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    private fun doFinish() {
        //Toast.makeText(this, "onDestroy().mStorageFile: $mStorageFile", Toast.LENGTH_SHORT).show()
        /*
         * onDestory()에 아래 코드를 넣으면 putExtra() 값과 Activity.RESULT_OK 값을 이전 Activity에서 가져오지 못 한다.
         */
        val intent = Intent(this, DownloadActivity::class.java)
        intent.putExtra("id", mArticleId)
        intent.putExtra("displayName", mDisplayName)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
