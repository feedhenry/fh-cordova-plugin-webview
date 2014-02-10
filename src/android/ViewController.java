package com.feedhenry.phonegap.webview;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AbsoluteLayout;
import android.widget.RelativeLayout;

@SuppressWarnings("deprecation")
public class ViewController extends CordovaPlugin {

  final String closed = "closed";
  final String openned = "openned";
  String status = closed;
  boolean visible = false;
  int success = 0;
  int failure = 1;
  public static final int START_WEBVIEW_REQUEST = 4;
  Intent intent;
  private WebViewContainer webviewContainer;
  private WebViewUpdateReceiver mReceiver;
  
  public static final String BROADCAST_ACTION_FILTER = "com.feedhenry.android.webview.urlChanged";
  
  private static final String TAG = "ViewController";

  public ViewController() {
    
  }

  public void closeView() {
    status = closed;
    this.cordova.getActivity().unregisterReceiver(mReceiver);
    jsCallBack(success, "closed");
  }

  public boolean isSynch(String action) {
    return false;
  }

  public void display(JSONArray args) {
    try {

      if (status.equals(closed)) {
        status = openned;
        jsCallBack(success, openned);
        Bundle data = new Bundle();
        data.putString("url", args.getString(0));
        data.putString("title", args.getString(1));
        data.putInt("x", args.getInt(2));
        data.putInt("y", args.getInt(3));
        data.putInt("width", args.getInt(4));
        data.putInt("height", args.getInt(5));
        data.putBoolean("showTitleBar", args.getBoolean(6));
        data.putString("titleBarColor", args.getString(7));
        data.putBoolean("showControls", args.getBoolean(8));
        data.putBoolean("setupBridge", args.getBoolean(9));
        final int x = args.getInt(2);
        final int y = args.getInt(3);
        final int width = args.getInt(4);
        final int height = args.getInt(5); 
        if(x!=0 || y!=0 || width !=0 || height !=0){
          webviewContainer = new WebViewContainer((Context)this.cordova, data, this);
          final WebViewContainer c = webviewContainer;
          
          this.cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
              c.onCreate();
              RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(width == 0 ? ViewGroup.LayoutParams.FILL_PARENT : (width+x), height == 0? ViewGroup.LayoutParams.FILL_PARENT : (height+y));
              c.getView().setTag("webviewcontainer");
              c.getView().setPadding(x, y, 0, 0);
              c.getView().setBackgroundColor(Color.TRANSPARENT);
              ViewGroup root = (ViewGroup) webView.getParent().getParent();
              root.addView(c.getView(), rl);
              root.requestLayout();
            }
          });
        } else {
          if (intent == null) {
            intent = new Intent( (Activity)this.cordova, WebViewIntent.class);
          }
          
          if(null == mReceiver){
            mReceiver = new WebViewUpdateReceiver();
          }
          
          IntentFilter filter = new IntentFilter(BROADCAST_ACTION_FILTER);
                this.cordova.getActivity().registerReceiver(mReceiver, filter);
                Log.d(TAG, "receiver registered");
          intent.putExtra("settings", data);
          this.cordova.startActivityForResult(this, intent, START_WEBVIEW_REQUEST);
        }
        //Thread.sleep(500);
      }
      else
      {
      jsCallBack(failure,"already openned");
      }

    } catch (Exception e) {
      e.printStackTrace();
      jsCallBack(failure, e.toString());
    }
  }
  
   public void close(){
     Log.d("ViewController", "close intent");
     if(status.equals(openned) ){
       if(null != intent){
         this.cordova.getActivity().finishActivity(START_WEBVIEW_REQUEST);
       } else if(null != webviewContainer){
           jsCallBack(success, "closed");
         final WebViewContainer c = webviewContainer;
         this.cordova.getActivity().runOnUiThread(new Runnable(){
        @Override
        public void run() {
         ViewGroup root = (ViewGroup) webView.getParent().getParent();
         root.removeView(c.getView());
        }
           
         });
       }
       status = closed;
   }
  }

  public void jsCallBack(final int type, final String message) {
    switch (type) {
      
      case 0:
        this.cordova.getActivity().runOnUiThread(new Runnable() {
      
      @Override
      public void run() {
        webView.loadUrl("javascript: navigator.webview.success('" + message
                    + "')");
      }
      });
          break;
      case 1:
        this.cordova.getActivity().runOnUiThread(new Runnable() {
      
      @Override
      public void run() {
        webView.loadUrl("javascript: navigator.webview.failure('" + message
                    + "')");
      }
      });
          break;
      }
    
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext pCallbackContext) {
    
    if (action.equals("display")) {
      this.display(args);
      pCallbackContext.success();
      return true;
    } else if(action.equals("close")){
      Log.d("ViewController", "In close");
      this.close();
      pCallbackContext.success();
      return true;
    } else {
      pCallbackContext.error("Unknown action : " + action);
      return false;
    }
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Log.d("onActivityResult", requestCode +":"+resultCode);

    if (requestCode == START_WEBVIEW_REQUEST) {
      this.closeView();
    }
  }

    private class WebViewUpdateReceiver extends BroadcastReceiver {

      @Override
      public void onReceive(Context pContext, Intent pIntent) {
        Log.d("WebView", "received event, data : " + pIntent.getStringExtra("url"));
        String js = "javascript:(function(){var e = document.createEvent('Events');e.initEvent('webviewUrlChange');e.data = '" + pIntent.getStringExtra("url") + "';document.dispatchEvent(e);})();";
        webView.loadUrl(js);
      }
      
    }

}
