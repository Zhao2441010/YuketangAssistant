package site.jiclub.gongxiaohu.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

/**
 * App 内 WebView 承载官方课堂页。
 *
 * <p>系统浏览器不共享 App 的 CookieManager，所以必须在本 Activity 里加载
 * 官方固定入口，注入的 sessionid/csrftoken 才会随请求发送，从而以在读学生身份
 * 直接进入课堂会话（不经过动态码签到）。
 */
public class ClassroomWebViewActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "classroom_url";
    public static final String EXTRA_DOMAIN = "classroom_domain";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);          // 官方课堂页是 SPA，必须开 JS
        s.setDomStorageEnabled(true);          // window.Authorization / localStorage
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // 官方页按桌面 UA 渲染更稳（fullscreen/v3 是 PC 端页面）
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36");

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        // 页内跳转留在同一 WebView；主文档加载失败时给出可见提示，而不是停在空白页
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadDataWithBaseURL(null, "<html><body style='padding:32px;font-size:15px;"
                            + "color:#8E8E93;background:#F2F2F7'>" + Html.escapeHtml(error.getDescription())
                            + "<br><br>请检查网络后按返回键重试</body></html>", "text/html", "utf-8", null);
                }
            }
        });

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null && !url.isEmpty()) webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
