package com.feedhenry.phonegap.webview;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.cordova.CordovaChromeClient;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewClient;
import org.apache.cordova.R;
import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.LOG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WebViewContainer {

  OptionMenuManager optionMenu;
  CordovaWebView appView;
  String homeUrl;
  ImageButton back, forward, home, close;
  ProgressBar progressbar;
  int clickDown = 0;
  int clickUp = 1;
  
  private Bundle settings = null;
  private Context context = null;
  private ViewGroup mainLayout = null;
  private Object controller = null;
  
  public WebViewContainer(Context pContext, Bundle settings, Object controller) {
	  this.context = pContext;
	  this.settings = settings;
	  this.controller = controller;
  }
  
  public void onCreate() { 
    String url = settings.getString("url");
    String text = settings.getString("title");
    boolean showTitleBar = settings.getBoolean("showTitleBar");
    boolean setupBridge = settings.getBoolean("setupBridge");
    
    Log.d("webview", url + text);
    
    mainLayout = null;
    mainLayout = new LinearLayout(this.context);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.0F);
	mainLayout.setLayoutParams(lp);
	((LinearLayout) mainLayout).setGravity(Gravity.CENTER_VERTICAL);
	((LinearLayout) mainLayout).setOrientation(LinearLayout.VERTICAL);
   
    appView = new CordovaWebView(this.context);
    appView.resumeTimers();
    
    CordovaChromeClient chromeClient = new CordovaChromeClient((CordovaInterface) context, appView);
    appView.setWebChromeClient(chromeClient);
    
    final boolean bridgeSetup = setupBridge;
    CordovaWebViewClient webviewClient = new CordovaWebViewClient((CordovaInterface)context) {
      
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        LOG.d("Webview window", "start to load " + url);
        super.onPageStarted(view, url, favicon);
        Intent i = new Intent();
        i.setAction(ViewController.BROADCAST_ACTION_FILTER);
        i.putExtra("url", url);
        context.sendBroadcast(i);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        LOG.d("Webview window", "finish loading " + url);
        super.onPageFinished(view, url);
        if(back != null){
            if (appView.canGoBack())
                back.setImageResource(R.drawable.left_arrow_white);
              else
                back.setImageResource(R.drawable.left_arrow);
        }
        if(forward != null){
            if (appView.canGoForward())
                forward.setImageResource(R.drawable.right_arrow_white);
              else
                forward.setImageResource(R.drawable.right_arrow);
        }
        if(null != progressbar){
          progressbar.setVisibility(ProgressBar.INVISIBLE);
        }

      }
    };
    appView.setWebViewClient(webviewClient);
    
    chromeClient.setWebView(appView);
    webviewClient.setWebView(appView);
    
    appView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0F));
    
    appView.requestFocusFromTouch();
    appView.setVisibility(View.VISIBLE);
    if(showTitleBar){
    	LinearLayout barlayout = new LinearLayout(this.context);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.0F);
        barlayout.setLayoutParams(blp);
        barlayout.setGravity(Gravity.CENTER_VERTICAL);

        initHeardBar(barlayout, text);
        mainLayout.addView(barlayout);
    }
    mainLayout.setBackgroundColor(Color.TRANSPARENT);
    mainLayout.setBackgroundResource(0);
    mainLayout.addView(this.appView);
    
    if(setupBridge){
    	HttpURLConnection conn = null;
    	StringBuffer content = new StringBuffer("");
    	try{
    	  content = new StringBuffer("<script>");
    	  content.append("\n");
    	  content.append(readAssetScript("www/fhext/js/container.js"));
          content.append("\n");
    	  content.append(readAssetScript("www/cordova.js"));
    	  content.append("\n");
    	  content.append(readAssetScript("www/cordova_plugins.js"));
    	  content.append("\n");
    	  content.append("</script>");
          URL requestUrl = new URL(url);
    	  conn = (HttpURLConnection) requestUrl.openConnection();
    	  conn.setRequestProperty("User-Agent", appView.getSettings().getUserAgentString());
    	  byte[] res = readStream(conn.getInputStream());
    	  content.append(new String(res));
    	}catch(Exception e){
    	  e.printStackTrace();
    	  content = new StringBuffer("<p>Failed to load page " + url + "</p>");
    	} finally {
    	  if(null != conn){
    	    conn.disconnect();
    	  }
    	}
    	appView.loadDataWithBaseURL(url, content.toString(), "text/html", "UTF-8", null);
    } else {
      appView.loadUrl(url);
    }    
    this.homeUrl = url;
  }

  private void close() {
    appView.stopLoading();
    if(controller instanceof ViewController){
        appView.handleDestroy();
    	((ViewController)controller).close();
    }else if(controller instanceof Activity){
    	((Activity)controller).finish();
    }
  }

  public boolean onOptionsItemSelected(MenuItem item) {

    if (item.getTitle() == "Close") {
      close();
      return true;

    }
    return false;
  }


  public boolean onCreateOptionsMenu(Menu menu) {
    optionMenu = new OptionMenuManager();
    optionMenu.popViewCtrlMenu(menu);
    return true;
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    appView.onKeyDown(keyCode, event);
    return false;
  }

  private void initHeardBar(LinearLayout barlayout, String title) {

    LinearLayout rightlayout = new LinearLayout(this.context);
    rightlayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.0F));
    barlayout.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    barlayout.setBackgroundColor(Color.BLACK);
    String barColor = settings.getString("titleBarColor");
    if(!barColor.equalsIgnoreCase("default")){
    	barlayout.setBackgroundColor(Color.parseColor(barColor));
    }

    LinearLayout.LayoutParams btnlayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F);
    btnlayout.gravity = Gravity.CENTER_VERTICAL;

    close = new ImageButton(this.context);
    close.setImageResource(R.drawable.close);
    close.setBackgroundColor(Color.TRANSPARENT);
    close.setOnTouchListener(new OnTouchListener() {

      public boolean onTouch(View arg0, MotionEvent event) {

        if (event.getAction() == clickDown) {
          close.setImageResource(R.drawable.close_off);
          return true;
        }
        if (event.getAction() == clickUp) {
          close();
          close.setImageResource(R.drawable.close);
          return true;
        }

        return false;
      }
    });

    TextView text = new TextView(this.context);
    if (!title.equals("undefined"))
      text.setText(title);
    text.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
    text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F));
    
    boolean showControls = settings.getBoolean("showControls");
    if(showControls){
    	back = new ImageButton(this.context);
        back.setImageResource(R.drawable.left_arrow);
        back.setLayoutParams(btnlayout);
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnTouchListener(new OnTouchListener() {

          public boolean onTouch(View arg0, MotionEvent event) {
            if (event.getAction() == clickDown) {
              back.setImageResource(R.drawable.left_arrow);
              return true;
            }
            if (event.getAction() == clickUp) {
              if (appView.canGoBack())
                appView.goBack();

              if (appView.canGoBack())
                back.setImageResource(R.drawable.left_arrow_white);
              else
                back.setImageResource(R.drawable.left_arrow);
              return true;
            }

            return false;
          }
        });

        forward = new ImageButton(this.context);
        forward.setImageResource(R.drawable.right_arrow);
        forward.setBackgroundColor(Color.TRANSPARENT);
        forward.setLayoutParams(btnlayout);
        forward.setOnTouchListener(new OnTouchListener() {

          public boolean onTouch(View arg0, MotionEvent event) {
            if (event.getAction() == clickDown) {
              forward.setImageResource(R.drawable.right_arrow);
              return true;
            }
            if (event.getAction() == clickUp) {
              if (appView.canGoForward())
                appView.goForward();
              if (appView.canGoForward())
                forward.setImageResource(R.drawable.right_arrow_white);
              else
                forward.setImageResource(R.drawable.right_arrow);
              return true;
            }

            return false;
          }
        });

        home = new ImageButton(this.context);
        home.setImageResource(R.drawable.house_icon);
        home.setBackgroundColor(Color.TRANSPARENT);
        home.setLayoutParams(btnlayout);
        home.setOnTouchListener(new OnTouchListener() {

          public boolean onTouch(View arg0, MotionEvent event) {
            if (event.getAction() == clickDown) {
              home.setBackgroundColor(Color.DKGRAY);
              return true;
            }
            if (event.getAction() == clickUp) {
              appView.loadUrl(homeUrl);
              home.setBackgroundColor(Color.TRANSPARENT);
              return true;
            }

            return false;
          }
        });
        rightlayout.addView(back);
        rightlayout.addView(forward);
        rightlayout.addView(home);
    }
    
    progressbar = new ProgressBar(this.context, null, android.R.attr.progressBarStyleSmall);
    progressbar.setLayoutParams(btnlayout);
    rightlayout.addView(progressbar);
    barlayout.addView(close);
    barlayout.addView(text);
    barlayout.addView(rightlayout);

  }
  
  public ViewGroup getView(){
	  return mainLayout;
  }
  
  public byte[] readStream(InputStream in) throws Exception {
	  BufferedInputStream bis = new BufferedInputStream(in);
	  ByteArrayOutputStream bos = new ByteArrayOutputStream();
	  byte[] buffer = new byte[1024];
	  int count = 0;
	  while((count = bis.read(buffer)) > -1){
		  bos.write(buffer, 0, count);
	  }
	  return bos.toByteArray();
  }
  
  public String readAssetScript(String pFileName) {
	  String content = "";
	  try{
		  byte[] script = readStream(this.context.getAssets().open(pFileName));
		  content = new String(script, "UTF-8");
		  LOG.d("WebViewContainer", "Loaded file " + pFileName + ": length :" + content.length());
	  }catch(Exception e){
		  LOG.e("WebViewContainer", "Failed to load script file " + pFileName, e);
	  }
	  return content;
  }

}
