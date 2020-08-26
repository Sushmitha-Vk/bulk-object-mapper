package org.alfresco.consulting.accelerator.bulk.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeleteCommand extends AbstractBulkUpdateCommandBase {
	private static final Log logger = LogFactory.getLog(DeleteCommand.class);
	public static final String COMMAND_NAME = "DELETE";
	public static final String FIELD_NODE_REF = "nodeRef";
	public static final String FIELD_QUERY = "query";
	public static final String FIELD_ANCESTOR_GUID = "ancestorGuid";
	public static final String FIELD_OBJECT_TYPE = "objectType";
	public static final String FIELD_LIMIT = "limit";
	public static final String FIELD_SKIP = "skip";
	public static final String CTX_FIELD_COUNT = "count";
	public static final String CONFIG_FIELD = "config";

	@Override
	public void workUnit(NodeRef nodeRef, JSONObject params, JSONObject ctx) {
		// TODO: Defer Folders
		logger.debug("Deleting: " + nodeRef.getId());
		nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
		nodeService.deleteNode(nodeRef);
		int count = ctx.optInt(CTX_FIELD_COUNT);
		count++;
		try {
			ctx.put(CTX_FIELD_COUNT, count);
		} catch (JSONException e) {
			logger.error("JSON ERROR", e);
		}
	}

	@Override
	public String commandName() {
		return COMMAND_NAME;
	}

	@Override
	public Map<NodeRef, JSONObject> parseJson(JSONObject data, JSONArray list, JSONObject ctx) {
		Map<NodeRef, JSONObject> map = new HashMap<NodeRef, JSONObject>();
		JSONObject config = new JSONObject();
		if (data instanceof JSONObject) {
			if (!data.optString(FIELD_ANCESTOR_GUID).isEmpty()) {
				int limit = data.optInt(FIELD_LIMIT, 100000);
				int skip = data.optInt(FIELD_SKIP, 0);
				String objectType = data.optString(FIELD_OBJECT_TYPE, "sys:base");
				String query = String.format("ANCESTOR:'workspace://SpacesStore/%s' AND TYPE:'%s'",
						data.optString(FIELD_ANCESTOR_GUID), objectType);
				SearchParameters sp = new SearchParameters();
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
				sp.setQuery(query);
				sp.setMaxItems(limit);
				sp.setSkipCount(skip);
				logger.info("Start Search: " + query);
				List<NodeRef> nodeRefs = searchService.query(sp).getNodeRefs();
				map = new HashMap<NodeRef, JSONObject>();
				for (NodeRef nodeRef : nodeRefs) {
					map.put(nodeRef, new JSONObject());
				}
				try {
					config.put(FIELD_ANCESTOR_GUID, data.optString(FIELD_ANCESTOR_GUID));
					config.put(FIELD_QUERY, query);
					config.put(FIELD_LIMIT, limit);
					config.put(FIELD_SKIP, skip);
					config.put(FIELD_OBJECT_TYPE, objectType);
					ctx.put(CONFIG_FIELD, config);
				} catch (JSONException e1) {
					logger.error("error updating context", e1);
				}
				return map;
			}
			if (!data.optString(FIELD_QUERY).isEmpty()) {
				String query = data.optString(FIELD_QUERY);
				int limit = data.optInt(FIELD_LIMIT, 100000);
				int skip = data.optInt(FIELD_SKIP, 0);
				SearchParameters sp = new SearchParameters();
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
				sp.setQuery(query);
				sp.setMaxItems(limit);
				sp.setSkipCount(skip);
				logger.info("Start Search: " + query);
				List<NodeRef> nodeRefs = searchService.query(sp).getNodeRefs();
				map = new HashMap<NodeRef, JSONObject>();
				for (NodeRef nodeRef : nodeRefs) {
					map.put(nodeRef, new JSONObject());
				}
				try {
					config.put(FIELD_QUERY, query);
					config.put(FIELD_LIMIT, limit);
					config.put(FIELD_SKIP, skip);
					ctx.put(CONFIG_FIELD, config);
				} catch (JSONException e1) {
					logger.error("error updating context", e1);
				}
				return map;

			}
		}
		logger.warn("Nothing To Delete");
		return map;
	}

	@Override
	public void postTxn(JSONObject ctx) {
		logger.info("INTERMEDIATE COUNT: " + ctx.optInt(CTX_FIELD_COUNT));
	}

	@Override
	public void preExec(JSONObject ctx) {
		try {
			ctx.put(CTX_FIELD_COUNT, 0);
		} catch (JSONException e) {
			logger.error("JSON ERROR", e);
		}
	}

	@Override
	public void postExec(JSONObject ctx) {
		logger.info("FINAL COUNT: " + ctx.optInt(CTX_FIELD_COUNT));
	}

}
