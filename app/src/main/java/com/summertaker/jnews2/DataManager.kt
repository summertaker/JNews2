package com.summertaker.jnews2

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter

class DataManager(val context: Context) {

    private val logTag = ">>" //Config.logPrefix + this.javaClass.simpleName

    fun getVideoFiles(): ArrayList<Video> {
        val videos: ArrayList<Video> = ArrayList()

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Video.VideoColumns.DISPLAY_NAME
        )

        if (cursor == null) {
            Log.e(logTag, "cursor is null.")
        } else {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val relativePathColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)

            val resolver = context.contentResolver
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val relativePath = cursor.getString(relativePathColumn)

                val ok = relativePath.contains(
                    Config.localDownloadSubPath,
                    ignoreCase = false
                ) // "日本語" 디렉토리
                if (ok) {
                    val contentUri =
                        Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                    resolver.openFileDescriptor(contentUri, "r")
                    val thumbnail = resolver.loadThumbnail(contentUri, Size(160, 90), null)

                    //Log.e(logTag, "getVideoFiles() - video.displayName: $displayName") // We2N0HrYmeY.mp4

                    val video = Video(id, displayName, contentUri, thumbnail)
                    videos.add(video)
                }
            }
            cursor.close()
        }

        val jsonPath = File(context.getExternalFilesDir(null), Config.localDownloadSubPath)
        val jsonFile = File(jsonPath, Config.localDataFileName)
        if (jsonFile.exists()) {
            val jsonString = jsonFile.readText()
            //Log.e(logTag, ">> readText(): $jsonString")

            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray("articles")
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val file = obj.getString("file")
                if (file.isNullOrEmpty()) {
                    continue
                }
                //Log.e(logTag, file) // upload/I6PxDCRxJEQ.mp4

                val fileName = file.substring(file.lastIndexOf('/') + 1)
                //Log.e(logTag, ">> fileName: $fileName") // I6PxDCRxJEQ.mp4

                for (video in videos) {
                    //Log.e(logTag, ">> " + video.displayName)
                    if (fileName == video.displayName) {
                        video.japanese = obj.getString("japanese")
                        video.furigana = obj.getString("furigana")
                        video.korean = obj.getString("korean")
                        if (obj.has("style")) {
                            video.style = obj.getString("style")
                        }
                        //Log.e(logTag, ">> " + video.korean)
                        break
                    }
                }
            }
        }

        return videos
    }

    //fun getLocalData(response: String?) {
        //val dir = File(getExternalFilesDir(null), getString(R.string.japanese))
        //if (dir.isDirectory) {
        //    val fs = dir.listFiles()
        //    fs?.forEach {
        //        Log.d(logTag, ">> ${it.name}")
        //        val video = Video(it.toURI().toString(), it.path, it.name)
        //        mVideos.add(video)
        //    }
        //} else {
        //    Log.e(logTag, getString(R.string.no_directory) + ": " + dir)
        //}
    //}

    fun saveFile(response: String): Boolean {
        var success = true
        val dir = File(context.getExternalFilesDir(null), Config.localDownloadSubPath)
        if (!dir.isDirectory) {
            success = dir.mkdir()
        }
        if (success) {
            val dest = File(dir, Config.localDataFileName)
            if (dest.exists()) {
                //Toast.makeText(context, "기존 파일 존재: " + Config.localDataFileName, Toast.LENGTH_SHORT).show()
                if (!dest.delete()) {
                    Toast.makeText(context, "기존 파일 삭제 실패", Toast.LENGTH_SHORT).show()
                    success = false
                }
            }
            if (success) {
                try {
                    // response is the data written to file
                    PrintWriter(dest).use { out -> out.println(response) }
                    //Log.e(logTag, ">> response: $response")
                    //Toast.makeText(context, "파일 저장 완료", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    success = false
                    Toast.makeText(context, "파일 저장 실패", Toast.LENGTH_SHORT).show()
                    Log.e(logTag, e.message.toString())
                }
            }
        } else {
            success = false
            Toast.makeText(context, "디렉토리 만들기 실패", Toast.LENGTH_SHORT).show()
        }
        return success
    }
}