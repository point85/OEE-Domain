package org.point85.domain.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.MaterialResponseDto;
import org.point85.domain.dto.PlantEntityResponseDto;
import org.point85.domain.dto.ReasonResponseDto;
import org.point85.domain.dto.SourceIdResponseDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.plant.PlantEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Class for making HTTP GET and PUT requests
 */
public class HttpOeeClient {
	// logger
	private Logger logger = LoggerFactory.getLogger(HttpOeeClient.class);

	// verbs
	private static final String GET = "GET";
	private static final String POST = "POST";

	// timeout for a GET/POST
	private static final int CONN_TIMEOUT = 15000;

	// HTTP data source
	private HttpSource httpSource;

	// JSON parser
	private final Gson gson = new Gson();

	public HttpOeeClient(HttpSource httpSource) {
		this.httpSource = httpSource;
	}

	private void checkResponseCode(HttpURLConnection conn) throws Exception {
		int codeGroup = conn.getResponseCode() / 100;

		if (codeGroup != 2) {
			String msg = DomainLocalizer.instance().getErrorString("failed.code", conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;

			while ((output = br.readLine()) != null) {
				msg += "\n" + output;
			}
			throw new Exception(msg);
		}
	}

	private String buildHttpUrl(String resource) throws Exception {
		if (httpSource == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.http.server"));
		}
		return "http://" + httpSource.getHost() + ":" + httpSource.getPort() + '/' + resource;
	}

	/**
	 * Sent an HTTP GET request
	 * 
	 * @param endpoint   URL resource
	 * @param parameters Hashtable of query parameters; Key = String, Value = String
	 * @return Request response as String
	 * @throws Exception Exception
	 */
	public String sendGetRequest(String endpoint, Properties parameters) throws Exception {
		HttpURLConnection conn = null;
		String result;

		try {
			// build URL
			String urlString = buildHttpUrl(endpoint);

			if (parameters != null) {
				for (Entry<Object, Object> entry : parameters.entrySet()) {
					urlString = addQueryParameter(urlString, (String) entry.getKey(), (String) entry.getValue());
				}
			}

			URL url = new URL(urlString);

			if (logger.isInfoEnabled()) {
				logger.info("Sending HTTP get request: " + url);
			}

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(GET);
			conn.setRequestProperty("User-Agent", "Point85");
			conn.setReadTimeout(CONN_TIMEOUT);

			checkResponseCode(conn);

			// return the data
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			StringBuilder sb = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();

			result = sb.toString();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return result;
	}

	private String encode(String input) {
		CharSequence space = " ";
		CharSequence encodedSpace = "%20";
		return input.replace(space, encodedSpace);
	}

	private String addQueryParameter(String url, String name, String value) {
		StringBuilder sb = new StringBuilder();

		if (!url.contains("?")) {
			sb.append('?');
		} else {
			sb.append('&');
		}

		// replace spaces
		sb.append(name).append('=').append(encode(value));

		return url + sb.toString();
	}

	/**
	 * Get all materials
	 * 
	 * @return {@link MaterialResponseDto}
	 * @throws Exception Exception
	 */
	public MaterialResponseDto getMaterials() throws Exception {
		String response = sendGetRequest(OeeHttpServer.MATERIAL_EP, null);

		MaterialResponseDto listDto = gson.fromJson(response, MaterialResponseDto.class);
		return listDto;
	}

	/**
	 * Get all plant entities
	 * 
	 * @return {@link PlantEntityResponseDto}
	 * @throws Exception Exception
	 */
	public PlantEntityResponseDto getPlantEntities() throws Exception {
		String response = sendGetRequest(OeeHttpServer.ENTITY_EP, null);
		PlantEntityResponseDto dto = gson.fromJson(response, PlantEntityResponseDto.class);
		return dto;
	}

	/**
	 * Get all reasons
	 * 
	 * @return {@link ReasonResponseDto}
	 * @throws Exception Exception
	 */
	public ReasonResponseDto getReasons() throws Exception {
		String response = sendGetRequest(OeeHttpServer.REASON_EP, null);
		ReasonResponseDto dto = gson.fromJson(response, ReasonResponseDto.class);
		return dto;
	}

	/**
	 * Get all source Ids
	 * 
	 * @param entity     {@link PlantEntity}
	 * @param sourceType {@link DataSourceType}
	 * @return {@link SourceIdResponseDto}
	 * @throws Exception Exception
	 */
	public SourceIdResponseDto getSourceIds(PlantEntity entity, DataSourceType sourceType) throws Exception {
		Properties parameters = new Properties();
		parameters.put(OeeHttpServer.EQUIP_ATTRIB, entity.getName());
		parameters.put(OeeHttpServer.DS_TYPE_ATTRIB, sourceType.name());

		String response = sendGetRequest(OeeHttpServer.SOURCE_ID_EP, parameters);
		SourceIdResponseDto dto = gson.fromJson(response, SourceIdResponseDto.class);
		return dto;
	}

	/**
	 * Post an equipment event with this payload
	 * 
	 * @param payload Post content
	 * @throws Exception Exception
	 */
	public void postEquipmentEvent(String payload) throws Exception {
		sendPostRequest(OeeHttpServer.EVENT_EP, payload);

	}

	/**
	 * Send a POST request
	 * 
	 * @param endpoint Resource
	 * @param payload  Content of POST
	 * @throws Exception Exception
	 */
	public void sendPostRequest(String endpoint, String payload) throws Exception {
		HttpURLConnection conn = null;

		try {
			// build URL
			String urlString = buildHttpUrl(endpoint);

			URL url = new URL(urlString);

			if (logger.isInfoEnabled()) {
				logger.info("Sending HTTP post request: " + url + " with payload " + payload);
			}

			// create a connection for a JSON POST request
			conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setRequestMethod(POST);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setConnectTimeout(CONN_TIMEOUT);

			// make the request
			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			if (logger.isInfoEnabled()) {
				logger.info("Posted request to URL " + url + " with payload " + payload);
			}

			checkResponseCode(conn);

		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
}
