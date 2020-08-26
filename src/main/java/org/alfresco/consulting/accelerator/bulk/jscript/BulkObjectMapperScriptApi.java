package org.alfresco.consulting.accelerator.bulk.jscript;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCache;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperService;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

public class BulkObjectMapperScriptApi extends BaseScopableProcessorExtension {
	private static final Log logger = LogFactory.getLog(BulkObjectMapperScriptApi.class);
	BulkObjectMapperService bulkObjectMapperService;
	ServiceRegistry serviceRegistry;
	BackgroundCache backgroundCache;
	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	public void setBulkObjectMapperService(BulkObjectMapperService bulkObjectMapperService) {
		this.bulkObjectMapperService = bulkObjectMapperService;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setBackgroundCache(BackgroundCache backgroundCache) {
		this.backgroundCache = backgroundCache;
	}

	public void tryToAttachContent(ScriptNode node) {
		bulkObjectMapperService.tryToAttachContent(node.getNodeRef());
	}

	public void tryToAttachContent(ScriptNode node, String contentUrl) {
		bulkObjectMapperService.tryToAttachContent(node.getNodeRef(), contentUrl);
	}

	public void mapInPlaceObjectsFromString(ScriptNode parent, String jsonArray) {
		try {
			bulkObjectMapperService.mapInPlaceObjects(parent.getNodeRef(), new JSONArray(jsonArray));
		} catch (JSONException e) {
			logger.error("ERROR PARSING JSON", e);
		}
	}

	public void mapInPlaceObjectsFromFile(ScriptNode parent, String jsonFile) {
		File jf = new File(jsonFile);
		String jsonArray;
		try {
			jsonArray = FileUtils.readFileToString(jf);
			mapInPlaceObjectsFromString(parent, jsonArray);
		} catch (IOException e) {
			logger.error("ERROR READING FILE", e);
		}
	}

	public void mapInPlaceObjectsFromString(ScriptNode parent, String jsonArray, boolean checkParents,
			boolean autoCreate) {
		try {
			bulkObjectMapperService.mapInPlaceObjects(parent.getNodeRef(), new JSONArray(jsonArray), checkParents, null,
					autoCreate);
		} catch (JSONException e) {
			logger.error("ERROR PARSING JSON", e);
		}
	}

	public void mapInPlaceObjectsFromFile(ScriptNode parent, String jsonFile, boolean checkParents,
			boolean autoCreate) {
		File jf = new File(jsonFile);
		String jsonArray;
		try {
			jsonArray = FileUtils.readFileToString(jf);
			mapInPlaceObjectsFromString(parent, jsonArray, checkParents, autoCreate);
		} catch (IOException e) {
			logger.error("ERROR READING FILE", e);
		}
	}

	public String mapInPlaceObjectsFromStringBG(ScriptNode parent, String jsonArray) {
		try {
			return bulkObjectMapperService.mapInPlaceObjectsBG(parent.getNodeRef(), new JSONArray(jsonArray));
		} catch (JSONException e) {
			logger.error("ERROR PARSING JSON", e);
		}
		return null;
	}

	public String mapInPlaceObjectsFromFileBG(ScriptNode parent, String jsonFile) {
		File jf = new File(jsonFile);
		String jsonArray;
		try {
			jsonArray = FileUtils.readFileToString(jf);
			return mapInPlaceObjectsFromStringBG(parent, jsonArray);
		} catch (IOException e) {
			logger.error("ERROR READING FILE", e);
		}
		return null;
	}

	public String mapInPlaceObjectsFromStringBG(ScriptNode parent, String jsonArray, boolean checkParents,
			boolean autoCreate) {
		try {
			return bulkObjectMapperService.mapInPlaceObjectsBG(parent.getNodeRef(), new JSONArray(jsonArray),
					checkParents, null, autoCreate);
		} catch (JSONException e) {
			logger.error("ERROR PARSING JSON", e);
		}
		return null;
	}

	public String mapInPlaceObjectsFromFileBG(ScriptNode parent, String jsonFile, boolean checkParents,
			boolean autoCreate) {
		File jf = new File(jsonFile);
		String jsonArray;
		try {
			jsonArray = FileUtils.readFileToString(jf);
			return mapInPlaceObjectsFromStringBG(parent, jsonArray, checkParents, autoCreate);
		} catch (IOException e) {
			logger.error("ERROR READING FILE", e);
		}
		return null;
	}

	public String getTypeInformation(String type) {
		try {
			return bulkObjectMapperService
					.getTypeInformation(QName.resolveToQName(serviceRegistry.getNamespaceService(), type)).toString(2);
		} catch (JSONException e) {
			logger.error("ERROR Processing Type Information", e);
			return "{ 'message': '" + e.getMessage() + "' }";
		}
	}

	public boolean checkContentUrl(String url) {
		return bulkObjectMapperService.checkContentUrl(url);
	}

	public void modifyCreateDate(ScriptNode node, String createDate) {
		try {
			bulkObjectMapperService.modifyCreateDate(node.getNodeRef(), formatter.parse(createDate));
		} catch (ParseException e) {
			logger.error("Error Parsing Date", e);
		}
	}

	public void bulkExecuteCommand(String cmdObj) {
		try {
			bulkObjectMapperService.bulkExecuteCommand(new JSONObject(cmdObj));
		} catch (JSONException e) {
			logger.error("Error Parsing JSON", e);
		}
	}

	public String bulkExecuteCommandBG(String cmdObj) {
		try {
			return bulkObjectMapperService.bulkExecuteCommandBG(new JSONObject(cmdObj));
		} catch (JSONException e) {
			logger.error("Error Parsing JSON", e);
		}
		return null;
	}

	public String bulkExecuteFilter(String config, String input) {
		try {
			InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
			return bulkObjectMapperService.bulkExecuteFilter(new JSONObject(config), stream);
		} catch (JSONException e) {
			logger.error("Error Parsing JSON", e);
		}
		return null;
	}

	public String getBackgroundCacheItemAsJson(String id) {
		try {
			return backgroundCache.getItemAsJSON(id).toString(2);
		} catch (JSONException e) {
			logger.error("Error Parsing JSON", e);
		}
		return null;
	}

	public JSONArray getBackgroundCacheItemIds() {
		return new JSONArray(bulkObjectMapperService.getBackgroundCacheItemIds());
	}
	
	public String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception {
		return bulkObjectMapperService.getAllBackgroundItems();
	}
	
	public String removeObjectsFromPersistance() {
		return bulkObjectMapperService.removeObjectsFromPersistance();
	}

	public ScriptNode resolveImportRoot(Map<String, String> params) {
		NodeRef node = bulkObjectMapperService.resolveImportRoot(params);
		if (node == null) {
			return null;
		}
		return new ScriptNode(node, this.serviceRegistry, getScope());
	}

	public ScriptVersion createLabeledVersion(ScriptNode node, String versionLabel, String history,
			boolean majorVersion) {
		Version version = bulkObjectMapperService.createLabeledVersion(node.getNodeRef(), versionLabel, history,
				majorVersion);
		return new ScriptVersion(version, this.serviceRegistry, getScope());
	}

}
