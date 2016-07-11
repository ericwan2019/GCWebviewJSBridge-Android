package com.cy.whe.gcwebviewjsbridge;

import android.content.Context;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GCBridgeUtil {
	final static String CY_OVERRIDE_SCHEMA = "gcwvjsbscheme://";
	final static String CY_RETURN_DATA = CY_OVERRIDE_SCHEMA + "return/";//格式为   gcwvjsbscheme://return/{function}/returncontent
	final static String CY_FETCH_QUEUE = CY_RETURN_DATA + "_fetchQueue/";
	final static String CY_EMPTY_STR = "";
	final static String CY_SPLIT_MARK = "/";

	final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:GCWebviewAndroidJSBridge._handleMessageFromNative('%s');";
	final static String JS_FETCH_QUEUE_FROM_JAVA = "javascript:GCWebviewAndroidJSBridge._fetchQueue();";


	public static String parseFunctionName(String jsUrl){
		return jsUrl.replace("javascript:GCWebviewAndroidJSBridge.", "").replaceAll("\\(.*\\);", "");
	}
	
	
	public static String getDataFromReturnUrl(String url) {
		if(url.startsWith(CY_FETCH_QUEUE)) {
			return url.replace(CY_FETCH_QUEUE, CY_EMPTY_STR);
		}
		
		String temp = url.replace(CY_RETURN_DATA, CY_EMPTY_STR);
		String[] functionAndData = temp.split(CY_SPLIT_MARK);

        if(functionAndData.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < functionAndData.length; i++) {
                sb.append(functionAndData[i]);
            }
            return sb.toString();
        }
		return null;
	}

	public static String getFunctionFromReturnUrl(String url) {
		String temp = url.replace(CY_RETURN_DATA, CY_EMPTY_STR);
		String[] functionAndData = temp.split(CY_SPLIT_MARK);
		if(functionAndData.length >= 1){
			return functionAndData[0];
		}
		return null;
	}

	
	
	/**
	 * js 文件将注入为第一个script引用
	 * @param view
	 * @param path
	 */
    public static void webViewLoadLocalJs(WebView view, String path){
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
    }
	
	public static String assetFile2Str(Context c, String urlStr){
		InputStream in = null;
		try{
			in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null && !line.matches("^\\s*\\/\\/.*")) {
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();
 
            return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
}
