package com.leandro.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppEntry> = emptyList()
    private var favorites: Set<String> = emptySet()

    fun submit(list: List<AppEntry>, favs: Set<String>) {
        items = list
        favorites = favs
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.card)
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val star: TextView = view.findViewById(R.id.star)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val density = parent.resources.displayMetrics.density
        val holder = VH(view)

        // Efeito de foco no estilo Apple TV: o cartão cresce, "levanta" com sombra
        // e o nome do app fica mais forte.
        view.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.12f else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(160).start()
            holder.card.animate()
                .translationZ(if (hasFocus) 28f * density else 0f)
                .setDuration(160)
                .start()
            holder.label.animate().alpha(if (hasFocus) 1f else 0.7f).setDuration(160).start()
        }
        return holder
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.label.text = entry.label

        // Cada ImageView precisa da sua própria cópia do Drawable
        val drawable = entry.image.constantState?.newDrawable(holder.itemView.resources)
            ?: entry.image
        holder.icon.setImageDrawable(drawable)

        if (entry.isBanner) {
            holder.icon.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.icon.setPadding(0, 0, 0, 0)
        } else {
            val pad = (holder.itemView.resources.displayMetrics.density * 20).toInt()
            holder.icon.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.icon.setPadding(pad, pad, pad, pad)
        }

        holder.star.visibility = if (entry.packageName in favorites) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(entry) }
        holder.itemView.setOnLongClickListener {
            onLongClick(entry)
            true
        }
    }
}
