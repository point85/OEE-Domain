package org.point85.domain.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.dto.DataSourceDto;
import org.point85.domain.dto.DataSourceResponseDto;
import org.point85.domain.dto.EquipmentEventRequestDto;
import org.point85.domain.dto.EquipmentEventResponseDto;
import org.point85.domain.dto.EquipmentStatusResponseDto;
import org.point85.domain.dto.MaterialDto;
import org.point85.domain.dto.MaterialResponseDto;
import org.point85.domain.dto.OeeEventDto;
import org.point85.domain.dto.OeeEventsResponseDto;
import org.point85.domain.dto.OeeResponseDto;
import org.point85.domain.dto.PlantEntityResponseDto;
import org.point85.domain.dto.ReasonDto;
import org.point85.domain.dto.ReasonResponseDto;
import org.point85.domain.dto.SourceIdResponseDto;
import org.point85.domain.exim.ExportContent;
import org.point85.domain.exim.Exporter;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.oee.EquipmentLoss;
import org.point85.domain.oee.EquipmentLossManager;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Jetty servlet class to handle GET, PUT and POST requests
 *
 */
class OeeHttpServlet extends HttpServlet {
	private static final long serialVersionUID = 3631737621490601680L;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeHttpServlet.class);

	// JSON parser
	private final Gson gson = new Gson();

	// flag for accepting event requests
	private static boolean acceptingEventRequests = true;

	// event listener
	private static HttpEventListener eventListener;

	OeeHttpServlet() {
	}

	HttpEventListener getDataChangeListener() {
		return eventListener;
	}

	static void setDataChangeListener(HttpEventListener dataChangeListener) {
		eventListener = dataChangeListener;
	}

	boolean isAcceptingEventRequests() {
		return acceptingEventRequests;
	}

	static void setAcceptingEventRequests(boolean flag) {
		acceptingEventRequests = flag;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serveRequest(request, response);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serveRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serveRequest(request, response);
	}

	private void serveRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String path = request.getServletPath();

			String[] tokens = path.split("/");

			if (tokens.length < 2) {
				throw new Exception(DomainLocalizer.instance().getErrorString("unrecognized.endpoint", path));
			}

			// response content
			String content = null;

			// entity
			if (tokens[1].equalsIgnoreCase(OeeHttpServer.ENTITY_EP)) {
				// plant entities request
				content = servePlantEntityRequest();

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.REASON_EP)) {
				// reasons request
				content = serveReasonRequest();

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.MATERIAL_EP)) {
				// material request
				content = serveMaterialRequest();

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.EVENT_EP)) {
				// equipment event
				// data is either x-www-form-urlencoded as a parameter or in the
				// request body
				Map<String, String[]> parameters = request.getParameterMap();

				String body = null;
				if (parameters.containsKey(OeeHttpServer.EVENT_KEY)) {
					// form encoded as a parameter
					body = request.getParameter(OeeHttpServer.EVENT_KEY);
				} else {
					// in the request body
					try (BufferedReader br = request.getReader()) {
						body = br.lines().collect(Collectors.joining(System.lineSeparator()));
					}
				}

				if (body == null || body.length() == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("invalid.event.data"));
				}

				if (logger.isInfoEnabled()) {
					logger.info("    Body: " + body);
				}

				content = serveEquipmentEvent(body);

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.SOURCE_ID_EP)) {
				// source id request
				Map<String, String[]> queryParameters = request.getParameterMap();

				String[] equipmentNames = queryParameters.get(OeeHttpServer.EQUIP_ATTRIB);
				if (equipmentNames == null || equipmentNames.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}

				String[] sourceTypes = queryParameters.get(OeeHttpServer.DS_TYPE_ATTRIB);
				if (sourceTypes == null || sourceTypes.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.data.source"));
				}

				content = serveSourceIdRequest(equipmentNames[0], sourceTypes[0]);

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.DATA_SOURCE_EP)) {
				// data source request
				Map<String, String[]> queryParameters = request.getParameterMap();

				String[] sourceTypes = queryParameters.get(OeeHttpServer.DS_TYPE_ATTRIB);
				if (sourceTypes == null || sourceTypes.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.data.source"));
				}

				content = serveDataSourceRequest(sourceTypes[0]);

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.STATUS_EP)) {
				// equipment status request
				Map<String, String[]> queryParameters = request.getParameterMap();

				String[] equipmentNames = queryParameters.get(OeeHttpServer.EQUIP_ATTRIB);
				if (equipmentNames == null || equipmentNames.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}

				content = serveEquipmentStatusRequest(equipmentNames[0]);

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.EVENTS_EP)) {
				// OEE events request
				Map<String, String[]> queryParameters = request.getParameterMap();

				// equipment is required
				String[] equipmentNames = queryParameters.get(OeeHttpServer.EQUIP_ATTRIB);

				if (equipmentNames == null || equipmentNames.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}

				// material can be null
				String materialId = null;
				String[] materialIds = queryParameters.get(OeeHttpServer.MATERIAL_ATTRIB);

				if (materialIds != null && materialIds.length > 0) {
					materialId = materialIds[0];
				}

				// event type can be null
				String eventType = null;
				String[] eventTypes = queryParameters.get(OeeHttpServer.EVENT_TYPE_ATTRIB);

				if (eventTypes != null && eventTypes.length > 0) {
					eventType = eventTypes[0];
				}

				// from timestamp can be null
				String fromTime = null;

				String[] fromTimes = queryParameters.get(OeeHttpServer.FROM_ATTRIB);

				if (fromTimes != null && fromTimes.length > 0) {
					fromTime = fromTimes[0];
				}

				// to timestamp can be null
				String toTime = null;

				String[] toTimes = queryParameters.get(OeeHttpServer.TO_ATTRIB);

				if (toTimes != null && toTimes.length > 0) {
					toTime = toTimes[0];
				}

				// server the request
				content = serveEventsRequest(equipmentNames[0], materialId, eventType, fromTime, toTime);

			} else if (tokens[1].equalsIgnoreCase(OeeHttpServer.OEE_EP)) {
				// OEE calculation request
				Map<String, String[]> queryParameters = request.getParameterMap();

				// equipment is required
				String[] equipmentNames = queryParameters.get(OeeHttpServer.EQUIP_ATTRIB);

				if (equipmentNames == null || equipmentNames.length == 0) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}

				// material can be null
				String materialId = null;
				String[] materialIds = queryParameters.get(OeeHttpServer.MATERIAL_ATTRIB);

				if (materialIds != null && materialIds.length > 0) {
					materialId = materialIds[0];
				}

				// from timestamp can be null
				String fromTime = null;

				String[] fromTimes = queryParameters.get(OeeHttpServer.FROM_ATTRIB);

				if (fromTimes != null && fromTimes.length > 0) {
					fromTime = fromTimes[0];
				}

				// to timestamp can be null
				String toTime = null;

				String[] toTimes = queryParameters.get(OeeHttpServer.TO_ATTRIB);

				if (toTimes != null && toTimes.length > 0) {
					toTime = toTimes[0];
				}

				// server the request
				content = serveOeeRequest(equipmentNames[0], materialId, fromTime, toTime);

			} else if (tokens[1].equalsIgnoreCase("favicon.ico")) {
				// ignore icon
				response.setStatus(HttpServletResponse.SC_OK);
				return;

			} else {
				throw new Exception(DomainLocalizer.instance().getErrorString("unrecognized.endpoint", path));
			}

			if (logger.isInfoEnabled()) {
				logger.info("Response: " + content);
			}

			// return data with OK response
			setResponseContent(response, content);

		} catch (Exception e) {
			logger.error(e.getMessage());

			try {
				createErrorResponse(response, e.getMessage());
			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
	}

	private void createErrorResponse(HttpServletResponse response, String message) throws Exception {
		EquipmentEventResponseDto responseDto = new EquipmentEventResponseDto(message);
		String payload = gson.toJson(responseDto);
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.getWriter().write(payload);
		response.getWriter().flush();
		response.getWriter().close();
	}

	private void setResponseContent(HttpServletResponse response, String responseJson) throws Exception {
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(responseJson);
		response.getWriter().flush();
		response.getWriter().close();
	}

	// handle request for source ids
	private String serveSourceIdRequest(String equipmentName, String sourceType) throws Exception {
		List<String> sourceIds = PersistenceService.instance().fetchResolverSourceIds(equipmentName,
				DataSourceType.valueOf(sourceType));

		return gson.toJson(new SourceIdResponseDto(sourceIds));
	}

	// handle request for equipment status
	private String serveEquipmentStatusRequest(String equipmentName) throws Exception {
		// get the equipment
		Equipment equipment = PersistenceService.instance().fetchEquipmentByName(equipmentName);

		if (equipment == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment", equipmentName));
		}

		// get last setup
		OeeEvent lastSetup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

		EquipmentStatusResponseDto responseDto = new EquipmentStatusResponseDto();

		if (lastSetup != null) {
			// material and job
			Material material = lastSetup.getMaterial();

			MaterialDto materialDto = new MaterialDto(material.getName(), material.getDescription(),
					material.getCategory());
			responseDto.setMaterial(materialDto);

			String job = lastSetup.getJob();
			responseDto.setJob(job);

			// production specs
			EquipmentMaterial equipmentMaterial = equipment.getEquipmentMaterial(material);

			if (equipmentMaterial == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.material", equipmentName,
						material.getName()));
			}

			// reject UOM
			responseDto.setRejectUOM(equipmentMaterial.getRejectUOM().getDisplayString());

			// get the numerator of the run rate
			responseDto.setRunRateUOM(equipmentMaterial.getRunRateUOM().getDividend().getDisplayString());
		}

		// last availability
		OeeEvent lastAvailability = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.AVAILABILITY);

		if (lastAvailability != null) {
			// reason
			Reason reason = lastAvailability.getReason();

			if (reason != null) {
				ReasonDto reasonDto = new ReasonDto(reason.getName(), reason.getDescription(),
						reason.getLossCategory().name());
				responseDto.setReason(reasonDto);
			}
		}

		return gson.toJson(responseDto);
	}

	// handle request for data source
	private String serveDataSourceRequest(String sourceType) throws Exception {
		List<CollectorDataSource> dataSources = PersistenceService.instance()
				.fetchDataSources(DataSourceType.valueOf(sourceType));

		List<DataSourceDto> dataSourceDtos = new ArrayList<>();
		for (CollectorDataSource dataSource : dataSources) {
			DataSourceDto dataSourceDto = new DataSourceDto(dataSource);
			dataSourceDtos.add(dataSourceDto);
		}

		return gson.toJson(new DataSourceResponseDto(dataSourceDtos));
	}

	// handle request for materials
	private String serveMaterialRequest() throws Exception {
		List<MaterialDto> materialDtos = new ArrayList<>();

		List<Material> allMaterial = PersistenceService.instance().fetchAllMaterials();

		for (Material material : allMaterial) {
			MaterialDto dto = new MaterialDto(material.getName(), material.getDescription(), material.getCategory());
			materialDtos.add(dto);
		}

		return gson.toJson(new MaterialResponseDto(materialDtos));
	}

	// handle request for OEE events
	private String serveEventsRequest(String equipmentName, String materialId, String eventType, String fromTimestamp,
			String toTimestamp) throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info("Request - equipment: " + equipmentName + ", material: " + materialId + ", type: " + eventType
					+ ", from: " + fromTimestamp + ", to: " + toTimestamp);
		}

		// equipment
		Equipment equipment = PersistenceService.instance().fetchEquipmentByName(equipmentName);

		// material
		Material material = (materialId != null) ? PersistenceService.instance().fetchMaterialByName(materialId) : null;

		// event type
		OeeEventType type = (eventType != null) ? OeeEventType.deserialize(eventType) : null;

		// from time can be offset or local
		OffsetDateTime fromODT = null;

		if (fromTimestamp != null) {
			if (fromTimestamp.length() > DomainUtils.LOCAL_DATE_TIME_8601.length()) {
				fromODT = DomainUtils.offsetDateTimeFromString(fromTimestamp, DomainUtils.OFFSET_DATE_TIME_8601);
			} else {
				LocalDateTime ldt = DomainUtils.localDateTimeFromString(fromTimestamp,
						DomainUtils.LOCAL_DATE_TIME_8601);
				fromODT = DomainUtils.fromLocalDateTime(ldt);
			}
		}

		// to time can be offset or local
		OffsetDateTime toODT = null;

		if (toTimestamp != null) {
			if (toTimestamp.length() > DomainUtils.LOCAL_DATE_TIME_8601.length()) {
				toODT = DomainUtils.offsetDateTimeFromString(toTimestamp, DomainUtils.OFFSET_DATE_TIME_8601);
			} else {
				LocalDateTime ldt = DomainUtils.localDateTimeFromString(toTimestamp, DomainUtils.LOCAL_DATE_TIME_8601);
				toODT = DomainUtils.fromLocalDateTime(ldt);
			}
		}

		if (toODT != null && fromODT != null && toODT.isBefore(fromODT)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("start.before.end", fromODT, toODT));
		}

		List<OeeEvent> events = PersistenceService.instance().fetchEvents(equipment, material, type, fromODT, toODT);

		List<OeeEventDto> eventDtos = new ArrayList<>();

		for (OeeEvent event : events) {
			OeeEventDto dto = new OeeEventDto(event);
			eventDtos.add(dto);
		}

		return gson.toJson(new OeeEventsResponseDto(eventDtos));
	}

	// handle request for OEE calculations
	private String serveOeeRequest(String equipmentName, String materialId, String fromTimestamp, String toTimestamp)
			throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info("Request - equipment: " + equipmentName + ", material: " + materialId + ", from: "
					+ fromTimestamp + ", to: " + toTimestamp);
		}

		// equipment
		Equipment equipment = PersistenceService.instance().fetchEquipmentByName(equipmentName);

		// from time can be offset or local
		OffsetDateTime fromODT = null;

		if (fromTimestamp != null) {
			if (fromTimestamp.length() > DomainUtils.LOCAL_DATE_TIME_8601.length()) {
				fromODT = DomainUtils.offsetDateTimeFromString(fromTimestamp, DomainUtils.OFFSET_DATE_TIME_8601);
			} else {
				LocalDateTime ldt = DomainUtils.localDateTimeFromString(fromTimestamp,
						DomainUtils.LOCAL_DATE_TIME_8601);
				fromODT = DomainUtils.fromLocalDateTime(ldt);
			}
		} else {
			// set a default
			LocalDateTime ldtStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
			fromODT = DomainUtils.fromLocalDateTime(ldtStart);
		}

		// to time can be offset or local
		OffsetDateTime toODT = null;

		if (toTimestamp != null) {
			if (toTimestamp.length() > DomainUtils.LOCAL_DATE_TIME_8601.length()) {
				toODT = DomainUtils.offsetDateTimeFromString(toTimestamp, DomainUtils.OFFSET_DATE_TIME_8601);
			} else {
				LocalDateTime ldt = DomainUtils.localDateTimeFromString(toTimestamp, DomainUtils.LOCAL_DATE_TIME_8601);
				toODT = DomainUtils.fromLocalDateTime(ldt);
			}
		} else {
			// set a default
			LocalDateTime ldtEnd = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
			toODT = DomainUtils.fromLocalDateTime(ldtEnd);
		}

		if (toODT != null && fromODT != null && toODT.isBefore(fromODT)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("start.before.end", fromODT, toODT));
		}

		// populate the equipment loss data
		EquipmentLoss equipmentLoss = new EquipmentLoss(equipment);

		// do the calculations
		EquipmentLossManager.buildLoss(equipmentLoss, materialId, fromODT, toODT);

		return gson.toJson(new OeeResponseDto(equipmentLoss));
	}

	// handle request for reasons
	private String serveReasonRequest() throws Exception {
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
		return gson.toJson(new ReasonResponseDto(topDtos));
	}

	// handle request for plant entities
	private String servePlantEntityRequest() throws Exception {
		ExportContent content = Exporter.instance().prepare(PlantEntity.class);

		// JSON payload
		PlantEntityResponseDto response = new PlantEntityResponseDto(content);

		return gson.toJson(response);
	}

	// handle equipment event
	private synchronized String serveEquipmentEvent(String body) throws Exception {
		EquipmentEventRequestDto requestDto = gson.fromJson(body, EquipmentEventRequestDto.class);
		String errorText = null;

		// entity data change event
		if (requestDto.getValue() == null || requestDto.getValue().length() == 0) {
			OeeEventType eventType = OeeEventType.valueOf(requestDto.getEventType());
			switch (eventType) {
			case AVAILABILITY:
				errorText = DomainLocalizer.instance().getErrorString("missing.reason");
				break;

			case JOB_CHANGE:
				if (requestDto.getJob() == null || requestDto.getJob().trim().length() == 0) {
					errorText = DomainLocalizer.instance().getErrorString("missing.job");
				}
				break;

			case MATL_CHANGE:
				errorText = DomainLocalizer.instance().getErrorString("missing.material");
				break;

			case PROD_GOOD:
			case PROD_REJECT:
			case PROD_STARTUP:
				errorText = DomainLocalizer.instance().getErrorString("missing.amount");
				break;

			case CUSTOM:
			default:
				break;
			}
		}

		// need source id or equipment name
		if (requestDto.getSourceId() == null || requestDto.getSourceId().length() == 0) {
			if (requestDto.getEquipmentName() == null || requestDto.getEquipmentName().length() == 0) {
				errorText = DomainLocalizer.instance().getErrorString("missing.source");
			}
		}

		if (!acceptingEventRequests) {
			errorText = DomainLocalizer.instance().getErrorString("no.requests");
		}

		if (eventListener == null) {
			errorText = DomainLocalizer.instance().getErrorString("missing.listener");
		}

		if (errorText != null) {
			throw new Exception(errorText);
		}

		// call listener for processing
		eventListener.onHttpEquipmentEvent(requestDto);

		EquipmentEventResponseDto responseDto = new EquipmentEventResponseDto();

		return gson.toJson(responseDto);
	}
}
