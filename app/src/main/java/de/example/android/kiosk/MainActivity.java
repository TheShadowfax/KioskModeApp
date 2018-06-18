package de.example.android.kiosk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {

    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP , KeyEvent.KEYCODE_HOME ));

    private int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
      static boolean lockTask = false;
    private  static boolean isOnline = false;
    private  static  boolean isSystemUiShown;
    private static WebView webView=null;
        private static String url = "http://booking.stanplus.co.in";
        private Context context=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_main);
        setWebView();
        // every time someone enters the kiosk mode, set the flag true
        PrefUtils.setKioskModeActive(true, getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setPinnedMode();
        else Toast.makeText(getApplicationContext(),"Pinned Mode cannot be used @API<LOLLIPOP",Toast.LENGTH_LONG).show();
        findViewById(R.id.rootLayout).getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);


    }


    void setWebView(){
        webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        context=this;
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                Toast.makeText(getApplicationContext(),url,Toast.LENGTH_LONG).show();
                if(url.contains(MainActivity.url)){
                    return  false;
                }else {
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {

                super.onPageFinished(view, url);

            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
//                Toast.makeText(getApplicationContext(),errorCode+""+description+failingUrl,Toast.LENGTH_LONG).show();
                webView.loadUrl("file:///android_asset/maintenance.html");

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.getAllowContentAccess();
        webView.loadUrl(url);


    }



    void breakOut() {
         // Break out!
         PrefUtils.setKioskModeActive(false, getApplicationContext());
         Toast.makeText(getApplicationContext(),"You can leave the app now!", Toast.LENGTH_SHORT).show();
         Log.d("Kiosk","Exit");
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lockTask)
             stopLockTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setPinnedMode() {
        // get policy manager
        DevicePolicyManager myDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        // get this app package name
        ComponentName mDPM = new ComponentName(this, Admin.class);
        Log.d("Kiosk", this.getPackageName()+"  de.example.android.kiosk");
        if (myDevicePolicyManager.isDeviceOwnerApp(this.getPackageName())) {
            // get this app package name
            String[] packages = {this.getPackageName()};
            // mDPM is the admin package, and allow the specified packages to lock task

            myDevicePolicyManager.setLockTaskPackages(mDPM, packages);

        } else {
//            Toast.makeText(getApplicationContext(),"Not owner", Toast.LENGTH_LONG).show();
        }
        if(myDevicePolicyManager.isLockTaskPermitted(this.getPackageName())){
            startLockTask();
            lockTask = true;
        }else {
//            Toast.makeText(getApplicationContext(),"Not allowed", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if(!hasFocus) {
            // Close every kind of system dialog
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
//            preventStatusBarExpansion(this);
//
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                getWindow().getDecorView().setSystemUiVisibility(uiFlags);
            }
        }
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (blockedKeys.contains(event.getKeyCode())) {
            return true;
        } else {
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);
            return super.dispatchKeyEvent(event);
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // navigation bar height

            int navigationBarHeight = 0;
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            // status bar height
            int statusBarHeight = 0;
            resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            // display window size for the app layout
            Rect rect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            ViewGroup viewGroup = (ViewGroup)findViewById(R.id.rootLayout);
            // screen height - (user app height + status + nav) ..... if non-zero, then there is a soft keyboard
            int keyboardHeight = viewGroup.getRootView().getHeight() - (statusBarHeight + navigationBarHeight + rect.height());

            if (keyboardHeight <= 0) {
                onHideKeyboard();
            }
        }


        private void onHideKeyboard() {
//            Toast.makeText(getApplicationContext(),"Hello",Toast.LENGTH_SHORT).show();
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);

            isSystemUiShown=true;
        }
    };

    static void networkListener(final boolean isOnline){
        Log.d("Kiosk",""+isOnline);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
// UI code goes here
                if(isOnline){
                    if(!webView.getUrl().contains(MainActivity.url)){
                        webView.loadUrl(url);
//                        webView.addJavascriptInterface(new WebAppInterface(context), "Android");

                    }
                }else {
                    webView.loadUrl("file:///android_asset/noConnection.html");

                }
        }});


    }

}
