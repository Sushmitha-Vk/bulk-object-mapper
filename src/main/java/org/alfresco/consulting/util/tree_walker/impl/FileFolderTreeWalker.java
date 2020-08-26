package  org.alfresco.consulting.util.tree_walker.impl;

import java.util.Iterator;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class FileFolderTreeWalker extends TreeWalkerHelper {
	ServiceRegistry serviceRegistry;
	NodeService nodeService;
	DictionaryService dictionaryService;


	private static Log logger = LogFactory.getLog(FileFolderTreeWalker.class);
 
    private class FileNavigator implements Navigator {
    	
    	private  QNamePattern truePattern = new TruePattern();
    	private  QNamePattern fileFolderAssociationPattern = new FileFolderAssociationPattern();
        private class FileFolderAssociationPattern implements QNamePattern {

    		@Override
    		public boolean isMatch(QName qname) {
                logger.debug("SimpleAssociationPattern " + qname.toString());
    			if (ContentModel.ASSOC_CONTAINS.equals(qname)) {
    				logger.debug("OK");
    				return true;
    			}
    			return false;
    		}
        }
    	NodeRef ref;
    	Iterator<ChildAssociationRef> items = null;
    	//Iterator containerIterator = nodeService
    	FileNavigator(NodeRef ref) {
    		this.ref = ref;
    		items = nodeService.getChildAssocs(ref, fileFolderAssociationPattern, truePattern).iterator();
    	}
    	
		@Override
		public Navigator getNavigator(NodeRef ref) {

			return (ref == this.ref)?this:new FileNavigator(ref);
		}

		@Override
		public boolean hasNext() {
			return items.hasNext();
		}

		@Override
		public NodeRef next() {
			return items.next().getChildRef();
		}

		@Override
		public boolean isContainer(NodeRef ref) {
			return dictionaryService.isSubClass(nodeService.getType(ref), ContentModel.TYPE_FOLDER);
		}
    	
    }
	
	public void walk(NodeRef contextNodeRef, Callback cb) {
        logger.debug("Doing a FileFolder Walk");
		walk(contextNodeRef,new FileNavigator(contextNodeRef),cb);
	}

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.dictionaryService = serviceRegistry.getDictionaryService(); 
	}
}
