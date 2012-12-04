/*******************************************************************************
 * Copyright (c) 2010, St. Schenk and Medshare GmbH
 *
 * Login EBM by HTTP POST
 *
 *******************************************************************************/

package ch.medshare.ebm;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;

import ch.elexis.Hub;

public class EbmLogIn {
	private String x = "";
	private String g = "";
	
	public static void main(String args[]){}
	
	public String doPostLogin(String suchstring){
		HttpClient httpClient = new HttpClient();
		
		String url = Hub.userCfg.get(Preferences.URL, Preferences.Defaults.URL);
		PostMethod post = new PostMethod(url);
		
		post.addParameter("USER", Hub.userCfg.get(Preferences.USER, Preferences.Defaults.USER));
		post.addParameter("PASS", Hub.userCfg.get(Preferences.PASS, Preferences.Defaults.PASS));
		if (!suchstring.isEmpty()) {
			post.addParameter("suchstring", suchstring);
		}
		
		try {
			httpClient.executeMethod(post);
		} catch (HttpException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			x = post.getResponseBodyAsString();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
		}
		
		final Header l = post.getResponseHeader("location");
		if (l != null) {
			g = l.getValue();
		} else {
			g = x;
		}
		
		post.releaseConnection();
		
		return g;
	}
}
