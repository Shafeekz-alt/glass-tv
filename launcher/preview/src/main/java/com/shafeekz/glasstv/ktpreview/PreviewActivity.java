package com.shafeekz.glasstv.ktpreview;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class PreviewActivity extends Activity {
  private WebView web;
  private final Handler main = new Handler(Looper.getMainLooper());
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    web = new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(true);
    web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new Bridge(), "GlassTVAndroid");
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }
  class Bridge {
    @JavascriptInterface public void requestPrayerTimes() {
      new Thread(() -> {
        String payload;
        HttpURLConnection c = null;
        try {
          URL u = new URL("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=TRG01");
          c = (HttpURLConnection) u.openConnection();
          c.setConnectTimeout(9000);
          c.setReadTimeout(9000);
          c.setRequestProperty("Accept","application/json");
          if(c.getResponseCode()!=200)throw new Exception("JAKIM HTTP "+c.getResponseCode());
          try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192]; int n;
            while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>524288)throw new Exception("Respons terlalu besar");}
            payload=out.toString("UTF-8");
          }
          new JSONObject(payload); // verify JSON object
        }catch(Exception ex){
          payload="{\"error\":"+JSONObject.quote(ex.getMessage()==null?"Sambungan gagal":ex.getMessage())+"}";
        }finally{if(c!=null)c.disconnect();}
        String data=JSONObject.quote(payload);
        main.post(()->web.evaluateJavascript("window.onNativePrayer("+data+")",null));
      }).start();
    }
    @JavascriptInterface public void openWifi(){
      main.post(()->{try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}catch(Exception ignored){}});
    }
    @JavascriptInterface public void openApp(String name){
      final String pkg;
      switch(name){
        case "YouTube": pkg="com.google.android.youtube.tv";break;
        case "Disney+": pkg="com.disney.disneyplus";break;
        case "Apple TV": pkg="com.apple.atve.androidtv.appletv";break;
        case "BrowseHere": pkg="com.tcl.browser";break;
        default:return;
      }
      main.post(()->{Intent launch=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(launch==null)launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch!=null)startActivity(launch);});
    }
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
