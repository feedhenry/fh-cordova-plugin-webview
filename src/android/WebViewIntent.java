package com.feedhenry.phonegap.webview;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.DroidGap;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class WebViewIntent extends DroidGap implements CordovaInterface {

  private WebViewContainer webviewContainer;
    protected CordovaPlugin activityResultCallback = null;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    webviewContainer = new WebViewContainer(this, getIntent()
        .getBundleExtra("settings"), this);
    webviewContainer.onCreate();
    this.appView = webviewContainer.appView;
    setContentView(webviewContainer.getView());
  }

  public void onDestroy() {
    Log.d("webview ", "destory");
    super.onDestroy();
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    return webviewContainer.onOptionsItemSelected(item);
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    return webviewContainer.onCreateOptionsMenu(menu);
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return webviewContainer.onKeyDown(keyCode, event);
  }

 /* @Override
  public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setActivityResultCallback(CordovaPlugin plugin) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Activity getActivity() {
    return this;
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public void cancelLoadUrl() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object onMessage(String id, Object data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExecutorService getThreadPool() {
    // TODO Auto-generated method stub
    return null;
  }*/
}
