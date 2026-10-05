package com.mawaeedi.app

import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "MawaeediCustomization")
class CustomizationPlugin : Plugin() {
    @PluginMethod
    fun saveAsset(call: PluginCall) {
        val key = call.getString("key") ?: return call.reject("key is required")
        val value = call.getString("value") ?: return call.reject("value is required")
        getSharedPreferences().edit().putString(key, value).apply()
        call.resolve()
    }

    @PluginMethod
    fun getAsset(call: PluginCall) {
        val key = call.getString("key") ?: return call.reject("key is required")
        val result = JSObject().put("value", getSharedPreferences().getString(key, null))
        call.resolve(result)
    }

    @PluginMethod
    fun removeAsset(call: PluginCall) {
        val key = call.getString("key") ?: return call.reject("key is required")
        getSharedPreferences().edit().remove(key).apply()
        call.resolve()
    }

    private fun getSharedPreferences() = context.getSharedPreferences("mawaeedi_customization", Context.MODE_PRIVATE)
}
