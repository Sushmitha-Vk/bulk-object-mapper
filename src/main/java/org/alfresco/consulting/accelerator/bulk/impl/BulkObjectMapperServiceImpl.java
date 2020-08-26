package org.alfresco.consulting.accelerator.bulk.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCacheItem;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperService;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ImportObjectGenerator;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ImportObjectPreProcessor;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.BaseImportObject;
import org.alfresco.consulting.util.transactions.BulkUpdater.BulkUpdaterCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

public class BulkObjectMapperServiceImpl implements BulkObjectMapperService {
	BulkObjectMapperComponent bulkObjectMapperComponent;
	ServiceRegistry serviceRegistry;
	Map<String, NodeMatchCallback> matchRegistry = new HashMap<String, NodeMatchCallback>();
	Map<String, BulkUpdaterCallback<NodeRef>> taskRegistry=new HashMap<String, BulkUpdaterCallback<NodeRef>>();
	

	public void setBulkObjectMapperComponent(BulkObjectMapperComponent bulkObjectMapperComponent) {
		this.bulkObjectMapperComponent = bulkObjectMapperComponent;
		JsonImportObjectGenerator.bulkObjectMapperComponent = this.bulkObjectMapperComponent;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		JsonImportObjectGenerator.namespaceService = this.serviceRegistry.getNamespaceService();
	}


	@Override
	public void tryToAttachContent(NodeRef nodeRef) {
		bulkObjectMapperComponent.tryToAttachContent(nodeRef);
	}

	@Override
	public JSONObject getTypeInformation(QName type) {
		return bulkObjectMapperComponent.getTypeInformation(type);
	}

	@Override
	public void mapInPlaceObjects(NodeRef parent, List<BaseImportObject> ios) {
		bulkObjectMapperComponent.mapInPlaceObjects(parent, ios);
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents) {
		mapInPlaceObjects(defaultParent, ios, checkParents, null);
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, List<BaseImportObject> ios, ImportObjectPreProcessor iopp) {
		mapInPlaceObjects(defaultParent, ios, false, iopp);
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents, ImportObjectPreProcessor iopp) {
		mapInPlaceObjects(defaultParent, ios, checkParents, iopp,false);
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents, ImportObjectPreProcessor iopp,boolean autoCreate) {
		bulkObjectMapperComponent.mapInPlaceObjects(defaultParent, ios, checkParents, iopp,autoCreate);
	}

