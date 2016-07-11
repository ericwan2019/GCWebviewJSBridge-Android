package com.cy.whe.gcwebviewjsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class GCBridgeWebView extends WebView implements GCWebviewJSBridge {

	private final String TAG = "GCWebviewJSBridge";

	public static final String toLoadJs = "GCWebviewJSBridge.js";
	Map<String, GCCallBackFunction> responseCallbacks = new HashMap<String, GCCallBackFunction>();
	Map<String, GCBridgeHandler> messageHandlers = new HashMap<String, GCBridgeHandler>();
	GCBridgeHandler defaultHandler = new GCDefaultHandler();

	private List<GCMessage> startupMessage = new ArrayList<GCMessage>();

	public List<GCMessage> getStartupMessage() {
		return startupMessage;
	}

	public void setStartupMessage(List<GCMessage> startupMessage) {
		this.startupMessage = startupMessage;
	}


	public GCBridgeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GCBridgeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GCBridgeWebView(Context context) {
		super(context);
		init();
	}

	/**
	 * 
	 * @param handler
	 *            default handler,handle messages send by js without assigned handler name,
     *            if js message has handler name, it will be handled by named handlers registered by native
	 */
	public void setDefaultHandler(GCBridgeHandler handler) {
       this.defaultHandler = handler;
	}

    private void init() {
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
		this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
		this.setWebViewClient(generateBridgeWebViewClient());
	}

    protected GCBridgeWebViewClient generateBridgeWebViewClient() {
        return new GCBridgeWebViewClient(this);
    }

	void handlerReturnData(String url) {
		String functionName = GCBridgeUtil.getFunctionFromReturnUrl(url);
		GCCallBackFunction f = responseCallbacks.get(functionName);
		String data = GCBridgeUtil.getDataFromReturnUrl(url);
		if (f != null) {
			f.onCallBack(data);
			responseCallbacks.remove(functionName);
			return;
		}
	}

	@Override
	public void send(String data) {
		send(data, null);
	}

	@Override
	public void send(String data, GCCallBackFunction responseCallback) {
		doSend(null, data, responseCallback);
	}

	private void doSend(String handlerName, String data, GCCallBackFunction responseCallback) {
		GCMessage m = new GCMessage();
		if (!TextUtils.isEmpty(data)) {
			m.setData(data);
		}
		if (responseCallback != null) {
			String callbackStr = String.format("gc_android_cy_%s",System.currentTimeMillis()) ;//String.format(GCBridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (GCBridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
			responseCallbacks.put(callbackStr, responseCallback);
			m.setCallbackId(callbackStr);
		}
		if (!TextUtils.isEmpty(handlerName)) {
			m.setHandlerName(handlerName);
		}
		queueMessage(m);
	}

	private void queueMessage(GCMessage m) {
		if (startupMessage != null) {
			startupMessage.add(m);
		} else {
			dispatchMessage(m);
		}
	}

	void dispatchMessage(GCMessage m) {
        String messageJson = m.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(GCBridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

	void flushMessageQueue() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			loadUrl(GCBridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new GCCallBackFunction() {

				@Override
				public void onCallBack(String data) {
					// deserializeMessage
					List<GCMessage> list = null;
					try {
						list = GCMessage.toArrayList(data);
					} catch (Exception e) {
                        e.printStackTrace();
						return;
					}
					if (list == null || list.size() == 0) {
						return;
					}
					for (int i = 0; i < list.size(); i++) {
						GCMessage m = list.get(i);
						String responseId = m.getResponseId();
						// 是否是response
						if (!TextUtils.isEmpty(responseId)) {
							GCCallBackFunction function = responseCallbacks.get(responseId);
							String responseData = m.getResponseData();
							function.onCallBack(responseData);
							responseCallbacks.remove(responseId);
						} else {
							GCCallBackFunction responseFunction = null;
							// if had callbackId
							final String callbackId = m.getCallbackId();
							if (!TextUtils.isEmpty(callbackId)) {
								responseFunction = new GCCallBackFunction() {
									@Override
									public void onCallBack(String data) {
										GCMessage responseMsg = new GCMessage();
										responseMsg.setResponseId(callbackId);
										responseMsg.setResponseData(data);
										queueMessage(responseMsg);
									}
								};
							} else {
								responseFunction = new GCCallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// do nothing
									}
								};
							}
							GCBridgeHandler handler;
							if (!TextUtils.isEmpty(m.getHandlerName())) {
								handler = messageHandlers.get(m.getHandlerName());
							} else {
								handler = defaultHandler;
							}
							if (handler != null){
								handler.handler(m.getData(), responseFunction);
							}
						}
					}
				}
			});
		}
	}

	public void loadUrl(String jsUrl, GCCallBackFunction returnCallback) {
		this.loadUrl(jsUrl);
		responseCallbacks.put(GCBridgeUtil.parseFunctionName(jsUrl), returnCallback);
	}

	/**
	 * register handler,so that javascript can call it
	 * 
	 * @param handlerName
	 * @param handler
	 */
	public void registerHandler(String handlerName, GCBridgeHandler handler) {
		if (handler != null) {
			messageHandlers.put(handlerName, handler);
		}
	}

	/**
	 * call javascript registered handler
	 *
     * @param handlerName
	 * @param data
	 * @param callBack
	 */
	public void callHandler(String handlerName, String data, GCCallBackFunction callBack) {
        doSend(handlerName, data, callBack);
	}


	/**
	 * 判断URL是否是返回数据
	 * @param url
	 * @return
     */
	public boolean isReturnData(String url){
		return  url.startsWith(GCBridgeUtil.CY_RETURN_DATA);
	}



	/**
	 * 判断URL是否是桥接
	 * @param url
	 * @return
     */
	public boolean isBridgeSchemeUrl(String url){
		return url.startsWith(GCBridgeUtil.CY_OVERRIDE_SCHEMA);
	}







}
