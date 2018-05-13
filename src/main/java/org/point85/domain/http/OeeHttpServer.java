package org.point85.domain.http;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;

public class OeeHttpServer extends NanoHTTPD {
	// default port
	public static final int DEFAULT_PORT = 8182;

	// query string attributes
	public static final String EQUIP_ATTRIB = "equipment";
	public static final String DS_TYPE_ATTRIB = "sourceType";

	// endpoints
	public static final String ENTITY_EP = "entity";
	public static final String REASON_EP = "reason";
	public static final String MATERIAL_EP = "material";
	public static final String EVENT_EP = "event";
	public static final String SOURCE_ID_EP = "source_id";
	public static final String DATA_SOURCE_EP = "data_source";

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeHttpServer.class);

	// flag for accepting event requests
	private boolean acceptingEventRequests = false;

	// event listener
	private HttpEventListener eventListener;

	// JSON parser
	private Gson gson = new Gson();

	// server state
	public enum ServerState {
		STARTED, STOPPED
	};

	private ServerState state = ServerState.STOPPED;

	public OeeHttpServer() {
		super(DEFAULT_PORT);
	}

	public OeeHttpServer(int port) {
		super(port);
	}

	public ServerState getState() {
		return state;
	}

	public void startup() throws Exception {
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		state = ServerState.STARTED;
		// execute server on its own thread
		// executorService.execute(() -> ServerRunner.executeInstance(this));
	}

	public void shutdown() {
		if (logger.isInfoEnabled()) {
			logger.info("Shutting down HTTP server " + getHostname());
		}

		stop();
		state = ServerState.STOPPED;
	}

	// serve the request
	@SuppressWarnings("deprecation")
	@Override
	public Response serve(IHTTPSession session) {
		Response response = null;

		// accept any method
		Method method = session.getMethod();

		// end point
		String uri = session.getUri();

		Map<String, List<String>> decodedQueryParameters = decodeParameters(session.getQueryParameterString());

		if (logger.isInfoEnabled()) {
			logger.info("Thread Id: " + Thread.currentThread().getId());
			logger.info(method + " '" + uri + "' " + ", parameters: " + session.getQueryParameterString());
			logger.info("DecodedQueryParameters: \n" + decodedQueryParameters);
			logger.info("Headers: \n" + session.getHeaders());
			logger.info("Params: \n" + session.getParms());
		}

		String[] tokens = uri.split("/");

		if (tokens.length < 2) {
			return createErrorResponse("Invalid endpoint " + uri);

		}
		try {
			// entity
			if (tokens[1].equalsIgnoreCase(ENTITY_EP)) {
				// plant entities request
				response = servePlantEntityRequest();

			} else if (tokens[1].equalsIgnoreCase(REASON_EP)) {
				// reasons request
				response = serveReasonRequest();

			} else if (tokens[1].equalsIgnoreCase(MATERIAL_EP)) {
				// material request
				response = serveMaterialRequest();

			} else if (tokens[1].equalsIgnoreCase(EVENT_EP)) {
				// equipment event
				EquipmentEventResponseDto responseDto = serveEquipmentEvent(session);
				String json = gson.toJson(responseDto);
				response = newFixedLengthResponse(Response.Status.ACCEPTED, NanoHTTPD.MIME_PLAINTEXT, json);

			} else if (tokens[1].equalsIgnoreCase(SOURCE_ID_EP)) {
				// source id request
				response = serveSourceIdRequest(session);
			} else if (tokens[1].equalsIgnoreCase(DATA_SOURCE_EP)) {
				// data source request
				response = serveDataSourceRequest(session);

			} else {
				response = createErrorResponse("Unrecognized endpoint " + uri);
			}
		} catch (Exception e) {
			response = createErrorResponse(e.getMessage());
			logger.error(e.getMessage());
		}

		return response;
	}

	private Response createErrorResponse(String message) {
		EquipmentEventResponseDto responseDto = new EquipmentEventResponseDto(message);
		String json = gson.toJson(responseDto);
		return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, json);
	}

	private EquipmentEventResponseDto serveEquipmentEvent(IHTTPSession session) throws Exception {
		EquipmentEventResponseDto responseDto = new EquipmentEventResponseDto();
		EquipmentEventRequestDto dto = null;

		// data expected as HTTP body
		Map<String, String> bodyMap = new HashMap<String, String>();

		try {
			session.parseBody(bodyMap);
		} catch (Exception e) {
			responseDto.setErrorText(e.getMessage());
			return responseDto;
		}

		String body = bodyMap.get("postData");

		if (body != null && body.length() > 0) {
			if (logger.isInfoEnabled()) {
				logger.info("    Body: " + body);
			}

			// de-serialize JSON body
			dto = gson.fromJson(body, EquipmentEventRequestDto.class);
		} else {
			responseDto.setErrorText("Invalid event data.");
			return responseDto;
		}

		// entity data change event
		if (dto.getValue() == null || dto.getValue().length() == 0) {
			responseDto.setErrorText("The value must be specified.");
			return responseDto;
		}

		if (dto.getSourceId() == null || dto.getSourceId().length() == 0) {
			responseDto.setErrorText("The source id must be specified.");
			return responseDto;
		}

		if (!acceptingEventRequests) {
			responseDto.setErrorText("The server is not accepting event requests.");
			return responseDto;
		}

		if (eventListener == null) {
			responseDto.setErrorText("There is no listener for this event.");
			return responseDto;
		}

		// timestamp
		OffsetDateTime odt = DomainUtils.offsetDateTimeFromString(dto.getTimestamp());

		if (logger.isInfoEnabled()) {
			logger.info("Data change for source id: " + dto.getSourceId() + ", Value: " + dto.getValue()
					+ ", Timestamp: " + odt);
		}

		// call listener on same thread
		eventListener.onHttpEquipmentEvent(dto.getSourceId(), dto.getValue(), odt);

		return responseDto;
	}

	protected Map<String, String> getBodyParameters(IHTTPSession session) throws Exception {
		Map<String, String> bodyMap = new HashMap<String, String>();
		session.parseBody(bodyMap);
		String body = session.getQueryParameterString();

		if (body != null && body.length() > 0) {
			if (logger.isInfoEnabled()) {
				logger.info("    Body: " + body);
			}
		}
		return bodyMap;
	}

	@SuppressWarnings("deprecation")
	protected Map<String, String> getQueryStringParameters(IHTTPSession session) {
		Map<String, String> parameterMap = session.getParms();

		if (logger.isInfoEnabled()) {
			logger.info("    Parameters: [" + parameterMap.size() + "] " + parameterMap);
		}
		return parameterMap;
	}

	private Response serveSourceIdRequest(IHTTPSession session) throws Exception {
		Response response = null;
		try {
			Map<String, String> queryParameters = getQueryStringParameters(session);

			String equipmentName = queryParameters.get(OeeHttpServer.EQUIP_ATTRIB);
			if (equipmentName == null || equipmentName.length() == 0) {
				throw new Exception("The name of the equipment must be specified.");
			}

			String sourceType = queryParameters.get(OeeHttpServer.DS_TYPE_ATTRIB);
			if (sourceType == null || sourceType.length() == 0) {
				throw new Exception("The data source type must be specified.");
			}

			List<String> sourceIds = PersistenceService.instance().fetchResolverSourceIds(equipmentName,
					DataSourceType.valueOf(sourceType));

			String payload = gson.toJson(new SourceIdResponseDto(sourceIds));

			response = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, payload);
		} catch (Exception e) {
			logger.error("Source Id request failed.", e);
			response = newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
		}

		return response;
	}

	private Response serveDataSourceRequest(IHTTPSession session) throws Exception {
		Response response = null;
		try {
			Map<String, String> queryParameters = getQueryStringParameters(session);

			String sourceType = queryParameters.get(OeeHttpServer.DS_TYPE_ATTRIB);
			if (sourceType == null || sourceType.length() == 0) {
				throw new Exception("The data source type must be specified.");
			}

			List<CollectorDataSource> dataSources = PersistenceService.instance()
					.fetchDataSources(DataSourceType.valueOf(sourceType));

			List<DataSourceDto> dataSourceDtos = new ArrayList<>();
			for (CollectorDataSource dataSource : dataSources) {
				DataSourceDto dataSourceDto = new DataSourceDto(dataSource);
				dataSourceDtos.add(dataSourceDto);
			}

			String payload = gson.toJson(new DataSourceResponseDto(dataSourceDtos));

			response = newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, payload);
		} catch (Exception e) {
			logger.error("Data source request failed.", e);
			response = newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
		}

		return response;
	}

	private Response serveMaterialRequest() throws Exception {
		List<MaterialDto> materialDtos = new ArrayList<>();

		List<Material> allMaterial = PersistenceService.instance().fetchAllMaterials();

		for (Material material : allMaterial) {
			MaterialDto dto = new MaterialDto(material.getName(), material.getDescription(), material.getCategory());
			materialDtos.add(dto);
		}

		String payload = gson.toJson(new MaterialResponseDto(materialDtos));

		if (logger.isInfoEnabled()) {
			logger.info(payload);
		}

		return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, payload);
	}

	private Response serveReasonRequest() throws Exception {
		Map<String, ReasonDto> reasonMap = new HashMap<>();

		List<ReasonDto> topDtos = new ArrayList<>();

		List<Reason> allReasons = PersistenceService.instance().fetchAllReasons();

		for (Reason reason : allReasons) {
			// this reason
			TimeLoss timeLoss = reason.getLossCategory();

			String lossName = null;
			if (timeLoss != null) {
				lossName = timeLoss.name();
			}
			ReasonDto reasonDto = new ReasonDto(reason.getName(), reason.getDescription(), lossName);

			// parent entity
			if (reason.getParent() == null) {
				topDtos.add(reasonDto);
			}

			// put in map
			reasonMap.put(reasonDto.getName(), reasonDto);
		}

		// set children
		for (Reason reason : allReasons) {
			ReasonDto parentDto = reasonMap.get(reason.getName());

			for (Reason childReason : reason.getChildren()) {
				ReasonDto childDto = reasonMap.get(childReason.getName());

				childDto.setParent(parentDto.getName());
				parentDto.getChildren().add(childDto);
			}
		}

		// JSON payload
		String payload = gson.toJson(new ReasonResponseDto(topDtos));

		if (logger.isInfoEnabled()) {
			logger.info(payload);
		}

		return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, payload);
	}

	private Response servePlantEntityRequest() throws Exception {
		Map<String, PlantEntityDto> entityMap = new HashMap<>();

		List<PlantEntityDto> topDtos = new ArrayList<>();

		List<PlantEntity> allEntities = PersistenceService.instance().fetchAllPlantEntities();

		for (PlantEntity entity : allEntities) {
			// this entity
			PlantEntityDto entityDto = new PlantEntityDto(entity.getName(), entity.getDescription(),
					entity.getLevel().toString());

			// parent entity
			if (entity.getParent() == null) {
				topDtos.add(entityDto);
			}

			// put in map
			entityMap.put(entityDto.getName(), entityDto);
		}

		// set children
		for (PlantEntity entity : allEntities) {
			PlantEntityDto parentDto = entityMap.get(entity.getName());

			for (PlantEntity childEntity : entity.getChildren()) {
				PlantEntityDto childDto = entityMap.get(childEntity.getName());

				childDto.setParent(parentDto.getName());
				parentDto.getChildren().add(childDto);
			}
		}

		// JSON payload
		String payload = gson.toJson(new PlantEntityResponseDto(topDtos));

		if (logger.isInfoEnabled()) {
			logger.info(payload);
		}

		return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, payload);
	}

	public HttpEventListener getDataChangeListener() {
		return eventListener;
	}

	public void setDataChangeListener(HttpEventListener dataChangeListener) {
		this.eventListener = dataChangeListener;
	}

	public boolean isAcceptingEventRequests() {
		return acceptingEventRequests;
	}

	public void setAcceptingEventRequests(boolean acceptingEventRequests) {
		this.acceptingEventRequests = acceptingEventRequests;
	}
}
