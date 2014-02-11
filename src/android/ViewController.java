package com.feedhenry.phonegap.webview;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

@SuppressWarnings("deprecation")
public class ViewController extends CordovaPlugin {

  final String closed = "closed";
  final String openned = "openned";
  String status = closed;
  boolean visible = false;
  int success = 0;
  int failure = 1;
  private WebViewContainer webviewContainer;
  private Dialog webviewDialog;
  
  private static final String TAG = "ViewController";

  public ViewController() {
    
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
        webviewContainer = new WebViewContainer((Context)this.cordova, data, this);
        final WebViewContainer c = webviewContainer;
        
        this.cordova.getActivity().runOnUiThread(new Runnable(){
          @Override
          public void run() {
            webviewDialog = new WebviewDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
            webviewDialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Activity;
            webviewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            webviewDialog.setCancelable(true);
            webviewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        close();
                    }
            });
            
            c.onCreate();
            
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(webviewDialog.getWindow().getAttributes());
            lp.width = width == 0? WindowManager.LayoutParams.MATCH_PARENT : (width);
            lp.height = height == 0? WindowManager.LayoutParams.MATCH_PARENT: (height);
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.x = x;
            lp.y = y;
            if(width != 0 || height != 0 || x != 0 || y != 0){
              c.setEmbedMode(true);
            }
            webviewDialog.getWindow().setAttributes(lp);
            webviewDialog.setContentView(c.getView());
            webviewDialog.show();
            
            /*webviewDialog.setOnKeyListener(new Dialog.OnKeyListener(){

              @Override
              public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK){
                  close();
                  return true;
                }
                return false;
              }
              
            });*/
          }
        });
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
     if(null != webviewContainer && status.equals(openned)){
       final WebViewContainer c = webviewContainer;
       this.cordova.getActivity().runOnUiThread(new Runnable(){
          @Override
          public void run() {
            if(null != webviewDialog){
              c.getWebView().handleDestroy();
              webviewDialog.dismiss();
            }
          }
       });
       status = closed;
     }
  }
   
   public boolean onBackKeyPressed(int keyCode, KeyEvent keyEvent){
     return this.webView.onKeyUp(keyCode, keyEvent);
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
  public void onReset() {
      close();        
  }
  
  @Override
  public void onDestroy() {
      close();        
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
  
  public void sendUpdate(String pUrl){
    Log.d("WebView", "received event, data : " + pUrl);
    String js = "javascript:(function(){var e = document.createEvent('Events');e.initEvent('webviewUrlChange');e.data = '" + pUrl + "';document.dispatchEvent(e);})();";
    webView.loadUrl(js);
  }
  
  class WebviewDialog extends Dialog {

    public WebviewDialog(Context context, int theme) {
      super(context, theme);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      if(!webviewContainer.getIsEmbedMode()){
        menu.add("Close");
        super.onCreateOptionsMenu(menu);
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
      if(!webviewContainer.getIsEmbedMode()){
        if (item.getTitle().equals("Close")) {
          close();
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
    
  }

}
