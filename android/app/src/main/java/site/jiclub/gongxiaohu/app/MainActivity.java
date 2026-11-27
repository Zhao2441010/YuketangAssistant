package site.jiclub.gongxiaohu.app;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        // 注册原生插件：免扫码进课堂会话（官方域 Cookie 注入 + App 内 WebView）
        registerPlugin(ClassroomEntryPlugin.class);
        // 注册原生插件：本机登录凭据持久化（SharedPreferences，替代不可靠的 localStorage）
        registerPlugin(SessionStorePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
