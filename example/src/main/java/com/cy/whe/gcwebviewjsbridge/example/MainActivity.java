package com.cy.whe.gcwebviewjsbridge.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Button;

import com.cy.whe.gcwebviewjsbridge.GCBridgeHandler;
import com.cy.whe.gcwebviewjsbridge.GCBridgeWebView;
import com.cy.whe.gcwebviewjsbridge.GCCallBackFunction;
import com.cy.whe.gcwebviewjsbridge.GCDefaultHandler;
import com.google.gson.Gson;

public class MainActivity extends Activity implements OnClickListener {

	private final String TAG = "GCWebviewJSBridge";

	GCBridgeWebView webView;

	Button button;

	int RESULT_CODE = 0;

	ValueCallback<Uri> mUploadMessage;

    static class Location {
        String address;
    }

    static class User {
        String name;
        Location location;
        String testStr;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        webView = (GCBridgeWebView) findViewById(R.id.webView);

		button = (Button) findViewById(R.id.button);

		button.setOnClickListener(this);

		webView.setDefaultHandler(new GCDefaultHandler());

		webView.setWebChromeClient(new WebChromeClient() {


		});

		webView.loadUrl("file:///android_asset/demo.html");

		webView.registerHandler("testObjcCallback", new GCBridgeHandler() {

			@Override
			public void handler(String data, GCCallBackFunction function) {
				Log.i(TAG, "handler = testObjcCallback, data from web = " + data);
                function.onCallBack("testObjcCallback, response data 中文 from Java");
			}

		});

        User user = new User();
        Location location = new Location();
        location.address = "SDU";
        user.location = location;
        user.name = "大头鬼";

        webView.callHandler("testJavascriptHandler", new Gson().toJson(user), new GCCallBackFunction() {
            @Override
            public void onCallBack(String data) {
				Log.i(TAG,"response data = "+data);
            }
        });

        webView.send("hello");

	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == RESULT_CODE) {
			if (null == mUploadMessage){
				return;
			}
			Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;
		}
	}

	@Override
	public void onClick(View v) {
		if (button.equals(v)) {
            webView.callHandler("testJavascriptHandler", "data from Java", new GCCallBackFunction() {

				@Override
				public void onCallBack(String data) {
					// TODO Auto-generated method stub
					Log.i(TAG, "reponse data from js " + data);
				}

			});
		}

	}

}
