package com.summertaker.jnews2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.android.synthetic.main.activity_articles.*
import org.json.JSONObject

class ArticlesActivity : SwipeActivity(), ArticlesInterface {

    private var mArticles: ArrayList<Article> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_articles)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initGesture()

        swipeRefresh.setOnRefreshListener {
            mArticles.clear()
            getRemoteData()
        }

        //mAdapter = ArticlesAdapter(this, mArticles)
        recyclerView.adapter = ArticlesAdapter(this, mArticles)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        getRemoteData()
    }

    override fun onSupportNavigateUp(): Boolean {
        //onBackPressed()
        finish()
        return true
    }

    private fun isInternetConnected(context: Context): Boolean {
        var result = false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                result = true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                result = true
            }
        }
        return result
    }

    private fun getRemoteData() {
        if (isInternetConnected(this)) {
            val stringRequest = StringRequest(
                Request.Method.GET, Config.remoteDataUrl,
                Response.Listener { response ->
                    //Log.e(logTag, ">> Response.Listener: $response")
                    val dataManager = DataManager(this)
                    val success = dataManager.saveFile(response)
                    if (success) {
                        mArticles.clear()
                        val videos = dataManager.getVideoFiles()
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("articles")
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val file = obj.getString("file")
                            if (file.isNullOrEmpty()) {
                                continue
                            }
                            //Log.e(logTag, file) // upload/I6PxDCRxJEQ.mp4

                            val fileName = file.substring(file.lastIndexOf('/') + 1)
                            var displayName: String? = null
                            for (video in videos) {
                                if (fileName == video.displayName) {
                                    displayName = video.displayName
                                    break
                                }
                            }

                            val article = Article(
                                obj.getString("id"),
                                obj.getString("yid"),
                                obj.getString("title"),
                                file,
                                displayName
                            )
                            mArticles.add(article)
                        }
                        recyclerView.adapter?.notifyDataSetChanged()
                        swipeRefresh.isRefreshing = false
                    }
                },
                Response.ErrorListener { error ->
                    Log.e(">>", error.toString())
                    Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_SHORT).show()
                })
            VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
        } else {
            Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onArticleSelected(article: Article) {
        //Toast.makeText(it.context, article.title, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DownloadActivity::class.java)
        intent.putExtra("id", article.id)
        intent.putExtra("title", article.title)
        intent.putExtra("file", article.file)
        startActivityForResult(intent, Config.activityRequestCodeDownload)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Config.activityRequestCodeDownload) {
            if (resultCode == Activity.RESULT_OK) {
                val id = data?.getStringExtra("id")
                val displayName = data?.getStringExtra("displayName")
                //Toast.makeText(this, "displayName: $displayName", Toast.LENGTH_SHORT).show()

                if (displayName != null) {
                    for (article in mArticles) {
                        if (article.id == id) {
                            article.displayName = displayName
                            break
                        }
                    }
                    setResult(Activity.RESULT_OK)
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onSwipeRight() {
        finish()
    }

    override fun onSwipeLeft() {
        finish()
    }
}