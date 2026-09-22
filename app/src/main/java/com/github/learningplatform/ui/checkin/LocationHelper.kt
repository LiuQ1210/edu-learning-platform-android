package com.github.learningplatform.ui.checkin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

/**
 * 位置签到用的定位工具。
 *
 * 只使用系统 [LocationManager]，不引入 play-services-location，
 * 避免为一个功能点增加 Google 服务依赖。
 */
object LocationHelper {

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasPermission(context: Context): Boolean = requiredPermissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取当前定位。
     * API 30+ 走 getCurrentLocation 主动定位；更低版本退回最近一次已知位置。
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasPermission(context)) {
            onError("未授予定位权限")
            return
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            onError("设备不支持定位")
            return
        }

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            onError("请先开启定位服务（GPS 或网络定位）")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val executor = Executor { it.run() }
            manager.getCurrentLocation(provider, CancellationSignal(), executor) { location ->
                if (location != null) {
                    onSuccess(location)
                } else {
                    lastKnown(manager, context)?.let(onSuccess)
                        ?: onError("定位失败，请到空旷处重试")
                }
            }
        } else {
            val last = lastKnown(manager, context)
            if (last != null) onSuccess(last) else onError("定位失败，请稍后重试")
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager, context: Context): Location? {
        if (!hasPermission(context)) return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /** 计算两点间距离（米）。客户端仅做前置提示，最终判定以后端为准。 */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}