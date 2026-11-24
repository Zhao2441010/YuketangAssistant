package site.jiclub.gongxiaohu.app;

import android.content.Intent;
import android.webkit.CookieManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * 免扫码进课堂会话：给官方域注入 L1 Cookie，并在 App 内 WebView 打开在读学生固定入口。
 *
 * <p>依据博客 §1 与官方 pc.js openFullscreen()：正规在读学生学号已在选课名单，
 * 首页点「正在上课」直接 window.open("/lesson/fullscreen/v3/{lessonId}?source=5")，
 * 全程无 checkin、无 sign、无 ts、无动态码，仅凭官方域 Cookie（sessionid）。
 *
 * <p>关键：CookieManager 注入的 Cookie 只对<b>本 App 的 WebView</b> 生效，
 * 系统浏览器（Intent.ACTION_VIEW / Chrome Custom Tab）不共享，因此必须用
 * {@link ClassroomWebViewActivity} 承载官方课堂页。
 */
@CapacitorPlugin(name = "ClassroomEntry")
public class ClassroomEntryPlugin extends Plugin {

    @PluginMethod
    public void open(PluginCall call) {
        String url = call.getString("url");
        String cookie = call.getString("cookie");
        String domain = call.getString("domain", "https://changjiang.yuketang.cn");

        if (url == null || url.isEmpty()) {
            call.reject("缺少课堂入口 URL");
            return;
        }
        if (cookie == null || cookie.isEmpty()) {
            call.reject("缺少 L1 Cookie，请先登录");
            return;
        }

        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            // 逐条注入：cookie 串形如 "csrftoken=..; sessionid=..; university_id=..; uv_id=.."
            for (String part : cookie.split(";")) {
                String kv = part.trim();
                if (!kv.isEmpty()) cm.setCookie(domain, kv);
            }
            cm.flush();

            Intent intent = new Intent(getContext(), ClassroomWebViewActivity.class);
            intent.putExtra(ClassroomWebViewActivity.EXTRA_URL, url);
            intent.putExtra(ClassroomWebViewActivity.EXTRA_DOMAIN, domain);
            getActivity().startActivity(intent);

            JSObject ret = new JSObject();
            ret.put("opened", true);
            ret.put("url", url);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("打开课堂失败：" + e.getMessage(), e);
        }
    }
}
