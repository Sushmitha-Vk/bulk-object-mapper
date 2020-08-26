package org.alfresco.consulting.accelerator.bulk.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCache;
import org.alfresco.consulting.accelerator.bgrun.BackgroundRunner;
import org.alfresco.consulting.accelerator.bgrun.BackgroundCacheItem;
import org.alfresco.consulting.accelerator.bgrun.BackgroundRunner.BackgroundCachedRunnable;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperConstants;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperService.ImportRootResolver;
import org.alfresco.consulting.accelerator.bulk.BulkUpdateCommand;
import org.alfresco.consulting.accelerator.bulk.BulkUpdateFilter;
import org.alfresco.consulting.accelerator.bulk.BulkUpdaterThreadPool;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperConstants.UrlCheckMode;
import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperService.NodeMatchCallback;
import org.alfresco.consulting.accelerator.bulk.ContentUrlValidator;
import org.alfresco.consulting.accelerator.bulk.url.ConstantContentUrlValidator;
import org.alfresco.consulting.accelerator.bulk.url.S3ContentUrlValidator;
import org.alfresco.consulting.accelerator.bulk.url.StoreContentUrlValidator;
import org.alfresco.consulting.util.folder_hierarchy.FolderHierarchyHelper;
import org.alfresco.consulting.util.transactions.BulkUpdater.BulkUpdaterCallback;
import org.alfresco.consulting.util.tree_walker.TreeWalker.Callback;
import org.alfresco.consulting.util.tree_walker.impl.FileFolderTreeWalker;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.InvalidAspectException;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.rendition.RenditionService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;

public class BulkObjectMapperComponent {
	BulkUpdaterThreadPool bulkUpdater;
	FileFolderTreeWalker fileFolderTreeWalker;
	static FolderHierarchyHelper folderHierarchyHelper;
	ContentService contentService;
	static ServiceRegistry serviceRegistry;
	RenditionService renditionService;
	NodeService nodeService;
	VersionService versionService;
	SearchService searchService;
	NodeLocatorService nodeLocatorService;
	NamespaceService namespaceService;
	RetryingTransactionHelper txnHelper;
	NodeRef samplesFolder;
	MimetypeService mimetypeService;
	DictionaryService dictionaryService;
	UrlCheckMode urlCheckMode;
	BehaviourFilter behaviourFilter;
	BackgroundCache backgroundCache;
	BackgroundRunner backgroundRunner;
	ContentUrlValidator contentUrlValidator;
	Map<String, BulkUpdateCommand> commandRegistry = new HashMap<String, BulkUpdateCommand>();
	Map<String, BulkUpdateFilter> filterRegistry = new HashMap<String, BulkUpdateFilter>();
	Set<ImportRootResolver> importRootResolverRegistry = new HashSet<ImportRootResolver>();
	OverridingVersionLabelPolicy overridingVersionLabelPolicy;
	private String regionId;
	private String kmsKey;
	private String s3bucket;
	
	static BulkObjectMapperComponent _instance;

	public static final String CONTENT_URL = "contentUrl";
	public static final String MIMETYPE = "mimetype";
	public static final String SIZE = "size";
	public static final String ENCODING = "encoding";
	public static final String LOCALE = "locale";
	private final static int MAX_CONTENT_URL_LENGTH = 255;
	private final static String DEFAULT_TEXT_ENCODING = "UTF-8";
	public static final String FIELD_COMMAND = "command";
	public static final String FIELD_FILTER = "filter";
	public static final String FIELD_DATA = "data";
	public static final String FIELD_LIST = "list";

	private static final Log logger = LogFactory.getLog(BulkObjectMapperComponent.class);
	private boolean versionPolicyNeedsToBeRegistered = true;

	private void registerVersionPolicy() {
		if (versionPolicyNeedsToBeRegistered && (versionService != null) && (overridingVersionLabelPolicy != null)) {
			versionPolicyNeedsToBeRegistered = false;
			versionService.registerVersionLabelPolicy(ContentModel.TYPE_BASE, overridingVersionLabelPolicy);
		}
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		BulkObjectMapperComponent.serviceRegistry = serviceRegistry;
		this.contentService = BulkObjectMapperComponent.serviceRegistry.getContentService();
		this.nodeService = BulkObjectMapperComponent.serviceRegistry.getNodeService();
		this.renditionService = BulkObjectMapperComponent.serviceRegistry.getRenditionService();
		this.nodeLocatorService = BulkObjectMapperComponent.serviceRegistry.getNodeLocatorService();
		this.namespaceService = BulkObjectMapperComponent.serviceRegistry.getNamespaceService();
		this.mimetypeService = BulkObjectMapperComponent.serviceRegistry.getMimetypeService();
		this.dictionaryService = BulkObjectMapperComponent.serviceRegistry.getDictionaryService();
		this.searchService = BulkObjectMapperComponent.serviceRegistry.getSearchService();
		this.txnHelper = BulkObjectMapperComponent.serviceRegistry.getRetryingTransactionHelper();
		this.versionService = BulkObjectMapperComponent.serviceRegistry.getVersionService();

		registerVersionPolicy();
		_instance = this;
	}

	public static ServiceRegistry getServiceRegistry() {
		return BulkObjectMapperComponent.serviceRegistry;
	}