	@Override
	public void mapInPlaceObjects(NodeRef parent, JSONArray arr) {
		ImportObjectGenerator gen = new JsonImportObjectGenerator(arr);
		
		List<BaseImportObject> ios = bulkObjectMapperComponent.generateImportObjects(gen, arr.length());
		bulkObjectMapperComponent.mapInPlaceObjects(parent, ios);		
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, JSONArray arr, boolean checkParents) {
		mapInPlaceObjects(defaultParent, arr, checkParents, null);
	}

	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, JSONArray arr, ImportObjectPreProcessor iopp) {
		mapInPlaceObjects(defaultParent, arr, false, iopp);
	}


	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, JSONArray arr, boolean checkParents, ImportObjectPreProcessor iopp) {
		mapInPlaceObjects(defaultParent, arr, checkParents, iopp,false);
	}
	@Override
	public void mapInPlaceObjects(NodeRef defaultParent, JSONArray arr, boolean checkParents, ImportObjectPreProcessor iopp,boolean autoCreate) {
		ImportObjectGenerator gen = new JsonImportObjectGenerator(arr);
		
		List<BaseImportObject> ios = bulkObjectMapperComponent.generateImportObjects(gen, arr.length());
		bulkObjectMapperComponent.mapInPlaceObjects(defaultParent, ios, checkParents, iopp,autoCreate);		
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef parent, List<BaseImportObject> ios) {
		return bulkObjectMapperComponent.mapInPlaceObjectsBG(parent, ios);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents) {
		return mapInPlaceObjectsBG(defaultParent, ios, checkParents, null);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, List<BaseImportObject> ios, ImportObjectPreProcessor iopp) {
		return mapInPlaceObjectsBG(defaultParent, ios, false, iopp);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents, ImportObjectPreProcessor iopp) {
		return mapInPlaceObjectsBG(defaultParent, ios, checkParents, iopp,false);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, List<BaseImportObject> ios, boolean checkParents, ImportObjectPreProcessor iopp,boolean autoCreate) {
		return bulkObjectMapperComponent.mapInPlaceObjectsBG(defaultParent, ios, checkParents, iopp,autoCreate);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef parent, JSONArray arr) {
		ImportObjectGenerator gen = new JsonImportObjectGenerator(arr);
		
		List<BaseImportObject> ios = bulkObjectMapperComponent.generateImportObjects(gen, arr.length());
		return bulkObjectMapperComponent.mapInPlaceObjectsBG(parent, ios);		
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, JSONArray arr, boolean checkParents) {
		return mapInPlaceObjectsBG(defaultParent, arr, checkParents, null);
	}

	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, JSONArray arr, ImportObjectPreProcessor iopp) {
		return mapInPlaceObjectsBG(defaultParent, arr, false, iopp);
	}


	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, JSONArray arr, boolean checkParents, ImportObjectPreProcessor iopp) {
		return mapInPlaceObjectsBG(defaultParent, arr, checkParents, iopp,false);
	}
	@Override
	public String mapInPlaceObjectsBG(NodeRef defaultParent, JSONArray arr, boolean checkParents, ImportObjectPreProcessor iopp,boolean autoCreate) {
		ImportObjectGenerator gen = new JsonImportObjectGenerator(arr);
		
		List<BaseImportObject> ios = bulkObjectMapperComponent.generateImportObjects(gen, arr.length());
		return bulkObjectMapperComponent.mapInPlaceObjectsBG(defaultParent, ios, checkParents, iopp,autoCreate);		
	}

	@Override
	public boolean checkContentUrl(String url) {
		return bulkObjectMapperComponent.checkContentUrl(url);
	}

	@Override
	public void tryToAttachContent(NodeRef nodeRef, QName prop, String contentUrl) {
		bulkObjectMapperComponent.tryToAttachContent(nodeRef, prop, contentUrl);
	}

	@Override
	public void tryToAttachContent(NodeRef nodeRef, String contentUrl) {
		bulkObjectMapperComponent.tryToAttachContent(nodeRef, contentUrl);
	}

	@Override
	public List<NodeRef> getMatchingNodes(NodeRef container,NodeMatchCallback cb) {
		return bulkObjectMapperComponent.getMatchingNodes(container,cb);
	}

	@Override
	public void registerNodeMatcher(String id, NodeMatchCallback cb) {
		matchRegistry.put(id, cb);
	}

	@Override
	public void registerNodeTask(String id, BulkUpdaterCallback<NodeRef> cb) {
		this.taskRegistry.put(id, cb);
	}

	@Override
	public void bulkUpdate(SearchParameters sp, String task, int batchSize, int numThreads,boolean runAsSystem) {
		bulkObjectMapperComponent.bulkUpdate(sp,taskRegistry.get(task),batchSize,numThreads,runAsSystem);
	}

	@Override
	public void bulkUpdate(NodeRef container,String match, String task, int batchSize, int numThreads,boolean runAsSystem) {
		bulkObjectMapperComponent.bulkUpdate(container,matchRegistry.get(match),taskRegistry.get(task),batchSize,numThreads, runAsSystem);
	}
	@Override
	public void modifyCreateDate(NodeRef nodeRef, Date createDate) {
		bulkObjectMapperComponent.modifyCreateDate(nodeRef,createDate);
	}

	@Override
	public void modifyCreateDates(Map<NodeRef, Date> updateList) {
		bulkObjectMapperComponent.modifyCreateDates(updateList);
	}

	@Override
	public void bulkExecuteCommand(JSONObject cmdObj) {
		bulkObjectMapperComponent.bulkExecuteCommand(cmdObj);
	}

	@Override
	public String bulkExecuteFilter(JSONObject config, InputStream input) {
		return bulkObjectMapperComponent.bulkExecuteFilter(config, input);

	}

	@Override
	public String bulkExecuteCommandBG(JSONObject cmdObj) {
		return bulkObjectMapperComponent.bulkExecuteCommandBG(cmdObj);
	}

	@Override
	public BackgroundCacheItem getBackgroundCacheItem(String id) {
		return bulkObjectMapperComponent.getBackgroundCacheItem(id);
	}

	@Override
	public Set<String> getBackgroundCacheItemIds() {
		return bulkObjectMapperComponent.getBackgroundCacheItemIds();
	}
	
	@Override
	public String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception {
		return bulkObjectMapperComponent.getAllBackgroundItems();
	}
	
	@Override
	public String removeObjectsFromPersistance() {
		return bulkObjectMapperComponent.removeObjectsFromPersistance();
	}

	@Override
	public void registerImportRootResolver(ImportRootResolver irr) {
		bulkObjectMapperComponent.registerImportRootResolver(irr);
	}

	@Override
	public NodeRef resolveImportRoot(Map<String, String> params) {
		return bulkObjectMapperComponent.resolveImportRoot(params);
	}

	@Override
	public Version createLabeledVersion(NodeRef nodeRef, String versionLabel, String history, boolean majorVersion) {
		return  bulkObjectMapperComponent.createLabeledVersion(nodeRef, versionLabel, history, majorVersion);
	}

}
