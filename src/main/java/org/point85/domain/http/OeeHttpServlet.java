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
import org.point85.domain.dto.AreaDto;
import org.point85.domain.dto.DataSourceDto;
import org.point85.domain.dto.DataSourceResponseDto;
import org.point85.domain.dto.EnterpriseDto;
import org.point85.domain.dto.EquipmentDto;
import org.point85.domain.dto.EquipmentEventRequestDto;
import org.point85.domain.dto.EquipmentEventResponseDto;
import org.point85.domain.dto.EquipmentStatusResponseDto;
import org.point85.domain.dto.MaterialDto;
import org.point85.domain.dto.MaterialResponseDto;
import org.point85.domain.dto.OeeEventDto;
import org.point85.domain.dto.OeeEventsResponseDto;
import org.point85.domain.dto.OeeResponseDto;
import org.point85.domain.dto.PlantEntityDto;
import org.point85.domain.dto.PlantEntityResponseDto;
import org.point85.domain.dto.ProductionLineDto;
import org.point85.domain.dto.ReasonDto;
import org.point85.domain.dto.ReasonResponseDto;
import org.point85.domain.dto.SiteDto;
import org.point85.domain.dto.SourceIdResponseDto;
import org.point85.domain.dto.WorkCellDto;
import org.point85.domain.exim.ExportImportContent;
import org.point85.domain.exim.Exporter;
import org.point85.domain.exim.Importer;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.oee.EquipmentLoss;
import org.point85.domain.oee.EquipmentLossManager;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
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

	// URL parts
	private static final int IDX_REST = 1;
	private static final int IDX_RESOURCE = IDX_REST + 1;
	private static final int IDX_ID = IDX_RESOURCE + 1;

	// verbs
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String PUT = "PUT";
	private static final String DELETE = "DELETE";

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

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		serveRequest(request, response);
	}

	protected Map<String, String[]> getQueryParameters(HttpServletRequest request) {
		return request.getParameterMap();
	}

	private String getID(String[] tokens) throws Exception {
		if (tokens.length < IDX_ID + 1) {
			throw new Exception(DomainLocalizer.instance().getErrorString("invalid.event.data"));
		}

		return tokens[IDX_ID];
	}

	private String getRequestBody(HttpServletRequest request) throws Exception {
		BufferedReader reader = request.getReader();
		String line;

		StringBuilder sb = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		return sb.toString();
	}

	private void checkForNull(KeyedObject keyed, String name) throws Exception {
		if (keyed == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.resource", name));
		}
	}

	// handle request for materials
	private String serveRestMaterialRequest(String[] tokens, String verb, String requestBody) throws Exception {
		String json = null;

		switch (verb) {
		case DELETE: {
			String name = getID(tokens);

			// delete specified material
			Material existing = PersistenceService.instance().fetchMaterialByName(name);
			checkForNull(existing, name);

			json = gson.toJson(new MaterialDto(existing));

			PersistenceService.instance().delete(existing);

			break;
		}

		case GET: {
			if (tokens.length > IDX_RESOURCE + 1) {
				String name = getID(tokens);

				// one material
				Material material = PersistenceService.instance().fetchMaterialByName(name);
				checkForNull(material, name);

				json = gson.toJson(new MaterialDto(material));
			} else {
				// all materials
				List<Material> materials = PersistenceService.instance().fetchAllMaterials();

				List<MaterialDto> materialDtos = new ArrayList<>();
				for (Material material : materials) {
					materialDtos.add(new MaterialDto(material));
				}

				json = gson.toJson(materialDtos);
			}

			break;
		}

		case POST: {
			// create material, content is JSON in the request body with name
			if (logger.isInfoEnabled()) {
				logger.info("Create material: " + requestBody);
			}

			// call into the Importer
			MaterialDto dto = gson.fromJson(requestBody, MaterialDto.class);

			if (dto.getName() == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.resource.name"));
			}

			ExportImportContent content = new ExportImportContent(true);
			content.getMaterials().add(dto);

			List<KeyedObject> savedMaterials = Importer.instance().restoreMaterials(content);

			json = gson.toJson(new MaterialDto((Material) savedMaterials.get(0)));

			break;
		}

		case PUT: {
			// update material, content is JSON in the request body with name
			if (logger.isInfoEnabled()) {
				logger.info("Update material: " + requestBody);
			}

			// call into the Importer
			MaterialDto dto = gson.fromJson(requestBody, MaterialDto.class);

			// maybe a new name in body
			String name = getID(tokens);
			String newName = dto.getName();

			// existing name is in URL
			dto.setName(name);

			ExportImportContent content = new ExportImportContent(false);
			content.getMaterials().add(dto);

			List<KeyedObject> savedMaterials = Importer.instance().restoreMaterials(content);
			Material material = (Material) savedMaterials.get(0);

			if (newName != null && !newName.equals(name)) {
				// change name
				material = PersistenceService.instance().fetchMaterialByName(name);
				checkForNull(material, name);

				material.setName(newName);
				PersistenceService.instance().save(material);
			}

			json = gson.toJson(new MaterialDto(material));

			break;
		}

		default:
			break;
		}

		return json;
	}

	private String restorePlantEntities(String requestBody, String name, Boolean forCreate) throws Exception {
		ExportImportContent content = new ExportImportContent(forCreate);
		String json = null;

		// find level
		PlantEntityDto entityDto = gson.fromJson(requestBody, PlantEntityDto.class);

		if (entityDto.getLevel() == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.entity.level"));
		}
		EntityLevel level = EntityLevel.valueOf(entityDto.getLevel());

		// maybe a new name in body
		String newName = entityDto.getName();

		if (name == null) {
			// from a POST
			name = newName;
		}

		switch (level) {
		case AREA: {
			AreaDto dto = gson.fromJson(requestBody, AreaDto.class);
			dto.setName(name);

			content.getAreas().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			Area area = (Area) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				area = (Area) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(area, name);

				area.setName(newName);
				PersistenceService.instance().save(area);
			}
			json = gson.toJson(new AreaDto(area));
			break;
		}
		case ENTERPRISE: {
			EnterpriseDto dto = gson.fromJson(requestBody, EnterpriseDto.class);
			dto.setName(name);

			content.getEnterprises().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			Enterprise enterprise = (Enterprise) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				enterprise = (Enterprise) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(enterprise, name);

				enterprise.setName(newName);
				PersistenceService.instance().save(enterprise);
			}
			json = gson.toJson(new EnterpriseDto(enterprise));
			break;
		}
		case EQUIPMENT: {
			EquipmentDto dto = gson.fromJson(requestBody, EquipmentDto.class);
			dto.setName(name);

			content.getEquipment().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			Equipment equipment = (Equipment) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				equipment = (Equipment) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(equipment, name);

				equipment.setName(newName);
				PersistenceService.instance().save(equipment);
			}
			json = gson.toJson(new EquipmentDto(equipment));
			break;
		}
		case PRODUCTION_LINE: {
			ProductionLineDto dto = gson.fromJson(requestBody, ProductionLineDto.class);
			dto.setName(name);

			content.getProductionLines().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			ProductionLine line = (ProductionLine) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				line = (ProductionLine) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(line, name);

				line.setName(newName);
				PersistenceService.instance().save(line);
			}
			json = gson.toJson(new ProductionLineDto(line));
			break;
		}
		case SITE: {
			SiteDto dto = gson.fromJson(requestBody, SiteDto.class);
			dto.setName(name);

			content.getSites().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			Site site = (Site) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				site = (Site) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(site, name);

				site.setName(newName);
				PersistenceService.instance().save(site);
			}
			json = gson.toJson(new SiteDto(site));
			break;
		}
		case WORK_CELL: {
			WorkCellDto dto = gson.fromJson(requestBody, WorkCellDto.class);
			dto.setName(name);

			content.getWorkCells().add(dto);
			List<KeyedObject> savedEntities = Importer.instance().restorePlantEntities(content);

			WorkCell cell = (WorkCell) savedEntities.get(0);

			// maybe a new name in body
			if (newName != null && !newName.equals(name)) {
				// change name
				cell = (WorkCell) PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(cell, name);

				cell.setName(newName);
				PersistenceService.instance().save(cell);
			}
			json = gson.toJson(new WorkCellDto(cell));
			break;
		}
		default:
			break;
		}

		return json;
	}

	private PlantEntityDto createPlantEntityDto(PlantEntity entity) {
		PlantEntityDto dto = null;

		switch (entity.getLevel()) {
		case AREA:
			dto = new AreaDto((Area) entity);
			break;
		case ENTERPRISE:
			dto = new EnterpriseDto((Enterprise) entity);
			break;
		case EQUIPMENT:
			dto = new EquipmentDto((Equipment) entity);
			break;
		case PRODUCTION_LINE:
			dto = new ProductionLineDto((ProductionLine) entity);
			break;
		case SITE:
			dto = new SiteDto((Site) entity);
			break;
		case WORK_CELL:
			dto = new WorkCellDto((WorkCell) entity);
			break;
		default:
			break;
		}
		return dto;
	}

	// handle request for plant entities
	private String serveRestEntityRequest(String[] tokens, String verb, String requestBody) throws Exception {
		String json = null;

		switch (verb) {
		case GET: {
			if (tokens.length > IDX_RESOURCE + 1) {
				String name = getID(tokens);

				// one entity
				PlantEntity entity = PersistenceService.instance().fetchPlantEntityByName(name);
				checkForNull(entity, name);

				json = gson.toJson(createPlantEntityDto(entity));

			} else {
				// all entities
				List<PlantEntity> entities = PersistenceService.instance().fetchAllPlantEntities();

				List<PlantEntityDto> entityDtos = new ArrayList<>();
				for (PlantEntity entity : entities) {
					entityDtos.add(createPlantEntityDto(entity));
				}

				json = gson.toJson(entityDtos);
			}
			break;
		}

		case DELETE: {
			String name = getID(tokens);

			// delete specified entity
			PlantEntity existing = PersistenceService.instance().fetchPlantEntityByName(name);
			checkForNull(existing, name);

			switch (existing.getLevel()) {
			case AREA:
				json = gson.toJson(new AreaDto((Area) existing));
				break;
			case ENTERPRISE:
				json = gson.toJson(new EnterpriseDto((Enterprise) existing));
				break;
			case EQUIPMENT:
				json = gson.toJson(new EquipmentDto((Equipment) existing));
				break;
			case PRODUCTION_LINE:
				json = gson.toJson(new ProductionLineDto((ProductionLine) existing));
				break;
			case SITE:
				json = gson.toJson(new SiteDto((Site) existing));
				break;
			case WORK_CELL:
				json = gson.toJson(new WorkCellDto((WorkCell) existing));
				break;
			default:
				break;
			}

			PlantEntity parentEntity = existing.getParent();
			if (parentEntity != null) {
				// remove from parent with orphan removal
				parentEntity.removeChild(existing);
				PersistenceService.instance().save(parentEntity);
			} else {
				// cascade delete
				PersistenceService.instance().delete(existing);
			}
			break;
		}

		case POST: {
			// create entity, content is JSON in the request body with name
			if (logger.isInfoEnabled()) {
				logger.info("Create plant entity: " + requestBody);
			}

			json = restorePlantEntities(requestBody, null, true);
			break;
		}

		case PUT: {
			if (logger.isInfoEnabled()) {
				logger.info("Update plant entity: " + requestBody);
			}

			json = restorePlantEntities(requestBody, getID(tokens), false);
			break;
		}

		default:
			break;
		}

		return json;
	}

	// handle request for reasons
	private String serveRestReasonRequest(String[] tokens, String verb, String requestBody) throws Exception {
		String json = null;

		switch (verb) {
		case GET: {
			if (tokens.length > IDX_RESOURCE + 1) {
				String name = getID(tokens);

				// one reason
				Reason reason = PersistenceService.instance().fetchReasonByName(name);
				checkForNull(reason, name);

				json = gson.toJson(new ReasonDto(reason));
			} else {
				// all reasons
				List<Reason> reasons = PersistenceService.instance().fetchAllReasons();

				List<ReasonDto> reasonDtos = new ArrayList<>();
				for (Reason reason : reasons) {
					reasonDtos.add(new ReasonDto(reason));
				}

				json = gson.toJson(reasonDtos);
			}
			break;
		}

		case DELETE: {
			String name = getID(tokens);

			// delete specified reason
			Reason existing = PersistenceService.instance().fetchReasonByName(name);
			checkForNull(existing, name);

			json = gson.toJson(new ReasonDto(existing));

			Reason parentReason = existing.getParent();
			if (parentReason != null) {
				// remove from parent with orphan removal
				parentReason.removeChild(existing);
				PersistenceService.instance().save(parentReason);
			} else {
				// cascade delete
				PersistenceService.instance().delete(existing);
			}

			break;
		}

		case POST: {
			// create reason, content is JSON in the request body with name
			if (logger.isInfoEnabled()) {
				logger.info("Create reason: " + requestBody);
			}

			// call into the Importer
			ReasonDto dto = gson.fromJson(requestBody, ReasonDto.class);

			if (dto.getName() == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.resource.name"));
			}

			ExportImportContent content = new ExportImportContent(true);
			content.getReasons().add(dto);
			List<KeyedObject> savedReasons = Importer.instance().restoreReasons(content);

			json = gson.toJson(new ReasonDto((Reason) savedReasons.get(0)));

			break;
		}

		case PUT: {
			// update reason, content is JSON in the request body with name
			if (logger.isInfoEnabled()) {
				logger.info("Update reason: " + requestBody);
			}

			// call into the Importer
			ReasonDto dto = gson.fromJson(requestBody, ReasonDto.class);

			// maybe a new name in body
			String name = getID(tokens);
			String newName = dto.getName();

			// existing name is in URL
			dto.setName(name);

			ExportImportContent content = new ExportImportContent(false);
			content.getReasons().add(dto);

			List<KeyedObject> savedReasons = Importer.instance().restoreReasons(content);
			Reason reason = (Reason) savedReasons.get(0);

			if (newName != null && !newName.equals(name)) {
				// change name
				reason = PersistenceService.instance().fetchReasonByName(name);
				checkForNull(reason, name);

				reason.setName(newName);
				PersistenceService.instance().save(reason);
			}

			json = gson.toJson(new ReasonDto(reason));
			break;
		}

		default:
			break;
		}

		return json;
	}

	private void serveRestRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String verb = request.getMethod();

			String path = request.getServletPath();

			if (logger.isInfoEnabled()) {
				logger.debug("Serving " + verb + " request on path " + path);
			}

			String[] tokens = path.split("/");

			if (tokens.length < IDX_RESOURCE || !tokens[1].equalsIgnoreCase(OeeHttpServer.REST_URL)) {
				throw new Exception(DomainLocalizer.instance().getErrorString("unrecognized.endpoint", path));
			}

			// response content
			String content = null;
			String requestBody = null;

			if (verb.equals(POST) || verb.equals(PUT)) {
				requestBody = getRequestBody(request);

				if (requestBody == null || requestBody.isEmpty()) {
					throw new Exception(DomainLocalizer.instance().getErrorString("invalid.event.data"));
				}

				if (logger.isInfoEnabled()) {
					logger.info("    Body: " + requestBody);
				}
			}

			// entities
			if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.ENTITIES_RESOURCE)) {
				// plant entities request
				content = serveRestEntityRequest(tokens, verb, requestBody);

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.REASONS_RESOURCE)) {
				// reasons request
				content = serveRestReasonRequest(tokens, verb, requestBody);

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.MATERIALS_RESOURCE)) {
				// material request
				content = serveRestMaterialRequest(tokens, verb, requestBody);

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.EVENT_RESOURCE)) {
				// equipment event
				if (tokens.length != IDX_ID + 1) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}

				content = serveEquipmentEvent(requestBody, getID(tokens));

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.STATUS_RESOURCE)) {
				// equipment status request
				if (tokens.length != IDX_ID + 1) {
					throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.name"));
				}
				String name = getID(tokens);

				// id
				Equipment equipment = PersistenceService.instance().fetchEquipmentByName(name);
				checkForNull(equipment, name);

				content = serveEquipmentStatusRequest(equipment);

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase(OeeHttpServer.EVENT_RESOURCE)) {
				// OEE event request
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

				// serve the request
				content = serveEventsRequest(equipmentNames[0], materialId, eventType, fromTime, toTime);

			} else if (tokens[IDX_RESOURCE].equalsIgnoreCase("favicon.ico")) {
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

	private void serveRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String path = request.getServletPath();

			// check for REST
			if (path.contains(OeeHttpServer.REST_URL)) {
				serveRestRequest(request, response);
				return;
			}

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

				content = serveEquipmentEvent(body, null);

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

				Equipment existing = PersistenceService.instance().fetchEquipmentByName(equipmentNames[0]);
				checkForNull(existing, equipmentNames[0]);

				content = serveEquipmentStatusRequest(existing);

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
	private String serveEquipmentStatusRequest(Equipment equipment) throws Exception {
		// get last setup
		OeeEvent lastSetup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

		MaterialDto materialDto = null;
		String job = null;
		String rejectUOM = null;
		String runRateUOM = null;
		Reason reason = null;

		if (lastSetup != null) {
			// material and job
			Material material = lastSetup.getMaterial();

			materialDto = new MaterialDto(material.getName(), material.getDescription(), material.getCategory());

			job = lastSetup.getJob();

			// production specs
			EquipmentMaterial equipmentMaterial = equipment.getEquipmentMaterial(material);

			if (equipmentMaterial == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment.material",
						equipment.getName(), material.getName()));
			}
			rejectUOM = equipmentMaterial.getRejectUOM().getDisplayString();
			runRateUOM = equipmentMaterial.getRunRateUOM().getDividend().getDisplayString();
		}

		// last availability
		OeeEvent lastAvailability = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.AVAILABILITY);

		if (lastAvailability != null) {
			// reason
			reason = lastAvailability.getReason();
		}

		EquipmentStatusResponseDto responseDto = new EquipmentStatusResponseDto();
		responseDto.setMaterial(materialDto);
		responseDto.setJob(job);
		responseDto.setRejectUOM(rejectUOM);
		responseDto.setRunRateUOM(runRateUOM);

		if (reason != null) {
			ReasonDto reasonDto = new ReasonDto(reason);
			responseDto.setReason(reasonDto);
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
		ExportImportContent content = Exporter.instance().prepare(PlantEntity.class);

		// JSON payload
		PlantEntityResponseDto response = new PlantEntityResponseDto(content);

		return gson.toJson(response);
	}

	// handle equipment event
	private synchronized String serveEquipmentEvent(String body, String equipmentName) throws Exception {
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
		if (equipmentName != null) {
			requestDto.setEquipmentName(equipmentName);
		} else if (requestDto.getSourceId() == null || requestDto.getSourceId().length() == 0) {
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

		EquipmentEventResponseDto responseDto = new EquipmentEventResponseDto(requestDto);

		return gson.toJson(responseDto);
	}
}
