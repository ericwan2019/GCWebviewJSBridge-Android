package com.cy.whe.gcwebviewjsbridge;

public class GCDefaultHandler implements GCBridgeHandler {

	String TAG = "GCDefaultHandler";
	
	@Override
	public void handler(String data, GCCallBackFunction function) {
		if(function != null){
			function.onCallBack("GCDefaultHandler response data");
		}
	}

}
