package com.fm.demoConnectivity;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.identity.client.UaaContext;
import org.cloudfoundry.identity.client.UaaContextFactory;
import org.cloudfoundry.identity.client.token.GrantType;
import org.cloudfoundry.identity.client.token.TokenRequest;
import org.cloudfoundry.identity.uaa.oauth.token.CompositeAccessToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FetchOnPremData {
	@RequestMapping("/fetchData")
	public String fetchDataController() {
		return fetchJsonData();

	}

	public String fetchJsonData() {
		try {
			HttpURLConnection urlConnection = null;
			// fetching variable values env variables
			
			// 36-39 xsuua env vars
			JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
			JSONArray jsonArr = jsonObj.getJSONArray("xsuaa");
			JSONObject jsoncredentials = jsonArr.getJSONObject(0).getJSONObject("credentials");
			URI xsUaaUrl = new URI(jsoncredentials.getString("url"));
			
			// 42-47 connectivity env var
			JSONArray jsonArrConnect = jsonObj.getJSONArray("connectivity");
			JSONObject jsoncredentialsConn = jsonArrConnect.getJSONObject(0).getJSONObject("credentials");
			String onprem_proxy_host = jsoncredentialsConn.getString("onpremise_proxy_host");
			int onprem_proxy_port = Integer.parseInt(jsoncredentialsConn.getString("onpremise_proxy_port"));
			String clientId = jsoncredentialsConn.getString("clientid");
			String clientSecret = jsoncredentialsConn.getString("clientsecret");
			
		
			
			// fetching a token from xsuaa using connectivity client id, secret which is needed by connectivity service 57-64
			UaaContextFactory factory = UaaContextFactory.factory(xsUaaUrl).authorizePath("/oauth/authorize").tokenPath("/oauth/token");
			TokenRequest tokenRequest = factory.tokenRequest();
			tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
			tokenRequest.setClientId(clientId);
			tokenRequest.setClientSecret(clientSecret);
			UaaContext xsUaaContext = factory.authenticate(tokenRequest);
			CompositeAccessToken jwtToken1 = xsUaaContext.getToken();
			

			// creating url object for calling er9 backend
			URL url = new URL(
					"http://ldai1er9.wdf.sap.corp:44300/sap/opu/odata/SAP/ZGEMINI_BO_SRV/AreaValueHelp?$format=json&sap-client=001");
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(onprem_proxy_host, onprem_proxy_port));
			urlConnection = (HttpURLConnection) url.openConnection(proxy);
			
			
			//adding the token fetched in er9 request
			urlConnection.setRequestProperty("Proxy-Authorization", "Bearer " + jwtToken1);
			
			// adding username and password Basic authentication to back end (er9)
			urlConnection.setRequestProperty("Authorization", "Basic");
			
			// getting the output from er9 into input stream object and converting into string and returning it
			InputStream instream = urlConnection.getInputStream();
			String outString = "";
			StringWriter writer = new StringWriter();
			IOUtils.copy(instream, writer);
			outString = writer.toString();
			return outString;
		} 
		catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
			
		}
	}
}
