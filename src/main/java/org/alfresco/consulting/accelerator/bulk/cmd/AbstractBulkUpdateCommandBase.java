package org.alfresco.consulting.accelerator.bulk.cmd;

import java.util.Map;

import org.alfresco.consulting.accelerator.bulk.BulkUpdateCommand;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class AbstractBulkUpdateCommandBase implements BulkUpdateCommand {
	protected NodeService nodeService;
	protected SearchService searchService;
	protected ServiceRegistry serviceRegistry;
	protected BulkObjectMapperComponent bulkObjectMapperComponent;

	public void setBulkObjectMapperComponent(BulkObjectMapperComponent bulkObjectMapperComponent) {
		this.bulkObjectMapperComponent = bulkObjectMapperComponent;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = this.serviceRegistry.getNodeService();
		this.searchService = this.serviceRegistry.getSearchService();
	}

	public void init() {
		bulkObjectMapperComponent.registerCommand(this);
	}

	@Override
	public void preTxn(JSONObject ctx) {

	}

	@Override
	public void postTxn(JSONObject ctx) {

	}

	public void preExec(JSONObject ctx) {

	}

	public Map<NodeRef, JSONObject> parseJson(JSONObject data, JSONArray list) {
		throw new RuntimeException("parseJson Method must be defined");
	}

	@Override
	public Map<NodeRef, JSONObject> parseJson(JSONObject data, JSONArray list, JSONObject ctx) {
		return parseJson(data, list);
	}

	@Override
	public void preExec(JSONObject ctx, Map<NodeRef, JSONObject> map) {
		preExec(ctx);
	}

	@Override
	public void postExec(JSONObject ctx) {

	}

}
