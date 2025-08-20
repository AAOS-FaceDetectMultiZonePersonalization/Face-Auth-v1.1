package com.example.tf_face.appgrid.model

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: CharSequence,
    val icon: Drawable,
    val launchIntent: Intent
)
