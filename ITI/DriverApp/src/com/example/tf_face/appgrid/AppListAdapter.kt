package com.example.tf_face.appgrid

import android.content.Context
import com.example.tf_face.R

import com.example.tf_face.appgrid.model.AppInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val context: Context,
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.app_icon)
        val labelView: TextView = itemView.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.iconView.setImageDrawable(app.icon)
        holder.labelView.text = app.label
        holder.itemView.setOnClickListener {
            context.startActivity(app.launchIntent)
        }
    }

    override fun getItemCount(): Int = apps.size
}
