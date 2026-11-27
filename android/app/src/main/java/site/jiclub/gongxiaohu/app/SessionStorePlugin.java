package site.jiclub.gongxiaohu.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * 本机登录凭据存储：用 SharedPreferences 代替 WebView 的 localStorage。
 *
 * <p>Android WebView 在部分机型 / 内嵌环境下 localStorage 会抛 SecurityError 或静默不落盘，
 * 导致「登录成功但重开 App 又要扫码」。SharedPreferences 位于应用私有目录，
 * 卸载 App 才会清除，满足「不主动退出就一直保持登录」。
 *
 * <p>只存前端加密后的账号快照字符串（键 gxh_auth_v2），原生层不解析内容。
 */
@CapacitorPlugin(name = "SessionStore")
public class SessionStorePlugin extends Plugin {

    private static final String PREFS_NAME = "gxh_auth_store";

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @PluginMethod
    public void get(PluginCall call) {
        String key = call.getString("key");
        if (key == null || key.isEmpty()) {
            call.reject("缺少 key");
            return;
        }
        String value = prefs().getString(key, null);
        com.getcapacitor.JSObject ret = new com.getcapacitor.JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    @PluginMethod
    public void set(PluginCall call) {
        String key = call.getString("key");
        String value = call.getString("value");
        if (key == null || key.isEmpty()) {
            call.reject("缺少 key");
            return;
        }
        boolean ok = prefs().edit().putString(key, value == null ? "" : value).commit();
        if (!ok) {
            call.reject("写入本机存储失败");
            return;
        }
        call.resolve();
    }

    @PluginMethod
    public void remove(PluginCall call) {
        String key = call.getString("key");
        if (key == null || key.isEmpty()) {
            call.reject("缺少 key");
            return;
        }
        prefs().edit().remove(key).commit();
        call.resolve();
    }
}
