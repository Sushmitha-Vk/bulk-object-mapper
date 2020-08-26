package org.alfresco.consulting.accelerator.bulk.filter;

import org.alfresco.consulting.accelerator.bulk.BulkUpdateFilter;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;

public abstract class AbstractBulkUpdateFilterBase implements BulkUpdateFilter {
	/* TODO Add Execution/Context Object */
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
		bulkObjectMapperComponent.registerFilter(this);
	}

}
