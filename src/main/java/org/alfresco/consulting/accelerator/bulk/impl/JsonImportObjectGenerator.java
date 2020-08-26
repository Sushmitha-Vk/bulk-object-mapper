package org.alfresco.consulting.accelerator.bulk.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperConstants;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.BaseImportObject;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ContentDataObject;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ImportObject;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ImportObjectGenerator;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.VersionHistory;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.VersionedImportObject;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonImportObjectGenerator implements ImportObjectGenerator {
	JSONArray arr;
	int currentIndex=0;
	static NamespaceService namespaceService;
	static BulkObjectMapperComponent bulkObjectMapperComponent;
	
	static private final String TYPE_NAME="type";
	static private final String PATH_NAME="path";
	static private final String ASPECTS_NAME="aspects";
	static private final String PROPS_NAME="properties";
	static private final String CONTENT_URLS_NAME="contentUrls";
	static private final String PRIMARY_PARENT_NAME="primaryParent";
	static private final String IMPORT_ID="importId";
	static public final String IMPORT_GROUP_ID="groupId";
	static public final String IMPORT_RUN_ID="runId";
	static public final String VERSION_HISTORY="versionHistory";
	static public final String VERSION_LABEL="versionLabel";
	static public final String VERSION_COMMENT="versionComment";

	private static final Log logger = LogFactory.getLog(JsonImportObjectGenerator.class);
	public JsonImportObjectGenerator(JSONArray arr) {
		this.arr = arr;
	}

	@Override
	public BaseImportObject generateObject() {
		if (currentIndex >= arr.length()) {
			return null;
		}

		try {
			JSONObject obj = arr.getJSONObject(currentIndex);
			if (obj.has(VERSION_HISTORY)) {
				return generateVersionHistoryFromJson(obj);
			}
			return generateImportObjectFromJson(obj);
		} catch (JSONException e) {
			logger.debug("ERROR GETTING JSON OBJECT",e);
			return null;
		} finally {
			currentIndex++;
		}
	}
	
	private VersionHistory generateVersionHistoryFromJson(JSONObject obj) {
		QName type=null;
		NodeRef primaryParent = null;
		List<String> pathSegments = new ArrayList<String>();
		List<VersionedImportObject> versionedObjects = new ArrayList<VersionedImportObject>();
		try {
			type = QName.resolveToQName(namespaceService, obj.getString(TYPE_NAME));
			if (obj.has(PRIMARY_PARENT_NAME)) {
				primaryParent=new NodeRef(obj.getString(PRIMARY_PARENT_NAME));
			}
			if (obj.has(PATH_NAME)) {
				String[] pathArray = obj.getString(PATH_NAME).split("/");
				for (String pathSeg :pathArray) {
					if (!pathSeg.isEmpty()) {
						pathSegments.add(pathSeg);
					}
				}
			}
			JSONArray vArr=obj.getJSONArray(VERSION_HISTORY);
			for (int i=0; i < vArr.length(); i++) {
				versionedObjects.add((VersionedImportObject) generateImportObjectFromJson(vArr.getJSONObject(i),obj));
			}
		} catch (JSONException e) {
			logger.error("ERROR PROCESSING JSON to IMPORT OBJECT -- SKIPPING: "+obj.toString(),e);
			return null;
		}
		if (type == null) {
			logger.error("ERROR PROCESSING JSON NULL TYPE -- SKIPPING: "+obj.toString());
			return null;
		}
		return BulkObjectMapperComponent.createVersionHistory(type, primaryParent, pathSegments, versionedObjects);
	}
	private String getStringFromObjectOrContainer(String name,JSONObject obj, JSONObject container) throws JSONException {
		if (container == null) return obj.getString(name);
		return obj.optString(name, container.getString(name));
	}
	private ImportObject generateImportObjectFromJson(JSONObject obj) {
		return generateImportObjectFromJson(obj,null);
	}
	private ImportObject generateImportObjectFromJson(JSONObject obj,JSONObject container) {
		QName type=null;
		NodeRef primaryParent = null;
		Set<QName> aspects = new HashSet<QName>();
		Map<QName,Serializable> props = new HashMap<QName,Serializable>();
		List<String> pathSegments = new ArrayList<String>();
		props.put(ContentModel.PROP_CREATOR, AuthenticationUtil.getFullyAuthenticatedUser());
		props.put(ContentModel.PROP_MODIFIER, AuthenticationUtil.getFullyAuthenticatedUser());
		props.put(ContentModel.PROP_CREATED, new Date());
		props.put(ContentModel.PROP_MODIFIED, new Date());
		
		JSONArray jAspects = new JSONArray();
		JSONObject jProps = new JSONObject();
		JSONObject jContentUrls = new JSONObject();
		JSONObject importErrorObject = new JSONObject();
		if (logger.isTraceEnabled()) {
			try {
				logger.trace("OBJ: "+obj.toString(2));
			} catch (JSONException e) {
				logger.trace("Error formating debug statement", e);
			}
		}
		try {
			type = QName.resolveToQName(namespaceService, getStringFromObjectOrContainer(TYPE_NAME,obj,container));
			if (obj.has(IMPORT_ID)) {
				importErrorObject.put(IMPORT_ID, obj.getString(IMPORT_ID));
				props.put(BulkObjectMapperConstants.propItemId(),obj.getString(IMPORT_ID));
			}
			if (obj.has(IMPORT_GROUP_ID)) { 
				importErrorObject.put(IMPORT_ID, obj.getString(IMPORT_GROUP_ID));
				props.put(BulkObjectMapperConstants.propGroupId(),obj.getString(IMPORT_GROUP_ID));
			}
			if (obj.has(IMPORT_RUN_ID)) { 
				importErrorObject.put(IMPORT_ID, obj.getString(IMPORT_RUN_ID));
				props.put(BulkObjectMapperConstants.propRunId(),obj.getString(IMPORT_RUN_ID));
			}
			if (obj.has(PRIMARY_PARENT_NAME)) {
				primaryParent=new NodeRef(obj.getString(PRIMARY_PARENT_NAME));
			}
			if (obj.has(PATH_NAME)  || ((container != null) &&container.has(PATH_NAME))) {
				String[] pathArray = getStringFromObjectOrContainer(PATH_NAME,obj,container).split("/");
				for (String pathSeg :pathArray) {
					if (!pathSeg.isEmpty()) {
						pathSegments.add(pathSeg);
					}
				}
			}
			if (obj.has(ASPECTS_NAME))  { jAspects = obj.getJSONArray(ASPECTS_NAME); }
			if (obj.has(PROPS_NAME))  { jProps = obj.getJSONObject(PROPS_NAME); }
			if (obj.has(CONTENT_URLS_NAME))  { jContentUrls = obj.getJSONObject(CONTENT_URLS_NAME); }
			for (int i=0; i < jAspects.length(); i++) {
				if (!jAspects.getString(i).isEmpty()) {
					aspects.add(QName.resolveToQName(namespaceService, jAspects.getString(i)));
				}
			}
			if (JSONObject.getNames(jProps) != null) {
				for  (String key : JSONObject.getNames(jProps)) {
					if (jProps.get(key) instanceof JSONArray) {
						JSONArray aObj = (JSONArray) jProps.get(key);
						ArrayList aProp = new ArrayList();
						for (int i = 0; i < aObj.length() ; i++) {
							aProp.add(aObj.get(i));
						}
						props.put(QName.resolveToQName(namespaceService, key), (Serializable) aProp);
					} else {
						props.put(QName.resolveToQName(namespaceService, key), (Serializable) jProps.get(key));
					}
				}
			}
			if (JSONObject.getNames(jContentUrls) != null) {
				for  (String key : JSONObject.getNames(jContentUrls)) {
					ContentDataObject cdo = bulkObjectMapperComponent.getContentDataObjectFromUrl(jContentUrls.getString(key));
					props.put(QName.resolveToQName(namespaceService, key), cdo);
				}
			}
		} catch (JSONException e) {
			logger.error("ERROR PROCESSING JSON to IMPORT OBJECT -- SKIPPING: "+importErrorObject.toString(),e);
			return null;
		}
		if (type == null) {
			logger.error("ERROR PROCESSING JSON NULL TYPE -- SKIPPING: "+importErrorObject.toString());
			return null;
		}
		if (obj.has(VERSION_LABEL)) {
			String versionLabel = obj.optString(VERSION_LABEL);
			String versionComment = obj.optString(VERSION_COMMENT);
			return BulkObjectMapperComponent.createVersionedImportObject(type, aspects, props,primaryParent,pathSegments,versionLabel,versionComment);
		}
		return BulkObjectMapperComponent.createImportObject(type, aspects, props,primaryParent,pathSegments);
	}

}
