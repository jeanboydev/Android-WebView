package com.jeanboy.app.webview.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private FrameLayout view_container;
    private WebView webView;

    private boolean mIsLandscape = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        setContentView(R.layout.activity_main);

        //注册 Settings.System.ACCELEROMETER_ROTATION
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true, rotationObserver);

        view_container = (FrameLayout) findViewById(R.id.view_container);
        view_container.setKeepScreenOn(true);

        initWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            try {
                webView.getClass().getMethod("onResume").invoke(webView,(Object[])null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            try {
                webView.getClass().getMethod("onPause").invoke(webView, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        getContentResolver().unregisterContentObserver(rotationObserver);
        try {
            // 处理浏览网页时防止内存泄漏
            setConfigCallback(null);
            if (webView != null) {
                webView.setVisibility(View.GONE);
                webView.removeAllViews();
                webView.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            //设置全屏即隐藏状态栏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mIsLandscape = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //恢复状态栏
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            mIsLandscape = false;
        }
    }


    private void initWebView() {
        try {
            webView = new WebView(getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // chromium, enable hardware acceleration
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                // older android version, disable hardware acceleration
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);//启用js
            settings.setJavaScriptCanOpenWindowsAutomatically(true);//js和android交互
            settings.setDomStorageEnabled(true);// 启用localStorage 和 sessionStorage
            settings.setAppCacheEnabled(true); // 开启应用程序缓存
            String appCacheDir = this.getApplicationContext()
                    .getDir("cache", Context.MODE_PRIVATE).getPath();
            settings.setAppCachePath(appCacheDir);//设置缓存的指定路径
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAppCacheMaxSize(1024 * 1024 * 10);
            settings.setAllowFileAccess(true);// 允许访问文件
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            // 启用Webdatabase数据库
            settings.setDatabaseEnabled(true);
            String databaseDir = this.getApplicationContext()
                    .getDir("database", Context.MODE_PRIVATE).getPath();
            settings.setDatabasePath(databaseDir);// 设置数据库路径
            settings.setGeolocationEnabled(false);// 地理定位
            settings.setGeolocationDatabasePath(databaseDir);
            // 开启插件（对flash的支持）
//            settings.setPluginState(WebSettings.PluginState.ON);
//            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            settings.setBuiltInZoomControls(false);//关闭zoom
            settings.setSupportZoom(false);//关闭zoom按钮
            //设置，可能的话使所有列的宽度不超过屏幕宽度
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
            settings.setUseWideViewPort(true); //设置webview自适应屏幕大小
//            settings.setLoadWithOverviewMode(true);//在加载页面时显示出整个网页内容

            webView.setWebChromeClient(webChromeClient);
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                }
            });
            webView.loadUrl("https://link.theplatform.com/s/JKPqfC/media/_QssTpyyBdPl?balance=true");
            view_container.addView(webView, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConfigCallback(WindowManager windowManager) {
        try {
            Field field = WebView.class.getDeclaredField("mWebViewCore");
            field = field.getType().getDeclaredField("mBrowserFrame");
            field = field.getType().getDeclaredField("sConfigCallback");
            field.setAccessible(true);
            Object configCallback = field.get(null);

            if (null == configCallback) {
                return;
            }

            field = field.getType().getDeclaredField("mWindowManager");
            field.setAccessible(true);
            field.set(configCallback, windowManager);
        } catch (Exception e) {

        }
    }

    private WebChromeClient webChromeClient = new WebChromeClient() {

        private View myView = null;
        private CustomViewCallback myCallback = null;

        @Override
        public View getVideoLoadingProgressView() {
            FrameLayout frameLayout = new FrameLayout(MainActivity.this);
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return frameLayout;
        }

        // 配置权限 （在WebChromeClint中实现）
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
            super.onGeolocationPermissionsShowPrompt(origin, callback);
        }

        // 扩充数据库的容量（在WebChromeClint中实现）
        @Override
        public void onExceededDatabaseQuota(String url,
                                            String databaseIdentifier, long currentQuota,
                                            long estimatedSize, long totalUsedQuota,
                                            WebStorage.QuotaUpdater quotaUpdater) {

            quotaUpdater.updateQuota(estimatedSize * 2);
        }

        // 扩充缓存的容量
        @Override
        public void onReachedMaxAppCacheSize(long spaceNeeded,
                                             long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {

            quotaUpdater.updateQuota(spaceNeeded * 2);
        }

        // Android 使WebView支持HTML5 Video（全屏）播放的方法
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (myCallback != null) {
                myCallback.onCustomViewHidden();
                myCallback = null;
                return;
            }

            FrameLayout frameLayout = (FrameLayout) getWindow().getDecorView();
            frameLayout.addView(view);
            myView = view;
            myCallback = callback;
            webChromeClient = this;

            if (!mIsLandscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        @Override
        public void onHideCustomView() {
            if (myView != null) {
                if (myCallback != null) {
                    myCallback.onCustomViewHidden();
                    myCallback = null;
                }

                FrameLayout frameLayout = (FrameLayout) getWindow().getDecorView();
                frameLayout.removeView(myView);
                myView = null;
                if (mIsLandscape) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }

//        @Override
//        public boolean onJsAlert(WebView view, String url, String message,
//                                 final JsResult result) {
//            // 弹窗处理
//            AlertDialog.Builder b2 = new AlertDialog.Builder(MainActivity.this)
//                    .setTitle(R.string.app_name).setMessage(message)
//                    .setPositiveButton("ok", new AlertDialog.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            result.confirm();
//                        }
//                    });
//
//
//            b2.setCancelable(false);
//            b2.create();
//            b2.show();
//            return true;
//        }
    };

    private ContentObserver rotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (selfChange) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        }
    };
}
