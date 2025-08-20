package com.example.tf_face.notification

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tf_face.R

class NotificationsAdapter(
    private val notifications: MutableList<Notification>,
    private val onActionClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView? = try {
            itemView.findViewById<TextView>(R.id.notification_message).also { view ->
                Log.d("NotificationsAdapter", "Found messageTextView: $view")
            }
        } catch (e: Exception) {
            Log.e("NotificationsAdapter", "Failed to find notification_message", e)
            null
        }
        val actionButton: Button? = try {
            itemView.findViewById<Button>(R.id.notification_action_button).also { view ->
                Log.d("NotificationsAdapter", "Found actionButton: $view")
            }
        } catch (e: Exception) {
            Log.e("NotificationsAdapter", "Failed to find notification_action_button", e)
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            Log.d("NotificationsAdapter", "Inflated item_notification layout")
            return ViewHolder(view)
        } catch (e: Exception) {
            Log.e("NotificationsAdapter", "Failed to inflate item_notification layout", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= notifications.size) {
            Log.e("NotificationsAdapter", "Invalid position: $position, size: ${notifications.size}")
            return
        }
        val notification = notifications[position]
        if (holder.messageTextView == null || holder.actionButton == null) {
            Log.e("NotificationsAdapter", "View initialization failed for position $position: messageTextView=${holder.messageTextView}, actionButton=${holder.actionButton}")
            return
        }
        Log.d("NotificationsAdapter", "Binding notification at position $position: ${notification.message}")
        holder.messageTextView.text = notification.message
        holder.messageTextView.visibility = View.VISIBLE // Ensure visibility
        holder.actionButton.setOnClickListener {
            try {
                Log.d("NotificationsAdapter", "Action button clicked for ${notification.type}")
                onActionClick(notification)
            } catch (e: Exception) {
                Log.e("NotificationsAdapter", "Error executing revert action for ${notification.type}", e)
            }
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        Log.d("NotificationsAdapter", "Updating notifications: ${newNotifications.size}")
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}