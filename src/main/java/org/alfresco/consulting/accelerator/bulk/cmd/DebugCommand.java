package org.alfresco.consulting.accelerator.bulk.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class DebugCommand extends AbstractBulkUpdateCommandBase {
	private static final Log logger = LogFactory.getLog(DebugCommand.class);
	public static final String COMMAND_NAME = "DEBUG";
	public static final String FIELD_QUERY = "query";
	public static final String FIELD_LIMIT = "limit";
	public static final String FIELD_SKIP = "skip";

	@Override
	public void workUnit(NodeRef nodeRef, JSONObject params, JSONObject ctx) {
		logger.debug("WORK_UNIT:" + nodeRef.getId());
	}

	@Override
	public String commandName() {
		return COMMAND_NAME;
	}

	@Override
	public Map<NodeRef, JSONObject> parseJson(JSONObject data, JSONArray list) {
		if (data == null) {
			data = new JSONObject();
		}
		String query = data.optString(FIELD_QUERY, "TYPE:'mdr:document'");
		int limit = data.optInt(FIELD_LIMIT, 1000);
		int skip = data.optInt(FIELD_SKIP, 0);
		SearchParameters sp = new SearchParameters();
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		sp.setQuery(query);
		sp.setMaxItems(limit);
		sp.setSkipCount(skip);
		logger.debug("Start Search");
		List<NodeRef> nodeRefs = searchService.query(sp).getNodeRefs();
		Map<NodeRef, JSONObject> map = new HashMap<NodeRef, JSONObject>();
		for (NodeRef nodeRef : nodeRefs) {
			map.put(nodeRef, new JSONObject());
		}
		return map;
	}

	@Override
	public void preTxn(JSONObject ctx) {
		logger.debug("PRE_TRANSACTION");
	}

	@Override
	public void postTxn(JSONObject ctx) {
		logger.debug("POST_TRANSACTION");
	}

	@Override
	public void preExec(JSONObject ctx) {
		logger.debug("PRE_EXECUTION");
	}

	@Override
	public void postExec(JSONObject ctx) {
		logger.debug("POST_EXECUTION");
	}

}
