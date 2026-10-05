package com.mawaeedi.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "MawaeediAlarms")
public class AlarmPlugin extends Plugin {
    private static final String PREFS = "mawaeedi_alarms";
    private static final String KEY_ALARMS = "items";

    @PluginMethod
    public void schedule(PluginCall call) {
        String id = call.getString("id");
        Long at = call.getLong("triggerAtMillis");
        String title = call.getString("title", "منبه مواعيدي");
        if (id == null || at == null || at <= System.currentTimeMillis()) {
            call.reject("id و triggerAtMillis مطلوبان ويجب أن يكون الموعد في المستقبل");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && !canScheduleExactAlarms()) {
            JSObject result = new JSObject();
            result.put("requiresExactAlarmPermission", true);
            call.resolve(result);
            return;
        }
        AlarmScheduler.schedule(this.getContext(), at, id, title);
        save(id, at, title);
        call.resolve();
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        String id = call.getString("id");
        if (id == null) { call.reject("id مطلوب"); return; }
        AlarmScheduler.cancel(getContext(), id);
        remove(id);
        call.resolve();
    }

    @PluginMethod
    public void requestExactAlarmPermission(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 31 && !canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
            getContext().startActivity(intent);
        }
        call.resolve();
    }

    @PluginMethod
    public void list(PluginCall call) {
        call.resolve(new JSObject().put("alarms", readAll()));
    }

    private boolean canScheduleExactAlarms() {
        AlarmManager manager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private SharedPreferences prefs() { return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private JSONArray readAll() { return new JSONArray(prefs().getString(KEY_ALARMS, "[]")); }
    private void save(String id, long at, String title) {
        JSONArray next = new JSONArray();
        JSONArray current = readAll();
        for (int i = 0; i < current.length(); i++) try { if (!id.equals(current.getJSONObject(i).getString("id"))) next.put(current.getJSONObject(i)); } catch (Exception ignored) {}
        next.put(new JSONObject().put("id", id).put("triggerAtMillis", at).put("title", title));
        prefs().edit().putString(KEY_ALARMS, next.toString()).apply();
    }
    private void remove(String id) {
        JSONArray next = new JSONArray();
        JSONArray current = readAll();
        for (int i = 0; i < current.length(); i++) try { if (!id.equals(current.getJSONObject(i).getString("id"))) next.put(current.getJSONObject(i)); } catch (Exception ignored) {}
        prefs().edit().putString(KEY_ALARMS, next.toString()).apply();
    }

    static JSONArray stored(Context context) { return new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ALARMS, "[]")); }
}

final class AlarmScheduler {
    static void schedule(Context context, long at, String id, String title) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("alarm_id", id).putExtra("title", title);
        PendingIntent pending = pending(context, id, intent);
        if (Build.VERSION.SDK_INT >= 23) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
        else manager.setExact(AlarmManager.RTC_WAKEUP, at, pending);
    }
    static void cancel(Context context, String id) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pending(context, id, new Intent(context, AlarmReceiver.class)));
    }
    static void rescheduleSavedAlarms(Context context) {
        JSONArray alarms = AlarmPlugin.stored(context);
        for (int i = 0; i < alarms.length(); i++) try {
            JSONObject item = alarms.getJSONObject(i);
            long at = item.getLong("triggerAtMillis");
            if (at > System.currentTimeMillis()) schedule(context, at, item.getString("id"), item.optString("title", "منبه مواعيدي"));
        } catch (Exception ignored) {}
    }
    private static PendingIntent pending(Context context, String id, Intent intent) {
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
