package com.feedhenry.phonegap.webview;

import android.view.Menu;

public class OptionMenuManager {

	public boolean populateMenu(Menu menu) {
		
		return false;		
		
	}
	
	public boolean popViewCtrlMenu(Menu menu)
	{
		menu.add("Close");
		return true;
	}

}
