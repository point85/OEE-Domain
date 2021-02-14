package org.point85.domain.proficy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ProficyClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(ProficyClient.class);

	// token time left threshold
	private static int EXPIRATION_THRESHOLD_SEC = 60;

	public static String REST_API = "/historian-rest-api/v1/";

	private String host;
	private Integer uaaHttpPort;
	private Integer httpsPort;
	private Integer httpPort;
	private String userName;
	private String password;
	private boolean enableSSLCertificateValidation = true;

	// JSON parser
	private final Gson gson = new Gson();

	private OauthBearerToken bearerToken;

	public ProficyClient(String host, Integer httpsPort, Integer httpPort) {
		this.host = host;
		this.httpsPort = httpsPort;
		this.httpPort = httpPort;
	}

	public void setBasicAuthenticationInfo(int uaaHhttpPort, String userName, String password) {
		this.uaaHttpPort = uaaHhttpPort;
		this.userName = userName;
		this.password = password;
	}

	public OauthBearerToken getBearerToken() throws Exception {
		if (isTokenExpiring()) {
			// get a fresh token
			String json = sendBasicAuthenticationRequest();
			bearerToken = gson.fromJson(json, OauthBearerToken.class);
			bearerToken.setExpirationTime(LocalDateTime.now().plusSeconds(bearerToken.getExpiresIn()));
		}
		return bearerToken;
	}

	private boolean isTokenExpiring() {
		if (bearerToken == null) {
			return true;
		}
		return Duration.between(bearerToken.getExpirationTime(), LocalDateTime.now())
				.getSeconds() < EXPIRATION_THRESHOLD_SEC;
	}

	private String sendBasicAuthenticationRequest() throws Exception {
		String usernameColonPassword = userName + ":" + password;
		String uaaUrl = "http://" + host + ":" + uaaHttpPort + "/uaa/oauth/token?grant_type=client_credentials";

		String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());

		if (logger.isInfoEnabled()) {
			System.out.println("Sending UAA Oath token request to " + uaaUrl + " for user " + userName);
		}

		BufferedReader httpResponseReader = null;
		try {
			// Connect to the web server endpoint
			URL serverUrl = new URL(uaaUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();

			// Set HTTP method as GET
			urlConnection.setRequestMethod("GET");

			// Include the HTTP Basic Authentication payload
			urlConnection.addRequestProperty("Authorization", basicAuthPayload);

			// Read response from web server, which will trigger HTTP Basic Authentication
			// request to be sent.
			httpResponseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String lineRead = null;
			String json = null;
			while ((lineRead = httpResponseReader.readLine()) != null) {
				// just one line
				json = lineRead;
			}

			return json;

		} finally {
			if (httpResponseReader != null) {
				try {
					httpResponseReader.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}
	}

	private String buildUrl(String resource, Map<String, String> parameters) {
		StringBuffer buffer = new StringBuffer();
		String protocol = httpsPort != null ? "https" : "http";
		Integer port = httpsPort != null ? httpsPort : httpPort;
		buffer.append(protocol).append("://").append(host).append(":").append(port).append(REST_API);
		buffer.append(resource);

		char separator = '?';

		for (Entry<String, String> entry : parameters.entrySet()) {
			buffer.append(separator).append(entry.getKey()).append("=").append(entry.getValue());
			separator = '&';
		}

		return buffer.toString();
	}

	private void checkSSLCertificateValidation() throws Exception {
		if (isEnableSSLCertificateValidation()) {
			if (logger.isInfoEnabled()) {
				System.out.println("SSL certificate validation is enabled.");
			}
			return;
		}

		// disable certificate validation
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}
		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		if (logger.isInfoEnabled()) {
			System.out.println("SSL certificate validation is disabled.");
		}
	}

	public String sendGetRequest(String resource, Map<String, String> parameters) throws Exception {
		checkSSLCertificateValidation();

		URL url = new URL(buildUrl(resource, parameters));
		// HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestProperty("Authorization", "Bearer " + getBearerToken().getAccessToken());
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestMethod("GET");

		if (logger.isInfoEnabled()) {
			System.out.println("Sending GET request to " + url);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String output;

		StringBuffer responseBuffer = new StringBuffer();
		while ((output = in.readLine()) != null) {
			responseBuffer.append(output);
		}

		in.close();
		return responseBuffer.toString();
	}

	public Object serialize(String json, Class<?> klass) {
		return gson.fromJson(json, klass);
	}

	public static void main(String[] args) {
		ProficyClient client = new ProficyClient("localhost", 443, 8070);
		client.setBasicAuthenticationInfo(9480, "DESKTOP-2Q64KVP.admin", "point85");
		client.setEnableSSLCertificateValidation(false);

		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("nameMask", "POINT85*");
			parameters.put("maxNumber", "100");

			// client.sendGetRequest("historian-rest-api/v1/tags?nameMask=POINT85*&maxNumber=100");
			String value = client.sendGetRequest("tags", parameters);
			Tags tags = (Tags) client.serialize(value, Tags.class);
			tags.getTags();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(int httpsPort) {
		this.httpsPort = httpsPort;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public boolean isEnableSSLCertificateValidation() {
		return enableSSLCertificateValidation;
	}

	public void setEnableSSLCertificateValidation(boolean value) {
		enableSSLCertificateValidation = value;
	}
}
