package com.studyfocus.app

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var selected: Boolean
)

class AppPickerActivity : AppCompatActivity() {

    private val allApps = ArrayList<AppItem>()
    private val shown = ArrayList<AppItem>()
    private lateinit var adapter: AppAdapter
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The list is locked while a focus session is running.
        if (FocusPrefs.isActive(this)) {
            Toast.makeText(this, "Locked during focus mode", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_app_picker)

        progress = findViewById(R.id.progress)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        adapter = AppAdapter(shown) { item, checked ->
            FocusPrefs.setBlocked(this, item.packageName, checked)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        etSearch.doAfterTextChanged { applyFilter(it?.toString().orEmpty()) }

        progress.visibility = View.VISIBLE
        Thread {
            val items = loadApps()
            runOnUiThread {
                allApps.clear()
                allApps.addAll(items)
                applyFilter(etSearch.text?.toString().orEmpty())
                progress.visibility = View.GONE
            }
        }.start()
    }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val blocked = FocusPrefs.getBlocked(this)
        val allowed = BlockRules.allowedPackages(this)

        return AppUtils.launchableApps(this)
            .filter { it.activityInfo.packageName !in allowed }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val pkg = it.activityInfo.packageName
                AppItem(pkg, it.loadLabel(pm).toString(), it.loadIcon(pm), pkg in blocked)
            }
            .sortedWith(
                compareByDescending<AppItem> { it.selected }
                    .thenBy { it.label.lowercase() }
            )
    }

    private fun applyFilter(query: String) {
        shown.clear()
        shown.addAll(allApps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }
}

class AppAdapter(
    private val items: List<AppItem>,
    private val onToggle: (AppItem, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val check: CheckBox = view.findViewById(R.id.check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.label.text = item.label
        holder.check.isChecked = item.selected
        holder.itemView.setOnClickListener {
            item.selected = !item.selected
            holder.check.isChecked = item.selected
            onToggle(item, item.selected)
        }
    }
}
