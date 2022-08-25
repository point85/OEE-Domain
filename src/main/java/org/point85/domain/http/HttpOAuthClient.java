package org.point85.domain.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for making OAuth 2.0 requests
 *
 */
public class HttpOAuthClient {
	private static final String CONTENT_TYPE_NAME = "Content-Type";
	private static final String JSON_VALUE = "application/json";
	private static final String ACCEPT_NAME = "Accept";
	private static final String AUTHORIZATION = "Authorization";

	// logger
	private static final Logger logger = LoggerFactory.getLogger(HttpOAuthClient.class);

	// OAuth access token
	private OAuthAccessToken oauthAccessToken = new OAuthAccessToken();

	/**
	 * Send an HTTP request with an access token
	 * 
	 * @param urlPath     URL path
	 * @param verb        Request verb, e.g. "POST"
	 * @param payload     JSON Body content
	 * @param accessToken {@link OAuthAccessToken}
	 * @return Response JSON content
	 * @throws Exception Exception
	 */
	public String sendRequest(String urlPath, String verb, String payload, OAuthAccessToken accessToken)
			throws Exception {

		HttpURLConnection conn = null;

		try {
			// send event
			URL url = new URL(urlPath);
			conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setRequestMethod(verb);

			// OAuth authorization
			if (accessToken != null) {
				conn.setRequestProperty(AUTHORIZATION, accessToken.getTokenType() + " " + accessToken.getAccessToken());
			}

			conn.setRequestProperty(ACCEPT_NAME, JSON_VALUE);
			conn.setRequestProperty(CONTENT_TYPE_NAME, JSON_VALUE);

			if (payload != null) {
				// write body
				OutputStream os = conn.getOutputStream();
				os.write(payload.getBytes());
				os.flush();
			}

			if (logger.isInfoEnabled()) {
				logger.info("Sent " + verb + " request to " + urlPath + " with payload\n " + payload);
			}

			// read the response
			InputStream is = conn.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader in = new BufferedReader(isr);

			String output = null;

			StringBuilder sb = new StringBuilder();
			while ((output = in.readLine()) != null) {
				sb.append(output);
			}
			in.close();

			String response = sb.toString();

			if (logger.isInfoEnabled()) {
				logger.info("Response: " + response);
			}

			return response;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public OAuthAccessToken getOAuthAccessToken() {
		return oauthAccessToken;
	}

	public void setOauthAccessToken(OAuthAccessToken token) {
		this.oauthAccessToken = token;
	}
}
