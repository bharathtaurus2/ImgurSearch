package com.example.imgursearch

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var searchButton: Button
    private lateinit var searchText: EditText
    private lateinit var switchSearch: Switch
    private lateinit var noImages: TextView
    private lateinit var p: Point
    private val photos = mutableListOf<Photo>()
    private lateinit var httpClient: OkHttpClient

    companion object {
        private val TAG = MainActivity::class.qualifiedName
    }

    private class Photo {
        var id: String = ""
        var title: String = ""
        var localDate = ""
        var numImages: Int = 0
        var points: Int = 0
        var score: Int = 0
        var topic_id: Int = 0
    }

    private class PhotoVH(itemView: View) : ViewHolder(itemView) {
        lateinit var photo: ImageView
        var title: TextView? = null
        var localDate: TextView? = null
        var numPhotos: TextView? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        searchButton = findViewById<Button>(R.id.button_search)
        searchText = findViewById<EditText>(R.id.editText_search)
        searchButton.setOnClickListener {
            editText_search.onEditorAction(EditorInfo.IME_ACTION_DONE)
        }
        searchText.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    search()
                }
                return false
            }

        })
        switchSearch = findViewById(R.id.switch_toggle_search)
        switchSearch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (photos.size > 0) {
                    renderToggleCheck(isChecked)
                }
            }
        })
        noImages = findViewById(R.id.textView_noImages)
    }

    private fun renderToggleCheck(isChecked: Boolean) {
        val renderPhotoList = mutableListOf<Photo>()
        renderPhotoList.clear()
        for (photo in photos) {
            if (isChecked) {
                if ((photo.score + photo.points + photo.topic_id) % 2 == 0) {
                    renderPhotoList.add(photo)
                }
            } else {
                renderPhotoList.add(photo)
            }
        }
        runOnUiThread({ render(renderPhotoList) })
    }

    private fun search() {
        p = Point()
        windowManager.defaultDisplay.getSize(p)
        val term = searchText.text.toString()
        photos.clear()
        fetchData(term)
    }

    private var cropPosterTransformation = object : Transformation {
        override fun key(): String {
            return "cropPosterTransformation" + p.x
        }

        override fun transform(source: Bitmap): Bitmap {
            val targetWidth = p.x.toDouble()
            val aspectRatio = source.height.toDouble() / source.width.toDouble()

            val targetHeight = targetWidth * aspectRatio
            val result =
                Bitmap.createScaledBitmap(source, targetWidth.toInt(), targetHeight.toInt(), false)
            if (result != source) {
                source.recycle()
            }
            return result
        }
    }

    private fun render(photos: List<Photo>) {

        val rv: RecyclerView = findViewById(R.id.rv_of_photos)
        if (photos.size == 0) {
            noImages.visibility = View.VISIBLE
            rv.visibility = View.INVISIBLE
        } else {
            rv.visibility = View.VISIBLE
            noImages.visibility = View.INVISIBLE
            rv.layoutManager = LinearLayoutManager(this)

            val adapter: RecyclerView.Adapter<PhotoVH> = object : RecyclerView.Adapter<PhotoVH>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
                    val vh = PhotoVH(layoutInflater.inflate(R.layout.item, null))
                    vh.photo = vh.itemView.findViewById(R.id.photo)
                    vh.title = vh.itemView.findViewById(R.id.title)
                    vh.localDate = vh.itemView.findViewById(R.id.textView_date)
                    vh.numPhotos = vh.itemView.findViewById(R.id.textView_numPhotos)
                    return vh
                }

                override fun onBindViewHolder(holder: PhotoVH, position: Int) {
                    val photo = photos[position]
                    Picasso.get().load(
                        Imgur.baseImageUrl +
                                photo.id + "h.jpg"
                    ).placeholder(R.drawable.placeholder_vec)
                        .transform(cropPosterTransformation)
                        .into(holder.photo)
                    holder.title!!.text = photo.title
                    holder.localDate!!.text = photo.localDate
                    if (photo.numImages > 1) {
                        val numpPhotoText = photo.numImages.toString() + " Pictures"
                        holder.numPhotos!!.text = numpPhotoText
                        holder.numPhotos!!.visibility = View.VISIBLE
                    } else {
                        holder.numPhotos!!.visibility = View.GONE
                    }
                }

                override fun getItemCount(): Int {
                    return photos.size
                }
            }
            rv.adapter = adapter
            rv.addItemDecoration(object : ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = 2
                }
            })
        }
    }

    private fun fetchData(term: String) {
        val searchUri = Imgur.baseSearchUrl + term
        httpClient = OkHttpClient.Builder().build()
        val request: Request = Request.Builder()
            .url(searchUri)
            .header("Authorization", Imgur.clientID)
            .header("User-Agent", "ImgurSearch")
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "An error has occurred " + e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body!!.string()
                try {
                    val data = JSONObject(responseData)
                    val items: JSONArray = data.getJSONArray("data")
                    if (items.length() == 0) {
                        renderToggleCheck(switchSearch.isChecked)
                        return
                    }

                    for (i in 0..items.length()) {
                        val item: JSONObject = items.getJSONObject(i)
                        val photo: Photo = Photo()
                        if (item.getBoolean("is_album")) {
                            photo.id = item.getString("cover")
                            photo.numImages = item.getInt("images_count")
                        } else {
                            photo.id = item.getString("id")
                        }
                        photo.title = item.getString("title")
                        val timestamp = item.getLong("datetime")
                        val calendar = Calendar.getInstance(Locale.ENGLISH)
                        calendar.timeInMillis = timestamp * 1000L
                        photo.localDate =
                            DateFormat.format("dd/MM/yyyy h:mm a", calendar).toString()
                        photo.score = item.getInt("score")
                        photo.topic_id = item.getInt("topic_id")
                        photo.points = item.getInt("points")
                        photos.add(photo)
                        renderToggleCheck(switchSearch.isChecked)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

        })
    }

    private class Imgur {
        companion object {
            val clientID = "Client-ID " + BuildConfig.CLIENT_ID
            const val baseImageUrl = "https://i.imgur.com/"
            const val baseSearchUrl = "https://api.imgur.com/3/gallery/search/week/?q="
        }
    }
}