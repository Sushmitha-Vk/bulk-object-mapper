package org.alfresco.consulting.util.folder_hierarchy.impl;


import java.util.List;


import org.alfresco.consulting.util.folder_hierarchy.FolderHierarchyHelper;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

public class FolderHierarchyHelperImpl implements FolderHierarchyHelper {
	
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = this.serviceRegistry.getNodeService();
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name) {
		return getFolder(folder,name,ContentModel.TYPE_FOLDER);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType) {
		return getFolder(folder,name,type,assocType,null);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			FolderBootstrapCallback cb) {
		return getFolder(folder,name,ContentModel.TYPE_FOLDER,cb);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, FolderBootstrapCallback cb) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,cb);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType, FolderBootstrapCallback cb) {
		return getFolder(folder,name,type,assocType,cb,true);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			boolean createFolder) {
		return getFolder(folder,name,ContentModel.TYPE_FOLDER,createFolder);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, boolean createFolder) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,createFolder);

	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType, boolean createFolder) {
		return getFolder(folder,name,type,assocType,null,createFolder);

	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			FolderBootstrapCallback cb, boolean createFolder) {
		return getFolder(folder,name,ContentModel.TYPE_FOLDER,cb,createFolder);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, FolderBootstrapCallback cb, boolean createFolder) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,cb,createFolder);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType, FolderBootstrapCallback cb,
			boolean createFolder) {
		return getFolder(folder,name,type,assocType,cb,createFolder,null,FolderBootstrapCallback.SINGLE_FOLDER);
	}

	private NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType, FolderBootstrapCallback cb,
			boolean createFolder,List<String> names,int idx) {
		if (folder == null) {
			return null;
		}
        NodeRef result = nodeService.getChildByName(folder, assocType,name);
		if (result == null  && createFolder) {
			result = nodeService.createNode(folder, assocType, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name), type).getChildRef();
			nodeService.setProperty(result, ContentModel.PROP_NAME, name);
			if (cb != null) {
				cb.bootstrap(result,names,idx);
			}
		}
		return result;
	}
	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			boolean createFolder, boolean runAsSystem) {
		return getFolder(folder, name,ContentModel.TYPE_FOLDER,createFolder,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, boolean createFolder, boolean runAsSystem) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,createFolder,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, QName assocType, boolean createFolder,
			boolean runAsSystem) {
		return getFolder(folder,name,type,assocType,null,createFolder,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			FolderBootstrapCallback cb, boolean createFolder,
			boolean runAsSystem) {
		return getFolder(folder,name,ContentModel.TYPE_FOLDER,cb,createFolder,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, String name,
			QName type, FolderBootstrapCallback cb, boolean createFolder,
			boolean runAsSystem) {
		return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,cb,createFolder,runAsSystem);
	}

	@Override
	public NodeRef getFolder(final NodeRef folder, final String name,
			final QName type, final QName assocType, final FolderBootstrapCallback cb,
			final boolean createFolder, final boolean runAsSystem) {
		if (runAsSystem) {
			return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {

				@Override
				public NodeRef doWork() throws Exception {
					return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,cb,createFolder);
				}
				
			});
		} else {
			return getFolder(folder,name,type,ContentModel.ASSOC_CONTAINS,cb,createFolder);
		}
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names) {
		return getFolder(folder,names, false);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names, QName type,
			QName assocType) {
		return getFolder(folder,names, type, assocType, false);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names,
			FolderBootstrapCallback cb) {
		return getFolder(folder,names,  cb, false);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names, QName type,
			QName assocType, FolderBootstrapCallback cb) {
		return getFolder(folder,names, type, assocType, cb, false);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names, boolean runAsSystem) {
		// TODO Auto-generated method stub
		return getFolder(folder,names, null,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names, QName type,
			QName assocType, boolean runAsSystem) {
		return getFolder(folder,names, type, assocType,null,runAsSystem);
	}

	@Override
	public NodeRef getFolder(NodeRef folder, List<String> names,
			FolderBootstrapCallback cb, boolean runAsSystem) {
		return getFolder(folder,names, ContentModel.TYPE_FOLDER, ContentModel.ASSOC_CONTAINS,cb,runAsSystem);
	}

	@Override
	public NodeRef getFolder(final NodeRef folder, final List<String> names, final QName type,
			final QName assocType, final FolderBootstrapCallback cb, final boolean runAsSystem) {
		if (runAsSystem) {
			return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {

				@Override
				public NodeRef doWork() throws Exception {
					return getFolder(folder,names,type,assocType,cb,false);
				}
				
			});
			
		} else {
		NodeRef retval=folder;
			int idx=0;
			for (String name : names) {
				retval = getFolder(retval,name,type,assocType,cb,true,names,idx);
				idx++;
			}
			return retval;
		}
	}

}
