package de.example.android.kiosk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {

    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP , KeyEvent.KEYCODE_HOME ));

    private int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
      static boolean lockTask = false;
    private  static boolean loaded = false;
    private  Context mainActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_main);
        mainActivity =getApplicationContext();



//        getSupportActionBar().hide();

        // every time someone enters the kiosk mode, set the flag true
        PrefUtils.setKioskModeActive(true, getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setPinnedMode();
        else Toast.makeText(getApplicationContext(),"Pinned Mode cannot be used @API<LOLLIPOP",Toast.LENGTH_LONG).show();
        String url = "http://192.168.3.173:8080";
        WebView webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                Toast.makeText(getApplicationContext(),url,Toast.LENGTH_LONG).show();
                if(url.contains("http://192.168.3.173:8080")){
                    return  false;
                }else {
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {

                super.onPageFinished(view, url);
                loaded= true;
                Toast.makeText(getApplicationContext(),"page loaded+="+view.getTitle(),Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Toast.makeText(getApplicationContext(),errorCode+""+description+failingUrl,Toast.LENGTH_LONG).show();
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
            Toast.makeText(getApplicationContext(),"Not owner", Toast.LENGTH_LONG).show();
        }
        if(myDevicePolicyManager.isLockTaskPermitted(this.getPackageName())){
            startLockTask();
            lockTask = true;
        }else {
            Toast.makeText(getApplicationContext(),"Not allowed", Toast.LENGTH_LONG).show();
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



//    public static void preventStatusBarExpansion(Context context) {
//        WindowManager manager = ((WindowManager) context.getApplicationContext()
//                .getSystemService(Context.WINDOW_SERVICE));
//
//        Activity activity = (Activity)context;
//        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
//        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
//        localLayoutParams.gravity = Gravity.TOP;
//        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
//
//                // this is to enable the notification to recieve touch events
//                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
//
//                // Draws over status bar
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
//
//        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
//        int resId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
//        int result = 0;
//        if (resId > 0) {
//            result = activity.getResources().getDimensionPixelSize(resId);
//        }
//
//        localLayoutParams.height = result;
//
//        localLayoutParams.format = PixelFormat.TRANSPARENT;
//
//        customViewGroup view = new customViewGroup(context);
//
//        manager.addView(view, localLayoutParams);
//    }
//
//    private static class customViewGroup extends ViewGroup {
//
//        public customViewGroup(Context context) {
//            super(context);
//        }
//
//        @Override
//        protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        }
//
//        @Override
//        public boolean onInterceptTouchEvent(MotionEvent ev) {
//            Log.v("customViewGroup", "**********Intercepted");
//            return true;
//        }
//    }


}
