package org.alfresco.consulting.accelerator.bulk;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCacheItem;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.BaseImportObject;
import org.alfresco.consulting.accelerator.bulk.impl.BulkObjectMapperComponent.ImportObjectPreProcessor;
import org.alfresco.consulting.util.transactions.BulkUpdater.BulkUpdaterCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface BulkObjectMapperService {
	public interface NodeMatchCallback {
		boolean matches(NodeRef nodeRef);
	}

	public interface ImportRootResolver {
		NodeRef resolve(Map<String, String> params);
	}

	public void registerImportRootResolver(ImportRootResolver irr);

	public void mapInPlaceObjects(NodeRef parent, List<BaseImportObject> ios);

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents);

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios,
			ImportObjectPreProcessor iopp);

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents,
			ImportObjectPreProcessor iopp);

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents,
			ImportObjectPreProcessor iopp, boolean autoCreate);

	public void mapInPlaceObjects(NodeRef parent, JSONArray arr);

	public void mapInPlaceObjects(final NodeRef defaultParent, final JSONArray arr, boolean checkParents);

	public void mapInPlaceObjects(final NodeRef defaultParent, final JSONArray arr, ImportObjectPreProcessor iopp);

	public void mapInPlaceObjects(final NodeRef defaultParent, final JSONArray arr, boolean checkParents,
			ImportObjectPreProcessor iopp);

	public void mapInPlaceObjects(final NodeRef defaultParent, final JSONArray arr, boolean checkParents,
			ImportObjectPreProcessor iopp, boolean autoCreate);

	public String mapInPlaceObjectsBG(NodeRef parent, List<BaseImportObject> ios);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios,
			boolean checkParents);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios,
			ImportObjectPreProcessor iopp);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios,
			boolean checkParents, ImportObjectPreProcessor iopp);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios,
			boolean checkParents, ImportObjectPreProcessor iopp, boolean autoCreate);

	public String mapInPlaceObjectsBG(NodeRef parent, JSONArray arr);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final JSONArray arr, boolean checkParents);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final JSONArray arr, ImportObjectPreProcessor iopp);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final JSONArray arr, boolean checkParents,
			ImportObjectPreProcessor iopp);

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final JSONArray arr, boolean checkParents,
			ImportObjectPreProcessor iopp, boolean autoCreate);

	public void tryToAttachContent(NodeRef nodeRef);

	public void tryToAttachContent(NodeRef nodeRef, String contentUrl);

	public void tryToAttachContent(NodeRef nodeRef, QName prop, String contentUrl);

	public Version createLabeledVersion(NodeRef nodeRef, String versionLabel, String history, boolean majorVersion);

	public JSONObject getTypeInformation(QName type);

	public boolean checkContentUrl(String url);

	List<NodeRef> getMatchingNodes(NodeRef container, NodeMatchCallback cb);

	void registerNodeMatcher(String id, NodeMatchCallback cb);

	void registerNodeTask(String id, BulkUpdaterCallback<NodeRef> cb);

	void bulkUpdate(SearchParameters sp, String task, int batchSize, int numThreads, boolean runAsSystem);

	void bulkUpdate(NodeRef container, String match, String task, int batchSize, int numThreads, boolean runAsSystem);

	public void modifyCreateDate(NodeRef nodeRef, Date createDate);

	public void modifyCreateDates(final Map<NodeRef, Date> updateList);

	public void bulkExecuteCommand(JSONObject cmdObj);

	public String bulkExecuteCommandBG(JSONObject cmdObj);

	public String bulkExecuteFilter(JSONObject config, InputStream input);

	public BackgroundCacheItem getBackgroundCacheItem(String id);

	public Set<String> getBackgroundCacheItemIds();
	
	public String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception;

	public String removeObjectsFromPersistance();

	public NodeRef resolveImportRoot(Map<String, String> params);
}
