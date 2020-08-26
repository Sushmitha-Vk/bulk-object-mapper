package org.alfresco.consulting.accelerator.bulk;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONObject;

public interface BulkUpdateCommand {
	void workUnit(NodeRef nodeRef, JSONObject params, JSONObject ctx);

	void preTxn(JSONObject ctx);

	void postTxn(JSONObject ctx);

	void preExec(JSONObject ctx, Map<NodeRef, JSONObject> map);

	void postExec(JSONObject ctx);

	String commandName();

	Map<NodeRef, JSONObject> parseJson(JSONObject data, JSONArray list, JSONObject ctx);
}
