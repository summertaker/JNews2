package com.summertaker.jnews2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.list_item_article.view.*

class ArticlesAdapter(
    private val listener: ArticlesInterface,
    private val article: ArrayList<Article>
) :
    RecyclerView.Adapter<ArticlesAdapter.ViewHolder>() {

    override fun getItemCount() = article.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = article[position]
        holder.apply {
            //bind(myListener, article)
            bind(article, listener)
            itemView.tag = article
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_article, parent, false)
        return ViewHolder(inflatedView)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var view: View = v

        fun bind(
            article: Article,
            listener: ArticlesInterface
        ) {
            if (view.thumbnail != null) {
                val yid = article.yid
                val url = "https://i.ytimg.com/vi/$yid/maxresdefault.jpg"
                Glide.with(view.context).load(url).into(view.thumbnail)
                //view.thumbnail.setImageBitmap(article.thumbnail)
            }

            view.title.text = article.title

            if (article.displayName == null) {
                view.ivTick.visibility = View.GONE
                view.btDownload.visibility = View.VISIBLE
                view.btDownload.setOnClickListener {
                    listener.onArticleSelected(article)
                }
            } else {
                view.ivTick.visibility = View.VISIBLE
                view.btDownload.visibility = View.GONE
            }
        }
    }
}
