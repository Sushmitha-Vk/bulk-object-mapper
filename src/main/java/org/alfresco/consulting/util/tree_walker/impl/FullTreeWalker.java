package  org.alfresco.consulting.util.tree_walker.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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


public class FullTreeWalker extends TreeWalkerHelper {
	ServiceRegistry serviceRegistry;
	NodeService nodeService;
	DictionaryService dictionaryService;


	private static Log logger = LogFactory.getLog(FullTreeWalker.class);
 
    private class FullNavigator implements Navigator {
    	
    	private  QNamePattern truePattern = new TruePattern();
    	NodeRef ref;
    	Iterator<NodeRef> items = null;

    	//Iterator containerIterator = nodeService
    	FullNavigator(NodeRef ref) {
    		this.ref = ref;
    		List<ChildAssociationRef>  buffer = nodeService.getChildAssocs(ref, truePattern, truePattern);
    		List<NodeRef> nodeList  = new ArrayList<NodeRef>();
    		for (ChildAssociationRef assocRef : buffer) {
    			if (assocRef.isPrimary()) {
    				nodeList.add(assocRef.getChildRef());
    			}
    		}
    		items = nodeList.iterator();
    	}
    	
		@Override
		public Navigator getNavigator(NodeRef ref) {

			return (ref == this.ref)?this:new FullNavigator(ref);
		}

		@Override
		public boolean hasNext() {
			return items.hasNext();
		}

		@Override
		public NodeRef next() {
			return items.next();
		}

		@Override
		public boolean isContainer(NodeRef ref) {
			return nodeService.countChildAssocs(ref, true) > 0;
		}
    	
    }
	
	public void walk(NodeRef contextNodeRef, Callback cb) {
        logger.debug("Doing a Full Walk");
		walk(contextNodeRef,new FullNavigator(contextNodeRef),cb);
	}

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.dictionaryService = serviceRegistry.getDictionaryService(); 
	}
}
