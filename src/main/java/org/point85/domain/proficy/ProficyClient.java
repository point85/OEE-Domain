package org.point85.domain.proficy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.polling.PollingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * A ProficyClient communicates with a historian via the REST API. It polls the
 * historian for tag values for new OEE events.
 *
 */
public class ProficyClient extends PollingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(ProficyClient.class);

	// token time left threshold
	private static final int EXPIRATION_THRESHOLD_SEC = 60;

	// REST URL
	private static final String REST_API = "/historian-rest-api/v1/";

	// REST endpoints
	public static final String TAGS_ENDPOINT = "tags";
	public static final String TAGS_LIST_ENDPOINT = "tagslist";
	public static final String CURR_VALUE_ENDPOINT = "datapoints/currentvalue";
	public static final String RAW_VALUE_ENDPOINT = "datapoints/raw";
	public static final String WRITE_VALUE_ENDPOINT = "datapoints/create";
	public static final String SAMPLED_ENDPOINT = "sampled";

	// JSON parser
	private final Gson gson = new Gson();

	// Oauth token
	private OauthBearerToken bearerToken;

	// listener to call back for tag data events
	private ProficyEventListener eventListener;

	// watch mode flag for trend chart
	private boolean watchMode = false;

	// time when polling started
	private Instant pollingStartTime = Instant.now();

	/**
	 * Construct a Proficy client
	 * 
	 * @param dataSource {@link ProficySource}
	 */
	public ProficyClient(ProficySource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Construct a Proficy client
	 * 
	 * @param eventListener  {@link ProficyEventListener}
	 * @param dataSource     {@link ProficySource}
	 * @param sourceIds      List of tag names to monitor
	 * @param pollingPeriods List of polling periods for each tag
	 */
	public ProficyClient(ProficyEventListener eventListener, ProficySource dataSource, List<String> sourceIds,
			List<Integer> pollingPeriods) {
		super(dataSource, sourceIds, pollingPeriods);
		this.eventListener = eventListener;
	}

	private OauthBearerToken getBearerToken() throws Exception {
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

	private ProficySource getProficySource() {
		return (ProficySource) getDataSource();
	}

	private String sendBasicAuthenticationRequest() throws Exception {
		String usernameColonPassword = getProficySource().getUserName() + ":" + getProficySource().getUserPassword();
		String uaaUrl = "http://" + getProficySource().getHost() + ":" + getProficySource().getUaaHttpPort()
				+ "/uaa/oauth/token?grant_type=client_credentials";

		String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());

		if (logger.isInfoEnabled()) {
			logger.info(
					"Sending UAA Oath token request to " + uaaUrl + " for user " + getProficySource().getUserName());
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

	private String buildUrl(String resource, Map<String, String> parameters) throws Exception {
		if (getProficySource().getHost() == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("proficy.no.host"));
		}

		Integer port = getProficySource().getHttpsPort() != null ? getProficySource().getHttpsPort()
				: getProficySource().getHttpPort();

		if (port == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("proficy.no.port"));
		}

		String protocol = getProficySource().getHttpsPort() != null ? "https" : "http";

		StringBuilder builder = new StringBuilder();
		builder.append(protocol).append("://").append(getProficySource().getHost()).append(":").append(port)
				.append(REST_API);
		builder.append(resource);

		char separator = '?';

		if (parameters != null) {
			for (Entry<String, String> entry : parameters.entrySet()) {
				builder.append(separator).append(entry.getKey()).append("=").append(entry.getValue());
				separator = '&';
			}
		}
		return builder.toString();
	}

	private void checkSSLCertificateValidation() throws Exception {
		if (getProficySource().getValidateCertificate()) {
			if (logger.isInfoEnabled()) {
				logger.info("SSL certificate validation is enabled.");
			}
			return;
		}

		// disable certificate validation
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// not implemented
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// not implemented
			}
		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		if (logger.isInfoEnabled()) {
			logger.info("SSL certificate validation is disabled.");
		}
	}

	private String sendGetRequest(String resource, Map<String, String> parameters) throws Exception {
		checkSSLCertificateValidation();
		HttpURLConnection conn = null;

		try {
			URL url = new URL(buildUrl(resource, parameters));
			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Authorization", "Bearer " + getBearerToken().getAccessToken());
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestMethod("GET");

			if (logger.isInfoEnabled()) {
				logger.info("Sending GET request to " + url);
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output;

			StringBuilder responseBuffer = new StringBuilder();
			while ((output = in.readLine()) != null) {
				responseBuffer.append(output);
			}

			in.close();
			return responseBuffer.toString();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void sendPostRequest(String resource, Map<String, String> parameters, TagData body) throws Exception {
		checkSSLCertificateValidation();
		HttpURLConnection conn = null;

		try {
			// POST event
			URL url = new URL(buildUrl(resource, parameters));
			conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Bearer " + getBearerToken().getAccessToken());
			conn.setRequestProperty("Content-Type", "application/json");

			// serialize the body
			String payload = gson.toJson(body);

			if (logger.isInfoEnabled()) {
				logger.info("Sending POST request to " + url + "\n content: " + payload);
			}

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			checkResponseCode(conn);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private Object serialize(String json, Class<?> klass) {
		return gson.fromJson(json, klass);
	}

	/**
	 * Read the tag names matching the mask up to the number specified
	 * 
	 * @param nameMask  Name mask with wild card characters
	 * @param maxNumber Maximum number of tag names to return
	 * @return List of matching tag names
	 * @throws Exception Exception
	 */
	public List<String> readTagNames(String nameMask, Integer maxNumber) throws Exception {
		Map<String, String> parameters = new HashMap<>();

		if (nameMask != null && !nameMask.isEmpty()) {
			parameters.put("nameMask", nameMask);
		}

		if (maxNumber != null) {
			parameters.put("maxNumber", String.valueOf(maxNumber));
		}

		// tags API
		String value = sendGetRequest(ProficyClient.TAGS_ENDPOINT, parameters);
		Tags tags = (Tags) serialize(value, Tags.class);
		return tags.getTags();
	}

	/**
	 * Read the tag details per the request parameters
	 * 
	 * @param parameters {@link TagsListRequestParameters}
	 * @return Tags list {@link TagsList}
	 * @throws Exception Exception
	 */
	public TagsList readTagsList(TagsListRequestParameters parameters) throws Exception {
		String value = sendGetRequest(ProficyClient.TAGS_LIST_ENDPOINT, parameters.getParameters());
		return (TagsList) serialize(value, TagsList.class);
	}

	private void checkErrorCode(int code, String context) throws Exception {
		if (code != ErrorCode.Success.getCode()) {
			ErrorCode errorCode = ErrorCode.fromInt(code);
			String msg = DomainLocalizer.instance().getErrorString(errorCode.name());

			throw new Exception(context != null ? context + ": " + msg : msg);
		}
	}

	/**
	 * Read the current value of the specified tags
	 * 
	 * @param tagNames List of tag names
	 * @return Tag values {@link TagValues}
	 * @throws Exception Exception
	 */
	public TagValues readCurrentValue(List<String> tagNames) throws Exception {
		String tagList = "";
		for (String tagName : tagNames) {
			if (!tagList.isEmpty()) {
				tagList += ";";
			}
			tagList += tagName;
		}

		Map<String, String> parameters = new HashMap<>();
		parameters.put("tagNames", tagList);

		String value = sendGetRequest(ProficyClient.CURR_VALUE_ENDPOINT, parameters);
		TagValues currentValues = (TagValues) serialize(value, TagValues.class);

		checkErrorCode(currentValues.getErrorCode(), null);

		for (TagData tagData : currentValues.getTagData()) {
			checkErrorCode(tagData.getErrorCode(), tagData.getTagName());
		}

		return currentValues;
	}

	/**
	 * Read the stored tag values for the specified time period and direction
	 * 
	 * @param tagNames  List of tag names to read
	 * @param start     Starting time instant
	 * @param end       Ending time instant
	 * @param direction {@link TagDirection}
	 * @param count     Maximum number of values to read
	 * @return Tag data values {@link TagValues}
	 * @throws Exception Exception
	 */
	public TagValues readRawDatapoints(List<String> tagNames, Instant start, Instant end, TagDirection direction,
			Integer count) throws Exception {
		String startTime = start.truncatedTo(ChronoUnit.MILLIS).toString();
		String endTime = end.truncatedTo(ChronoUnit.MILLIS).toString();

		String tags = "";

		// tag names
		for (int i = 0; i < tagNames.size(); i++) {
			if (i > 0) {
				tags += ";";
			}
			tags += tagNames.get(i);
		}

		Map<String, String> parameters = new HashMap<>();

		// fill out URL
		StringBuilder sb = new StringBuilder();
		sb.append(ProficyClient.RAW_VALUE_ENDPOINT).append('/').append(tags).append('/').append(startTime).append('/')
				.append(endTime).append('/').append(direction.getDirection());

		if (count != null) {
			sb.append('/').append(count);
		}
		String url = sb.toString();

		String value = sendGetRequest(url, parameters);
		TagValues currentValues = (TagValues) serialize(value, TagValues.class);

		checkErrorCode(currentValues.getErrorCode(), null);

		for (TagData tagData : currentValues.getTagData()) {
			checkErrorCode(tagData.getErrorCode(), tagData.getTagName());
		}

		return currentValues;
	}

	private void checkResponseCode(HttpURLConnection conn) throws Exception {
		int codeGroup = conn.getResponseCode() / 100;

		if (codeGroup != 2) {
			String msg = DomainLocalizer.instance().getErrorString("failed.code", conn.getResponseCode());
			throw new Exception(msg);
		}

		// read the response json for an error
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String output;

		StringBuilder sb = new StringBuilder();
		while ((output = in.readLine()) != null) {
			sb.append(output);
		}
		in.close();

		TagError tagError = gson.fromJson(sb.toString(), TagError.class);

		if (!tagError.getEnumeratedError().equals(ErrorCode.Success)) {
			String msg = DomainLocalizer.instance().getErrorString(tagError.getEnumeratedError().name());

			if (tagError.getErrorMessage() != null && !tagError.getErrorMessage().isEmpty()) {
				msg += ": " + tagError.getErrorMessage();
			}
			throw new Exception(msg);
		}
	}

	/**
	 * Write the data value to the tag
	 * 
	 * @param tagName Name of tag
	 * @param value   Data value
	 * @throws Exception Exception
	 */
	public void writeTag(String tagName, Object value) throws Exception {
		TagSample sample = new TagSample(value);
		TagData tagData = new TagData(tagName);
		tagData.getSamples().add(sample);

		Map<String, String> parameters = new HashMap<>();

		sendPostRequest(ProficyClient.WRITE_VALUE_ENDPOINT, parameters, tagData);
	}

	public ProficyEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(ProficyEventListener eventListener) {
		this.eventListener = eventListener;
	}

	@Override
	protected void onPoll(String sourceId) throws Exception {
		if (sourceId == null) {
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Querying for new Proficy events for tag " + sourceId);
		}

		// query historian for new events
		// get last event for any of these tags
		if (!watchMode) {
			// go to database
			OeeEvent lastEvent = PersistenceService.instance().fetchLastEvent(sourceIds);

			if (lastEvent != null) {
				pollingStartTime = lastEvent.getStartTime().toInstant();
			}
		}

		Instant end = Instant.now();

		// fetch all events in this period. Start after the last poll
		TagValues tagValues = readRawDatapoints(sourceIds, pollingStartTime.plus(1, ChronoUnit.MILLIS), end,
				TagDirection.Forward, 0);

		// find max time stamp
		List<TagData> tagDataList = tagValues.getTagData();

		Instant lastTimestamp = null;

		for (TagData data : tagDataList) {
			if (logger.isInfoEnabled() && !data.getSamples().isEmpty()) {
				logger.info("Found " + data.getSamples().size() + " samples for tag " + data.getTagName());
			}

			for (TagSample sample : data.getSamples()) {
				Instant timestamp = sample.getTimeStampInstant();

				if (lastTimestamp == null || timestamp.isAfter(lastTimestamp)) {
					lastTimestamp = timestamp;
				}
			}
		}

		if (lastTimestamp != null) {
			pollingStartTime = lastTimestamp;
		}

		// call the listener back
		for (TagData tagData : tagDataList) {
			if (!tagData.getSamples().isEmpty()) {
				eventListener.onProficyEvent(tagData);
			}
		}
	}

	public boolean isWatchMode() {
		return watchMode;
	}

	public void setWatchMode(boolean watchMode) {
		this.watchMode = watchMode;
	}
}
