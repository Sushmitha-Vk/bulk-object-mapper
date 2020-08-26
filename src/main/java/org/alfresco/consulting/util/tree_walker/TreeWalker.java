package org.alfresco.consulting.util.tree_walker;

import org.alfresco.service.cmr.repository.NodeRef;

public interface TreeWalker {
        public interface Navigator {
                Navigator getNavigator(NodeRef ref); // Needed? -- Factory Method Needed?
                NodeRef next();
                boolean hasNext();
                boolean isContainer(NodeRef ref);
        }
        public interface Callback {
            void doWork(NodeRef node);
            void startWalk(NodeRef node);
        }
        public interface CallbackE extends Callback{
            void endWalk();
        }
        void walk(NodeRef contextNodeRef,Navigator nav,Callback cb);
}
