package  org.alfresco.consulting.util.tree_walker.impl;

import java.util.Stack;

import org.alfresco.consulting.util.tree_walker.TreeWalker;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TreeWalkerHelper implements TreeWalker {
	ServiceRegistry serviceRegistry;
	NodeService nodeService;
	DictionaryService dictionaryService;


	private static Log logger = LogFactory.getLog(TreeWalkerHelper.class);

	public static class TruePattern implements QNamePattern {

		@Override
		public boolean isMatch(QName qname) {
            logger.debug("TruePattern " + qname.toString());
			return true;
		}
    }

 

	@Override
	public void walk(NodeRef contextNodeRef, Navigator nav, Callback cb) {
        Stack<NodeRef> toSearch = new Stack<NodeRef>();
        toSearch.push(contextNodeRef);
        
        // Now we need to walk down the folders.
        int containerCount=0;
        int itemCount=0;
        cb.startWalk(contextNodeRef);
        while(!toSearch.empty())
        {
            NodeRef currentDir = toSearch.pop();
            Navigator myNav=nav.getNavigator(currentDir);

            logger.debug("Processing " + currentDir.toString());
            while (myNav.hasNext())
            {
            	NodeRef currentObject = myNav.next();
            	boolean isContainer =myNav.isContainer(currentObject);
            	if (isContainer) {
                    logger.debug("Pushing " + currentObject.toString());
                	toSearch.push(currentObject);
            	}
                logger.debug("Working " + currentObject.toString());
            	cb.doWork(currentObject);
            	if (isContainer) {
            		containerCount++;
            	} else {
            		itemCount++;
            	}
            }
                
        }
        if (cb instanceof CallbackE) {
        	((CallbackE) cb).endWalk();
        }
        
        logger.debug("Processed " + itemCount + " items in " + containerCount + " containers");

	}
	

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.dictionaryService = serviceRegistry.getDictionaryService(); 
	}
}
