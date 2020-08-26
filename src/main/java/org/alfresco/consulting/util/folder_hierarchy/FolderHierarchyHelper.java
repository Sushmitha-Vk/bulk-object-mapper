package org.alfresco.consulting.util.folder_hierarchy;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface FolderHierarchyHelper  {
	public interface FolderBootstrapCallback {
		public static final int SINGLE_FOLDER=-1;
		void bootstrap(NodeRef folder,List<String> name,int idx);
	}
	// All of these create the folder by default
	public NodeRef getFolder(NodeRef folder,String name);
	public NodeRef getFolder(NodeRef folder,String name,QName type);	
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType);	
	public NodeRef getFolder(NodeRef folder,String name,FolderBootstrapCallback cb);
	public NodeRef getFolder(NodeRef folder,String name,QName type,FolderBootstrapCallback cb);
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType,FolderBootstrapCallback cb);
	
	// All of these runAs the current user by default
	public NodeRef getFolder(NodeRef folder,String name,boolean createFolder);
	public NodeRef getFolder(NodeRef folder,String name,QName type,boolean createFolder);	
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType,boolean createFolder);	
	public NodeRef getFolder(NodeRef folder,String name,FolderBootstrapCallback cb,boolean createFolder);
	public NodeRef getFolder(NodeRef folder,String name,QName type,FolderBootstrapCallback cb,boolean createFolder);
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType,FolderBootstrapCallback cb,boolean createFolder);
	
	// These let you specify whether or not the folder is to be created and if it is to be created as the system user
	public NodeRef getFolder(NodeRef folder,String name,boolean createFolder,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,String name,QName type,boolean createFolder,boolean runAsSystem);	
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType,boolean createFolder,boolean runAsSystem);	
	public NodeRef getFolder(NodeRef folder,String name,FolderBootstrapCallback cb,boolean createFolder,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,String name,QName type,FolderBootstrapCallback cb,boolean createFolder,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,String name,QName type,QName assocType,FolderBootstrapCallback cb,boolean createFolder,boolean runAsSystem);

	public NodeRef getFolder(NodeRef folder,List<String> names);
	public NodeRef getFolder(NodeRef folder,List<String> names,QName type,QName assocType);
	public NodeRef getFolder(NodeRef folder,List<String> names,FolderBootstrapCallback cb);
	public NodeRef getFolder(NodeRef folder,List<String> names,QName type,QName assocType,FolderBootstrapCallback cb);

	public NodeRef getFolder(NodeRef folder,List<String> names,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,List<String> names,QName type,QName assocType,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,List<String> names,FolderBootstrapCallback cb,boolean runAsSystem);
	public NodeRef getFolder(NodeRef folder,List<String> names,QName type,QName assocType,FolderBootstrapCallback cb,boolean runAsSystem);
}
