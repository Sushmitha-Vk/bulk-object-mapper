package org.alfresco.consulting.accelerator.bulk.impl;

import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperConstants;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperService;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

public class BulkUpdateContentPolicy extends AbstractLifecycleBean implements OnContentUpdatePolicy {

	private PolicyComponent policyComponent;
	private BulkObjectMapperService bulkObjectMapperService;
	private static final Log logger = LogFactory.getLog(BulkUpdateContentPolicy.class);
	ServiceRegistry serviceRegistry;
	NodeService nodeService;
	DictionaryService dictionaryService;
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = this.serviceRegistry.getNodeService();
		this.dictionaryService = this.serviceRegistry.getDictionaryService();
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setBulkObjectMapperService(
			BulkObjectMapperService bulkObjectMapperService) {
		this.bulkObjectMapperService = bulkObjectMapperService;
	}
	
	public void initialise() {
		logger.info("Deffering adding policy to clean up objects with hanging content URL to onBootstrap");
	}	


	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		if (!nodeService.exists(nodeRef)) return;
		bulkObjectMapperService.tryToAttachContent(nodeRef);
	}

	@Override
	protected void onBootstrap(ApplicationEvent event) {
		this.policyComponent.bindClassBehaviour(
				OnContentUpdatePolicy.QNAME,
				BulkObjectMapperConstants.aspectMissingContent(),
				new JavaBehaviour(this, OnContentUpdatePolicy.QNAME.getLocalName(), NotificationFrequency.TRANSACTION_COMMIT));
		logger.info("INITIALISED adding policy to clean up objects with hanging content URL");
	}

	@Override
	protected void onShutdown(ApplicationEvent event) {
		// TODO Auto-generated method stub
		
	}


}
