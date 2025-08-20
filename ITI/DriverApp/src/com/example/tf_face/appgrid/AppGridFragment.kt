package com.example.tf_face.appgrid

import android.content.pm.PackageManager
import android.os.Bundle
import com.example.tf_face.R
import com.example.tf_face.appgrid.model.AppInfo
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppGridFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appListAdapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_grid, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_apps)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        appListAdapter = AppListAdapter(requireContext(), getInstalledApps())
        recyclerView.adapter = appListAdapter
        return view
    }

    private fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val pm = requireContext().packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in allApps) {
            // Skip the app itself
            if (appInfo.packageName == "android.vendor.driverapp") {
                continue
            }
            val label = appInfo.loadLabel(pm)
            val icon = appInfo.loadIcon(pm)
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)

            if (launchIntent != null) {
                apps.add(AppInfo(label, icon, launchIntent))
            }
        }

        return apps.sortedBy { it.label.toString().lowercase() }
    }
}