	public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
		this.behaviourFilter = behaviourFilter;
	}

	public void setUrlCheckMode(String urlCheckMode) {
		this.urlCheckMode = UrlCheckMode.valueOf(urlCheckMode.toUpperCase());
		if (this.urlCheckMode == UrlCheckMode.STORE) {
			contentUrlValidator = new StoreContentUrlValidator(contentService);
		} else if (this.urlCheckMode == UrlCheckMode.S3) {
			contentUrlValidator = new S3ContentUrlValidator(contentService, s3bucket, regionId, kmsKey);
		} else if (this.urlCheckMode == UrlCheckMode.TRUE) {
			contentUrlValidator = new ConstantContentUrlValidator(true);
		} else if (this.urlCheckMode == UrlCheckMode.FALSE) {
			contentUrlValidator = new ConstantContentUrlValidator(false);
		} else {
			contentUrlValidator = new StoreContentUrlValidator(contentService);
		}
	}

	public void setBulkUpdater(BulkUpdaterThreadPool bulkUpdater) {
		this.bulkUpdater = bulkUpdater;
	}

	public void setFolderHierarchyHelper(FolderHierarchyHelper folderHierarchyHelper) {
		BulkObjectMapperComponent.folderHierarchyHelper = folderHierarchyHelper;
	}

	public void setBackgroundCache(BackgroundCache backgroundCache) {
		this.backgroundCache = backgroundCache;
	}

	public void setBackgroundRunner(BackgroundRunner backgroundRunner) {
		this.backgroundRunner = backgroundRunner;
	}

	public void setOverridingVersionLabelPolicy(OverridingVersionLabelPolicy overridingVersionLabelPolicy) {
		this.overridingVersionLabelPolicy = overridingVersionLabelPolicy;
		registerVersionPolicy();
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public void setKmsKey(String kmsKey) {
		this.kmsKey = kmsKey;
	}

	public void setS3bucket(String s3bucket) {
		this.s3bucket = s3bucket;
	}

	public interface BaseImportObject {
		QName getType();

		List<String> getPath();

		QName getAssocName();

		QName getAssocType();

		NodeRef getParentFromPath(NodeRef defaultParent);

		NodeRef getParentFromPath(NodeRef defaultParent, boolean autoCreate);

		NodeRef getPrimaryParent();

		Set<NodeRef> getSecondaryParents();

		void setPrimaryParent(NodeRef primaryParent);

		void addSecondaryParent(NodeRef secondaryParent);

		void setPath(List<String> path);
	}

	public interface ImportObject extends BaseImportObject {
		Set<QName> getAspects();

		Map<QName, Serializable> getProps();

		void setType(QName type);

		void setAssocName(QName assocName);

		void setAssocType(QName assocType);

		void setAspects(Set<QName> aspects);

		void setProps(Map<QName, Serializable> props);
	}

	public interface VersionedImportObject extends ImportObject {
		String getVersionLabel();

		String getVersionComment();

	}

	public interface VersionHistory extends BaseImportObject {
		List<VersionedImportObject> getHistory();

	}

	public interface ImportObjectPreProcessor {
		public void preProcessImportObject(BaseImportObject io);
	}

	public interface ContentDataObject extends Serializable {
		public String getContentUrl();

		public void setContentUrl(String url);

		public String getMimetype();

		public long getSize();

		public String getEncoding();

		public Locale getLocale();

		public String getInfoUrl();

		public ContentData getContentProperty();

		public boolean hasContent();
	}

	public interface ImportObjectGenerator {
		BaseImportObject generateObject();
	}

	public interface NameValidator {
		boolean isValidName(String name);

		String getValidName(String name);
	}

	private static class MissingContentDataObjectImpl implements ContentDataObject {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3987266690541642432L;
		String contentUrl;

		MissingContentDataObjectImpl(String contentUrl) {
			this.contentUrl = contentUrl;
		}

		public String getContentUrl() {
			return this.contentUrl;
		}

		public String getMimetype() {
			return null;
		}

		public long getSize() {
			return -1;
		}

		public String getEncoding() {
			return null;
		}

		public Locale getLocale() {
			return null;
		}

		public String getInfoUrl() {
			return null;
		}

		public ContentData getContentProperty() {
			return null;
		}

		public boolean hasContent() {
			return false;
		}

		@Override
		public void setContentUrl(String url) {
			this.contentUrl = url;
		}
	}

	private static class ContentDataObjectImpl implements ContentDataObject {

		/**
		 * 
		 */
		private static final long serialVersionUID = -9042526357754576927L;
		ContentData cd;

		ContentDataObjectImpl(String infoUrl) {
			ContentData.createContentProperty(infoUrl);
		}

		ContentDataObjectImpl(ContentData cd) {
			this.cd = cd;
		}

		ContentDataObjectImpl(Map<String, Object> map) {
			this.cd = new ContentData((String) map.get(CONTENT_URL), (String) map.get(MIMETYPE), (Long) map.get(SIZE),
					(String) map.get(ENCODING), new Locale((String) map.get(LOCALE)));
		}

		ContentDataObjectImpl(JSONObject obj) {
			try {
				this.cd = new ContentData(obj.getString(CONTENT_URL), obj.getString(MIMETYPE), obj.getLong(SIZE),
						obj.getString(ENCODING), new Locale(obj.getString(LOCALE)));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ContentDataObjectImpl(String contentUrl, String mimetype, long size, String encoding, Locale locale) {
			this.cd = new ContentData(contentUrl, mimetype, size, encoding, locale);
		}

		@Override
		public String getContentUrl() {
			return cd.getContentUrl();
		}

		@Override
		public String getMimetype() {
			return cd.getMimetype();
		}

		@Override
		public long getSize() {
			return cd.getSize();
		}

		@Override
		public String getEncoding() {
			return cd.getEncoding();
		}

		@Override
		public Locale getLocale() {
			return cd.getLocale();
		}

		@Override
		public String getInfoUrl() {
			return cd.getInfoUrl();
		}

		@Override
		public ContentData getContentProperty() {
			return cd;
		}

		@Override
		public boolean hasContent() {
			return true;
		}

		@Override
		public void setContentUrl(String url) {
			cd = new ContentData(url, cd.getMimetype(), cd.getSize(), cd.getEncoding(), cd.getLocale());
		}

	}

	private static class ImportObjectImpl implements ImportObject {

		QName type;
		QName assocName;
		QName assocType;
		List<String> path;
		Set<QName> aspects;
		Map<QName, Serializable> props;
		NodeRef primaryParent;
		Set<NodeRef> secondaryParents;

		ImportObjectImpl(QName type, QName assocName, QName assocType, Set<QName> aspects,
				Map<QName, Serializable> props, NodeRef primaryParent, List<String> path) {
			this.type = type;
			this.assocType = assocType;
			this.assocName = assocName;
			this.aspects = aspects;
			this.props = props;
			this.primaryParent = primaryParent;
			this.secondaryParents = new HashSet<NodeRef>();
			this.path = path;
		}

		@Override
		public String toString() {
			JSONObject obj = new JSONObject();
			try {
				obj.put("Path", path);
				obj.put("Type", type);
				obj.put("AssocName", assocName);
				obj.put("Aspects", aspects);
				obj.put("Props", props);
				obj.put("PrimaryParent", primaryParent);

				return obj.toString(2);
			} catch (JSONException e) {
				logger.error("Trouble converting to string", e);
				return "Bad JSON for: " + super.toString();
			}
		}

		@Override
		public QName getType() {
			return this.type;
		}

		@Override
		public QName getAssocName() {
			return this.assocName;
		}

		@Override
		public QName getAssocType() {
			return this.assocType;
		}

		@Override
		public Set<QName> getAspects() {
			return this.aspects;
		}

		@Override
		public Map<QName, Serializable> getProps() {
			return this.props;
		}

		@Override
		public NodeRef getPrimaryParent() {
			return this.primaryParent;
		}

		@Override
		public void setType(QName type) {
			this.type = type;
		}

		@Override
		public void setAssocName(QName assocName) {
			this.assocName = assocName;
		}

		@Override
		public void setAssocType(QName assocType) {
			this.assocType = assocType;
		}

		@Override
		public void setPrimaryParent(NodeRef primaryParent) {
			this.primaryParent = primaryParent;
		}

		@Override
		public void setAspects(Set<QName> aspects) {
			this.aspects = aspects;
		}

		@Override
		public void setProps(Map<QName, Serializable> props) {
			this.props = props;
		}

		@Override
		public void addSecondaryParent(NodeRef secondaryParent) {
			this.secondaryParents.add(secondaryParent);
		}

		@Override
		public Set<NodeRef> getSecondaryParents() {
			return this.secondaryParents;
		}

		@Override
		public NodeRef getParentFromPath(NodeRef defaultParent) {
			return getParentFromPath(defaultParent, false);
		}

		@Override
		public NodeRef getParentFromPath(final NodeRef defaultParent, boolean autoCreate) {
			if (this.path == null) {
				return null;
			}
			if (this.path.isEmpty()) {
				return defaultParent;
			}
			if (!autoCreate) {
				NodeRef retval = defaultParent;
				for (String name : this.path) {
					retval = folderHierarchyHelper.getFolder(retval, name, false);
					if (retval == null) {
						return null;
					}
				}
				return retval;

			}
			return _instance.txnHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {

				@Override
				public NodeRef execute() throws Throwable {
					return folderHierarchyHelper.getFolder(defaultParent, path, true);
				}

			}, false, true);

		}

		@Override
		public List<String> getPath() {
			return path;
		}

		@Override
		public void setPath(List<String> path) {
			this.path = path;
		}

	}

	private static class VersionedImportObjectImpl extends ImportObjectImpl implements VersionedImportObject {

		String versionLabel;
		String versionComment;

		VersionedImportObjectImpl(QName type, QName assocName, QName assocType, Set<QName> aspects,
				Map<QName, Serializable> props, NodeRef primaryParent, List<String> path, String versionLabel,
				String versionComment) {
			super(type, assocName, assocType, aspects, props, primaryParent, path);
			this.versionComment = versionComment;
			this.versionLabel = versionLabel;
		}

		@Override
		public String getVersionLabel() {
			return versionLabel;
		}

		@Override
		public String getVersionComment() {
			return versionComment;
		}

	}

	private static class VersionHistoryImpl implements VersionHistory {

		QName type;
		QName assocName;
		QName assocType;
		List<String> path;
		NodeRef primaryParent;
		Set<NodeRef> secondaryParents;
		List<VersionedImportObject> versionedObjects;

		VersionHistoryImpl(QName type, QName assocName, QName assocType, NodeRef primaryParent, List<String> path,
				List<VersionedImportObject> versionedObjects) {
			this.type = type;
			this.assocType = assocType;
			this.assocName = assocName;
			this.primaryParent = primaryParent;
			this.secondaryParents = new HashSet<NodeRef>();
			this.path = path;
			this.versionedObjects = versionedObjects;
		}

		@Override
		public QName getType() {
			return type;
		}

		@Override
		public List<String> getPath() {
			return path;
		}

		@Override
		public QName getAssocName() {
			return assocName;
		}

		@Override
		public QName getAssocType() {
			return assocType;
		}

		@Override
		public NodeRef getParentFromPath(NodeRef defaultParent) {
			return getParentFromPath(defaultParent, false);
		}

		@Override
		public NodeRef getParentFromPath(final NodeRef defaultParent, boolean autoCreate) {
			if (this.path == null) {
				return null;
			}
			if (this.path.isEmpty()) {
				return defaultParent;
			}
			if (!autoCreate) {
				NodeRef retval = defaultParent;
				for (String name : this.path) {
					retval = folderHierarchyHelper.getFolder(retval, name, false);
					if (retval == null) {
						return null;
					}
				}
				return retval;

			}
			return _instance.txnHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {

				@Override
				public NodeRef execute() throws Throwable {
					return folderHierarchyHelper.getFolder(defaultParent, path, true);
				}

			}, false, true);

		}

		@Override
		public NodeRef getPrimaryParent() {
			return primaryParent;
		}

		@Override
		public Set<NodeRef> getSecondaryParents() {
			return secondaryParents;
		}

		@Override
		public void setPrimaryParent(NodeRef primaryParent) {
			this.primaryParent = primaryParent;
		}

		@Override
		public void addSecondaryParent(NodeRef secondaryParent) {
			this.secondaryParents.add(secondaryParent);
		}

		@Override
		public void setPath(List<String> path) {
			this.path = path;
		}

		@Override
		public List<VersionedImportObject> getHistory() {
			return versionedObjects;
		}

	}

	private String getFileNameFromUrl(String url) {
		File file = new File(url);
		String filename = file.getName();
		return filename;
	}

	private long getContentLength(InputStream is) {
		try {
			return IOUtils.copy(is, new NullOutputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean checkContentUrl(String url) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Reader for " + url);
		}
		if (url == null) {
			return false;
		}
		return contentUrlValidator.checkContentUrl(url);
	}

	// TODO: Improve performance
	public ContentDataObject getContentDataObjectFromUrl(String iUrl) {
		String[] urlParts = iUrl.split("\\|");
		String url = urlParts[0];
		String mimeType = (urlParts.length > 1) ? urlParts[1] : null;
		String filename = getFileNameFromUrl(url);
		if (checkContentUrl(url)) {
			ContentReader cr = contentService.getRawReader(url);
			InputStream is = cr.getContentInputStream();
			long size = getContentLength(is);
			ContentDataObject cdo = new ContentDataObjectImpl(buildContentProperty(url, filename, is, size, mimeType));
			try {
				is.close();
			} catch (IOException e) {
				logger.error("Issue closing " + url, e);
			}
			return cdo;
		}
		return new MissingContentDataObjectImpl(url);

	}

	public ImportObject getImportObject(QName type, Map<QName, Serializable> props, NodeRef primaryParent) {
		return getImportObject(type, null, props, primaryParent);
	}

	public ImportObject getImportObject(QName type, Set<QName> aspects, Map<QName, Serializable> props,
			NodeRef primaryParent) {
		String name = (String) props.get(ContentModel.PROP_NAME);
		QName assocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
		return getImportObject(type, assocName, ContentModel.ASSOC_CONTAINS, aspects, props, primaryParent);
	}

	public ImportObject getImportObject(QName type, QName assocName, QName assocType, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent) {
		return new ImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, null);
	}

	public ImportObject getImportObject(QName type, QName assocName, QName assocType, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent, List<String> path) {
		return new ImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, path);
	}

	// Use this for object that only need to create an ImportObject
	public static ImportObject createImportObject(QName type, Map<QName, Serializable> props, NodeRef primaryParent) {
		return createImportObject(type, null, props, primaryParent);
	}

	public static ImportObject createImportObject(QName type, Set<QName> aspects, Map<QName, Serializable> props,
			NodeRef primaryParent) {
		return createImportObject(type, aspects, props, primaryParent, null);
	}

	public static ImportObject createImportObject(QName type, Set<QName> aspects, Map<QName, Serializable> props,
			NodeRef primaryParent, List<String> path) {
		String name = (String) props.get(ContentModel.PROP_NAME);
		if (name == null) {
			logger.error("No CM NAME");
			name = GUID.generate();
		}
		QName assocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
		return createImportObject(type, assocName, ContentModel.ASSOC_CONTAINS, aspects, props, primaryParent, path);
	}

	public static ImportObject createImportObject(QName type, QName assocName, QName assocType, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent) {
		return new ImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, null);
	}

	public static ImportObject createImportObject(QName type, QName assocName, QName assocType, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent, List<String> path) {
		return new ImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, path);
	}

	public static VersionedImportObject createVersionedImportObject(QName type, QName assocName, QName assocType,
			Set<QName> aspects, Map<QName, Serializable> props, NodeRef primaryParent, String versionLabel,
			String versionComment) {
		return new VersionedImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, null,
				versionLabel, versionComment);
	}

	public static VersionedImportObject createVersionedImportObject(QName type, QName assocName, QName assocType,
			Set<QName> aspects, Map<QName, Serializable> props, NodeRef primaryParent, List<String> path,
			String versionLabel, String versionComment) {
		return new VersionedImportObjectImpl(type, assocName, assocType, aspects, props, primaryParent, path,
				versionLabel, versionComment);
	}

	public static VersionedImportObject createVersionedImportObject(QName type, Map<QName, Serializable> props,
			NodeRef primaryParent, String versionLabel, String versionComment) {
		return createVersionedImportObject(type, null, props, primaryParent, versionLabel, versionComment);
	}

	public static VersionedImportObject createVersionedImportObject(QName type, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent, String versionLabel, String versionComment) {
		return createVersionedImportObject(type, aspects, props, primaryParent, null, versionLabel, versionComment);
	}

	public static VersionedImportObject createVersionedImportObject(QName type, Set<QName> aspects,
			Map<QName, Serializable> props, NodeRef primaryParent, List<String> path, String versionLabel,
			String versionComment) {
		String name = (String) props.get(ContentModel.PROP_NAME);
		if (name == null) {
			logger.error("No CM NAME");
			name = GUID.generate();
		}
		QName assocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
		return createVersionedImportObject(type, assocName, ContentModel.ASSOC_CONTAINS, aspects, props, primaryParent,
				path, versionLabel, versionComment);
	}

	public static VersionHistory createVersionHistory(QName type, QName assocName, QName assocType,
			NodeRef primaryParent, List<VersionedImportObject> versionedObjects) {
		return new VersionHistoryImpl(type, assocName, assocType, primaryParent, null, versionedObjects);
	}

	public static VersionHistory createVersionHistory(QName type, QName assocName, QName assocType,
			NodeRef primaryParent, List<String> path, List<VersionedImportObject> versionedObjects) {
		return new VersionHistoryImpl(type, assocName, assocType, primaryParent, path, versionedObjects);
	}

	public static VersionHistory createVersionHistory(QName type, NodeRef primaryParent,
			List<VersionedImportObject> versionedObjects) {
		return createVersionHistory(type, primaryParent, null, versionedObjects);
	}

	public static VersionHistory createVersionHistory(QName type, NodeRef primaryParent, List<String> path,
			List<VersionedImportObject> versionedObjects) {
		String name = null;
		for (VersionedImportObject vio : versionedObjects) {
			name = (String) vio.getProps().get(ContentModel.PROP_NAME);
			if (name != null && !name.isEmpty()) {
				break;
			}
		}
		if (name == null) {
			logger.error("No CM NAME");
			name = GUID.generate();
		}
		QName assocName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
		return createVersionHistory(type, assocName, ContentModel.ASSOC_CONTAINS, primaryParent, path,
				versionedObjects);
	}

	public NodeRef mapInPlaceObject(NodeRef parent, BaseImportObject io) {
		if (io instanceof ImportObject) {
			return (mapInPlaceObject(parent, (ImportObject) io));
		} else if (io instanceof VersionHistory) {
			return (mapInPlaceObject(parent, (VersionHistory) io));
		} else {
			throw new RuntimeException("Unknown Import Object Type");
		}
	}

	private NodeRef mapInPlaceObject(final NodeRef parent, VersionHistory io) {
		// TODO: Beware of Long Version Histories
		if (logger.isTraceEnabled()) {
			logger.trace("Creating: " + io.getAssocName());
		}
		final NodeRef nodeRef = nodeService.createNode(parent, io.getAssocType(), io.getAssocName(), io.getType())
				.getChildRef();
		for (final VersionedImportObject vio : io.getHistory()) {
			if (logger.isTraceEnabled()) {
				logger.trace("  Adding Version: " + vio.getVersionLabel());
			}
			updateInPlaceObject(parent, nodeRef, vio);
			createLabeledVersion(nodeRef, vio.getVersionLabel(), vio.getVersionComment(), false);
		}
		return nodeRef;
	}

	private void updateInPlaceObject(NodeRef parent, NodeRef nodeRef, ImportObject io) {
		if (logger.isTraceEnabled()) {
			logger.trace("Loading " + io.toString());
		}
		Map<QName, Serializable> props = io.getProps();
		ArrayList<String> errors = new ArrayList<String>();
		for (QName name : new HashSet<QName>(props.keySet())) {
			Serializable prop = props.get(name);
			if (prop instanceof ContentDataObject) {
				ContentDataObject cdo = (ContentDataObject) prop;
				if (!cdo.hasContent()) {
					errors.add(generateErrorMessage(name, cdo.getContentUrl()));
					props.remove(name);
				} else {
					props.put(name, cdo.getContentProperty());
				}
			}
			if (ContentModel.PROP_NAME.equals(name)) {
				props.put(name, prop);
			}
		}

		try {
			// Add remove Aspect?
			nodeService.addProperties(nodeRef, io.getProps());
			if (io.getAspects() != null) {
				for (QName aspect : io.getAspects()) {
					nodeService.addAspect(nodeRef, aspect, null);
				}
			}
			if (!errors.isEmpty()) {
				nodeService.setProperty(nodeRef, BulkObjectMapperConstants.propMissingContentInfo(), errors);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("DONE");
			}
		} catch (Exception e) {
			logger.error("MAPPOBJECT-Other Error " + e.getMessage() + " Processing Object: "
					+ io.getProps().get(ContentModel.PROP_NAME) + " to " + io.getPrimaryParent(), e);
		}
	}

	private NodeRef mapInPlaceObject(NodeRef parent, ImportObject io) {
		if (logger.isTraceEnabled()) {
			logger.trace("Loading " + io.toString());
		}
		Map<QName, Serializable> props = io.getProps();
		ArrayList<String> errors = new ArrayList<String>();
		for (QName name : new HashSet<QName>(props.keySet())) {
			Serializable prop = props.get(name);
			if (prop instanceof ContentDataObject) {
				ContentDataObject cdo = (ContentDataObject) prop;
				if (!cdo.hasContent()) {
					errors.add(generateErrorMessage(name, cdo.getContentUrl()));
					props.remove(name);
				} else {
					props.put(name, cdo.getContentProperty());
				}
			}
			if (ContentModel.PROP_NAME.equals(name)) {
				props.put(name, prop);
			}
		}

		NodeRef retval;
		try {
			retval = nodeService.createNode(parent, io.getAssocType(), io.getAssocName(), io.getType(), io.getProps())
					.getChildRef();
			if (io.getAspects() != null) {
				for (QName aspect : io.getAspects()) {
					nodeService.addAspect(retval, aspect, null);
				}
			}
			if (io.getSecondaryParents() != null) {
				for (NodeRef secondaryParent : io.getSecondaryParents()) {
					nodeService.addChild(secondaryParent, retval, io.getAssocType(), io.getAssocName());
				}
			}
			if (!errors.isEmpty()) {
				nodeService.setProperty(retval, BulkObjectMapperConstants.propMissingContentInfo(), errors);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("DONE");
			}
			return retval;
		} catch (DuplicateChildNodeNameException | InvalidTypeException | InvalidAspectException
				| InvalidNodeRefException e) {
			// TODO Add Error Listener
			logger.error("MAPPOBJECT-Error " + e.getMessage() + " Processing Object: "
					+ io.getProps().get(ContentModel.PROP_NAME) + " to " + io.getPrimaryParent(), e);
			return null;
		} catch (Exception e) {
			logger.error("MAPPOBJECT-Other Error " + e.getMessage() + " Processing Object: "
					+ io.getProps().get(ContentModel.PROP_NAME) + " to " + io.getPrimaryParent(), e);
			return null;
		}

	}

	public BaseImportObject generateImportObject(ImportObjectGenerator gen) {
		return gen.generateObject();
	}

	public List<BaseImportObject> generateImportObjects(ImportObjectGenerator gen, int count) {
		List<BaseImportObject> list = new ArrayList<BaseImportObject>(count);
		while (count-- > 0) {
			list.add(gen.generateObject());
		}
		return list;
	}

	public void mapInPlaceObjects(final NodeRef parent, final List<BaseImportObject> ios) {
		mapInPlaceObjects(parent, ios, null);
	}

	public void mapInPlaceObjects(final NodeRef parent, final List<BaseImportObject> ios, JSONObject ctx) {
		bulkUpdater.threadedBulkUpdate(ios, new BulkUpdaterCallback<BaseImportObject>() {

			@Override
			public void executeUpdate(BaseImportObject item) {
				behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
				mapInPlaceObject(parent, item);
			}

		}, true, ctx);
	}

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios) {
		final String id = backgroundCache.newItem("Import Into: " + defaultParent);

		BackgroundCachedRunnable bcr = new BackgroundCachedRunnable() {
			BackgroundCacheItem cachedItem = backgroundCache.getItem(id);
			
			@Override
			public void run() {
				AuthenticationUtil.runAs(new RunAsWork<Object>() {
					public Object doWork() {
						cachedItem.run();
						mapInPlaceObjects(defaultParent, ios, cachedItem.getContext());
						// TODO add Terminated
						cachedItem.complete();
						backgroundCache.removeItem(id);
						return null;
					}
				}, cachedItem.getRunAsUser());
			}

			@Override
			public BackgroundCacheItem getCacheItem() {
				return cachedItem;
			}

		};

		backgroundRunner.process(bcr);

		return id;
	}

	public String mapInPlaceObjectsBG(final NodeRef defaultParent, final List<BaseImportObject> ios,
			final boolean checkParents, final ImportObjectPreProcessor iopp, final boolean autoCreate) {
		final String id = backgroundCache.newItem("Import Into: " + defaultParent);

		BackgroundCachedRunnable bcr = new BackgroundCachedRunnable() {
			BackgroundCacheItem cachedItem = backgroundCache.getItem(id);
		
			@Override
			public void run() {
				AuthenticationUtil.runAs(new RunAsWork<Object>() {
					public Object doWork() {
						cachedItem.run();
						mapInPlaceObjects(defaultParent, ios, checkParents, iopp, autoCreate, cachedItem.getContext());
						// TODO add Terminated
						cachedItem.complete();
						backgroundCache.removeItem(id);
						return null;
					}
				}, cachedItem.getRunAsUser());
			}

			@Override
			public BackgroundCacheItem getCacheItem() {
				return cachedItem;
			}

		};

		backgroundRunner.process(bcr);

		return id;
	}

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents,
			ImportObjectPreProcessor iopp, boolean autoCreate) {
		mapInPlaceObjects(defaultParent, ios, checkParents, iopp, autoCreate, null);
	}

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents,
			ImportObjectPreProcessor iopp, boolean autoCreate, JSONObject ctx) {
		if (!checkParents && iopp == null) {
			mapInPlaceObjects(defaultParent, ios, ctx);
		} else {
			Map<NodeRef, List<BaseImportObject>> ioBundles = new HashMap<NodeRef, List<BaseImportObject>>();
			for (BaseImportObject io : ios) {
				if (iopp != null) {
					iopp.preProcessImportObject(io);
				}
				NodeRef parent = io.getPrimaryParent();
				if (parent == null) {
					parent = io.getParentFromPath(defaultParent, autoCreate);
				}
				if (parent == null) {
					parent = defaultParent;
				}
				if (!ioBundles.containsKey(parent)) {
					ioBundles.put(parent, new ArrayList<BaseImportObject>());
				}
				ioBundles.get(parent).add(io);
			}
			for (NodeRef parent : ioBundles.keySet()) {
				mapInPlaceObjects(parent, ioBundles.get(parent), ctx);
			}
		}
	}

	public void mapInPlaceObjects(final NodeRef defaultParent, final List<BaseImportObject> ios, boolean checkParents,
			ImportObjectPreProcessor iopp) {
		mapInPlaceObjects(defaultParent, ios, checkParents, iopp, false);
	}

	public boolean checkContentUrl(NodeRef node, QName propertyName) {
		Serializable prop = nodeService.getProperty(node, propertyName);
		if (prop instanceof ContentData) {
			return checkContentUrl((ContentData) prop);
		} else {
			return false;
		}
	}

	public void tryToAttachContent(NodeRef nodeRef) {
		tryToAttachContent(nodeRef, null, null);
	}

	public void tryToAttachContent(NodeRef nodeRef, String contentUrl) {
		tryToAttachContent(nodeRef, null, contentUrl);
	}

	public void tryToAttachContent(NodeRef nodeRef, QName propQname, String contentUrl) {
		List<String> oldErrors = (List<String>) nodeService.getProperty(nodeRef,
				BulkObjectMapperConstants.propMissingContentInfo());
		if (oldErrors != null) {
			ArrayList<String> newErrors = new ArrayList<String>();
			for (String error : oldErrors) {
				String[] sections = error.split("\\|");
				QName propName = QName.createQName(sections[0]);
				String url = sections[1];
				if (logger.isDebugEnabled()) {
					logger.debug("Checking - Prop:" + propName + " URL:" + url);
				}
				if ((propQname == null || propQname.equals(propName))
						&& (contentUrl == null || contentUrl.equals(url))) {
					logger.debug(" --- reviewing");
					Serializable prop = nodeService.getProperty(nodeRef, propName);
					boolean hasContent = checkContentUrl(url);
					if (prop != null && prop instanceof ContentData && checkContentUrl((ContentData) prop)) {
						logger.debug("   --- content already there");
						; // Do Nothing -- we have content
					} else if (hasContent) {
						// TODO Fix This For Mime types
						ContentReader cr = contentService.getRawReader(url);
						InputStream is = cr.getContentInputStream();
						long size = getContentLength(is);
						ContentData cd = buildContentProperty(url, new File(url).getName(), is, size, null);
						nodeService.setProperty(nodeRef, propName, cd);
						logger.debug("   --- content found adding it");
					} else {
						newErrors.add(error);
						logger.debug("   --- still no contentt");
					}
				}
				logger.debug("--- done");
			}
			nodeService.removeAspect(nodeRef, BulkObjectMapperConstants.aspectMissingContent());
			if (!newErrors.isEmpty()) {
				nodeService.setProperty(nodeRef, BulkObjectMapperConstants.propMissingContentInfo(), newErrors);
			}
		}

	}

	// Handling Missing Content
	boolean checkContentUrl(ContentData contentData) {
		if (!ContentData.hasContent(contentData))
			return false;
		return contentService.getRawReader(contentData.getContentUrl()).exists();
	}

	public void validateContentUrls(NodeRef node) {
		ArrayList<String> errors = new ArrayList<String>();
		Map<QName, Serializable> props = nodeService.getProperties(node);

		for (QName name : props.keySet()) {
			Serializable prop = props.get(name);
			if (prop instanceof ContentData) {
				if (!checkContentUrl((ContentData) prop)) {
					errors.add(flagMissingContent(node, name));
				}
			}
			if (prop instanceof ContentDataObject) {
				if (!((ContentDataObject) prop).hasContent()) {
					errors.add(flagMissingContent(node, name));
				}
			}
		}

		if (!errors.isEmpty()) {
			nodeService.setProperty(node, BulkObjectMapperConstants.propMissingContentInfo(), errors);
		}
	}

	String flagMissingContent(NodeRef node, QName propertyName) {
		Serializable prop = nodeService.getProperty(node, propertyName);
		String contentUrl = "*** MISSING CONTENT URL ***";
		if (prop instanceof ContentData) {
			contentUrl = ((ContentData) prop).getContentUrl();
		} else if (prop instanceof ContentDataObject) {
			contentUrl = ((ContentDataObject) prop).getContentUrl();
		}
		nodeService.removeProperty(node, propertyName);

		return generateErrorMessage(propertyName, contentUrl);
	}

	String generateErrorMessage(QName propertyName, String contentUrl) {
		return propertyName.toString() + "|" + contentUrl;
	}

	/**
	 * This method does the magic of constructing the content URL for "in-place"
	 * content.
	 * 
	 * @param iMimeType
	 * 
	 * @param mimeTypeService The Alfresco MimetypeService <i>(must not be
	 *                        null)</i>.
	 * @param contentStore    The content store Alfresco is configured to use
	 *                        <i>(must not be null)</i>.
	 * @param contentFile     The content file to build a content URL for <i>(must
	 *                        not be null)</i>.
	 * @return The constructed <code>ContentData</code>, or null if the contentFile
	 *         cannot be in-place imported for any reason.
	 */
	private ContentData buildContentProperty(String contentUrl, String filename, InputStream contentStream,
			long fileLength, String iMimeType) {
		ContentData result = null;
		// If the resulting content URL would be too long, we can't in-place import
		if (contentUrl.length() <= MAX_CONTENT_URL_LENGTH) {
			final String mimeType = (iMimeType == null) ? mimetypeService.guessMimetype(filename) : iMimeType;
			final String encoding = guessEncoding(contentStream, mimeType);

			result = new ContentData(contentUrl, mimeType, fileLength, encoding);
		}

		return (result);
	}

	/**
	 * Attempt to guess the encoding of a text file , falling back to
	 * {@link #DEFAULT_TEXT_ENCODING}.
	 *
	 * @param mimeTypeService The Alfresco MimetypeService <i>(must not be
	 *                        null)</i>.
	 * @param file            The {@link java.io.File} to test <i>(must not be
	 *                        null)</i>.
	 * @param mimeType        The file MIME type. Used to first distinguish between
	 *                        binary and text files <i>(must not be null)</i>.
	 * @return The text encoding as a {@link String}.
	 */
	private String guessEncoding(InputStream is, String mimeType) {
		String result = DEFAULT_TEXT_ENCODING;
		ContentCharsetFinder charsetFinder = mimetypeService.getContentCharsetFinder();

		if (mimetypeService.isText(mimeType)) {
			try {
				result = charsetFinder.getCharset(is, mimeType).name();
			} finally {
				IOUtils.closeQuietly(is);
			}
		}

		return (result);
	}

	private JSONObject getJSONfromPropertyDefinition(PropertyDefinition pd) {
		JSONObject pdobj = new JSONObject();
		try {
			pdobj.put("Name", pd.getName().getPrefixString());
			pdobj.put("Type", pd.getDataType().getName().getPrefixString());
			pdobj.put("Indexed", pd.isIndexed());
			pdobj.put("Mandatory", pd.isMandatory());
			pdobj.put("Faceted", pd.getFacetable().toString());
		} catch (JSONException e) {
			logger.error("Error getting property informtion for: " + pd.getName().getPrefixString(), e);
		}
		return pdobj;
	}

	public JSONObject getTypeInformation(QName type) {
		ClassDefinition cd = dictionaryService.getClass(type);
		Map<QName, PropertyDefinition> pdmap = cd.getProperties();
		List<AspectDefinition> alist = cd.getDefaultAspects(true);
		Map<QName, ChildAssociationDefinition> camap = cd.getChildAssociations();
		Map<QName, AssociationDefinition> pamap = cd.getAssociations();
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", cd.getName().getPrefixString());
			JSONObject props = new JSONObject();
			for (QName pdqn : pdmap.keySet()) {
				PropertyDefinition pd = pdmap.get(pdqn);
				JSONObject pdobj = getJSONfromPropertyDefinition(pd);
				props.put(pdqn.getPrefixString(), pdobj);
			}
			JSONArray aspects = new JSONArray();
			for (AspectDefinition ad : alist) {
				Map<QName, PropertyDefinition> apdmap = ad.getProperties();
				for (QName apdqn : apdmap.keySet()) {
					PropertyDefinition pd = apdmap.get(apdqn);
					JSONObject pdobj = getJSONfromPropertyDefinition(pd);
					props.put(apdqn.getPrefixString(), pdobj);
				}
				aspects.put(ad.getName().getPrefixString());
			}
			obj.put("props", props);
			obj.put("aspects", aspects);
		} catch (JSONException e) {
			logger.error("Error getting type informtion for: " + type, e);
		}
		return obj;
	}

	public void modifyCreateDate(NodeRef nodeRef, Date createDate) {
		Map<NodeRef, Date> map = new HashMap<NodeRef, Date>();
		map.put(nodeRef, createDate);
		modifyCreateDates(map);
	}

	public void modifyCreateDates(final Map<NodeRef, Date> updateList) {
		txnHelper.doInTransaction(new RetryingTransactionCallback<Object>() {
			@Override
			public Object execute() throws Exception {
				// Disable the auditable aspect's behaviours for this transaction, to allow
				// creation & modification dates to be set
				behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);

				for (NodeRef nodeRef : updateList.keySet()) {
					Date createDate = updateList.get(nodeRef);
					nodeService.setProperty(nodeRef, ContentModel.PROP_CREATED, createDate);
				}
				return (null);
			}
		}, false, // read only flag, false=R/W txn
				true);
	}

	public List<NodeRef> getMatchingNodes(NodeRef container, final NodeMatchCallback cb) {
		final List<NodeRef> matchList = new ArrayList<NodeRef>();
		Callback ffbb = new Callback() {

			@Override
			public void doWork(NodeRef node) {
				if (cb.matches(node)) {
					matchList.add(node);
				}
			}

			@Override
			public void startWalk(NodeRef node) {
				// TODO Auto-generated method stub

			}

		};
		fileFolderTreeWalker.walk(container, ffbb);

		return matchList;
	}

	void bulkUpdate(List<NodeRef> nodes, BulkUpdaterCallback<NodeRef> cb, int batchSize, int numThreads,
			boolean runAsSystem) {
		if (numThreads <= 1 && !runAsSystem) {
			bulkUpdater.bulkUpdate(nodes, cb, batchSize);
		} else if (numThreads <= 1) {
			bulkUpdater.threadedBulkUpdate(nodes, cb, 1, runAsSystem);
		} else {
			bulkUpdater.threadedBulkUpdate(nodes, cb, numThreads, batchSize, runAsSystem);
		}
	}

	public void bulkUpdate(SearchParameters sp, BulkUpdaterCallback<NodeRef> bulkUpdaterCallback, int batchSize,
			int numThreads, boolean runAsSystem) {
		List<NodeRef> nodes = searchService.query(sp).getNodeRefs();
		bulkUpdate(nodes, bulkUpdaterCallback, batchSize, numThreads, runAsSystem);
	}

	public void bulkUpdate(NodeRef container, NodeMatchCallback nodeMatchCallback,
			BulkUpdaterCallback<NodeRef> bulkUpdaterCallback, int batchSize, int numThreads, boolean runAsSystem) {
		bulkUpdate(getMatchingNodes(container, nodeMatchCallback), bulkUpdaterCallback, batchSize, numThreads,
				runAsSystem);
	}

	public static BulkObjectMapperComponent getInstance() {
		return _instance;
	}

	public void registerCommand(BulkUpdateCommand cmd) {
		commandRegistry.put(cmd.commandName(), cmd);
	}

	public void registerFilter(BulkUpdateFilter filter) {
		filterRegistry.put(filter.filterName(), filter);
	}

	public void disableAuditableBehaviour() {
		disableBehaviour(ContentModel.ASPECT_AUDITABLE);
	}

	public void disableBehaviour(QName clz) {
		_instance.behaviourFilter.disableBehaviour(clz);
	}

	public void bulkExecuteCommand(JSONObject cmdObj) {
		bulkExecuteCommand(cmdObj, null);
	}

	public void bulkExecuteCommand(JSONObject cmdObj, BackgroundCacheItem bci) {
		JSONArray list = null;
		JSONObject data = null;
		BulkUpdateCommand cmd;
		try {
			cmd = this.commandRegistry.get(cmdObj.get(FIELD_COMMAND));
			if (cmd == null) {
				throw new NullPointerException("Command Not found: " + cmdObj.get(FIELD_COMMAND));
			}
		} catch (JSONException | NullPointerException e) {
			logger.error("Trouble Finding Command", e);
			throw (new RuntimeException(e));
		}
		data = cmdObj.optJSONObject(FIELD_DATA);
		list = cmdObj.optJSONArray(FIELD_LIST);
		bulkExecuteCommand(cmd.parseJson(data, list, (bci == null) ? null : bci.getContext()), cmd, bci);
	}

	public void bulkExecuteCommand(Map<NodeRef, JSONObject> updateList, BulkUpdateCommand cmd,
			BackgroundCacheItem bci) {
		bulkExecuteCommand(updateList, cmd, 100, bci);
	}

	public void bulkExecuteCommand(final Map<NodeRef, JSONObject> updateList, final BulkUpdateCommand cmd,
			final int batch_size, final BackgroundCacheItem bci) {
		class CommandPackage {
			public NodeRef node;
			public JSONObject obj;

			public CommandPackage(NodeRef node, JSONObject obj) {
				this.node = node;
				this.obj = obj;
			}
		}
		final List<CommandPackage> cmdPkgs = new ArrayList<CommandPackage>();
		for (NodeRef node : updateList.keySet()) {
			CommandPackage cmdPkg = new CommandPackage(node, updateList.get(node));
			cmdPkgs.add(cmdPkg);
		}
		int startIdx = 0;
		int pCount = 0;
		final JSONObject ctx = (bci == null) ? new JSONObject() : bci.getContext();
		logger.debug("START Command: " + cmd.commandName());
		cmd.preExec(ctx, updateList);
		/*
		 * (if (bci != null) { bci.setContext(ctx); }
		 */
		for (startIdx = 0; startIdx < cmdPkgs.size(); startIdx += batch_size) {
			final int myStartIdx = startIdx;
			pCount = serviceRegistry.getRetryingTransactionHelper()
					.doInTransaction(new RetryingTransactionCallback<Integer>() {

						@Override
						public Integer execute() throws Throwable {
							int i;
							cmd.preTxn(ctx);
							if (bci != null) {
								bci.setContext(ctx);
							}
							for (i = myStartIdx; i < Math.min(myStartIdx + batch_size, cmdPkgs.size()); i++) {
								cmd.workUnit(cmdPkgs.get(i).node, cmdPkgs.get(i).obj, ctx);
							}
							cmd.postTxn(ctx);
							if (bci != null) {
								bci.setContext(ctx);
							}
							return i;
						}
					}, false, true);
		}
		cmd.postExec(ctx);
		if (bci != null) {
			bci.setContext(ctx);
		}
		logger.debug("FINISH Command: " + cmd.commandName());
	}

	public String bulkExecuteFilter(JSONObject config, InputStream input) {
		BulkUpdateFilter filter;
		try {
			filter = this.filterRegistry.get(config.get(FIELD_FILTER));
			if (filter == null) {
				throw new NullPointerException("FILTER Not found: " + config.get(FIELD_FILTER));
			}
		} catch (JSONException | NullPointerException e) {
			logger.error("Trouble Finding Filter", e);
			throw (new RuntimeException(e));
		}
		logger.info("Running " + filter.filterName());
		return filter.filter(config, input);
	}

	public String bulkExecuteCommandBG(final JSONObject cmdObj) {
		final String id = backgroundCache.newItem(cmdObj.optString(FIELD_COMMAND));

		BackgroundCachedRunnable bcr = new BackgroundCachedRunnable() {
			BackgroundCacheItem cachedItem = backgroundCache.getItem(id);
			
			@Override
			public void run() {
				AuthenticationUtil.runAs(new RunAsWork<Object>() {
					public Object doWork() {
						cachedItem.run();
						bulkExecuteCommand(cmdObj, cachedItem);
						// TODO add Terminated
						cachedItem.complete();
						backgroundCache.removeItem(id);
						return null;
					}
				}, cachedItem.getRunAsUser());
			}

			@Override
			public BackgroundCacheItem getCacheItem() {
				return cachedItem;
			}

		};

		backgroundRunner.process(bcr);

		return id;
	}

	public BackgroundCacheItem getBackgroundCacheItem(String id) {
		return backgroundCache.getItem(id);
	}

	public Set<String> getBackgroundCacheItemIds() {
		return backgroundCache.getBackgroundCacheItemIds();

	}
	
	public String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception {
		return backgroundCache.getAllBackgroundItems();
	}
	
	public String removeObjectsFromPersistance() {
		return backgroundCache.removeObjectsFromPersistance();
	}

	public void registerImportRootResolver(ImportRootResolver irr) {
		importRootResolverRegistry.add(irr);
	}

	public NodeRef resolveImportRoot(Map<String, String> params) {
		for (ImportRootResolver irr : importRootResolverRegistry) {
			NodeRef result = irr.resolve(params);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	public Version createLabeledVersion(NodeRef nodeRef, String versionLabel, String history, boolean majorVersion) {
		Map<String, Serializable> props = new HashMap<String, Serializable>(2, 1.0f);
		props.put(Version.PROP_DESCRIPTION, history);
		props.put(VersionModel.PROP_VERSION_TYPE, majorVersion ? VersionType.MAJOR : VersionType.MINOR);
		if (versionLabel != null) {
			props.put(BulkObjectMapperConstants.OVERRIDE_VERSION_LABEL, versionLabel);
		}
		return versionService.createVersion(nodeRef, props);
	}
}
