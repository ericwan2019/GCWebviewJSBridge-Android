package com.cy.whe.gcwebviewjsbridge;


public interface GCWebviewJSBridge {
	
	public void send(String data);
	public void send(String data, GCCallBackFunction responseCallback);
	
	

}
