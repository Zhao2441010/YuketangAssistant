package site.jiclub.gongxiaohu.app;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        // 注册原生插件：免扫码进课堂会话（官方域 Cookie 注入 + App 内 WebView）
        registerPlugin(ClassroomEntryPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
