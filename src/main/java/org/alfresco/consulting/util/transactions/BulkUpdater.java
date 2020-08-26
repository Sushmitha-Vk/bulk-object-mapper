package org.alfresco.consulting.util.transactions;

import java.util.List;

public interface BulkUpdater {
	public interface BulkUpdaterCallback<T> {
		public void executeUpdate(T item);
	}
	<T> int bulkUpdate(List<T> items,BulkUpdaterCallback<T> cb);
	<T> int bulkUpdate(List<T> items,BulkUpdaterCallback<T> cb,int batch_size);
}
