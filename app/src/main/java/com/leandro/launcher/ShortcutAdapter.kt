package com.leandro.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShortcutAdapter : RecyclerView.Adapter<ShortcutAdapter.VH>() {

    private var items: List<ShortcutEntry> = emptyList()

    fun submit(list: List<ShortcutEntry>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val chip: TextView) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shortcut, parent, false) as TextView
        chip.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(140).start()
        }
        return VH(chip)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.chip.text = entry.label
        holder.chip.setOnClickListener { entry.action() }
    }
}
