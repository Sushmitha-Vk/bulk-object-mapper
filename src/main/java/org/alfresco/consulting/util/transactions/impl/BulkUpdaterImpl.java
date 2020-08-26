package org.alfresco.consulting.util.transactions.impl;

import java.util.List;

import org.alfresco.consulting.util.transactions.BulkUpdater;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;

public class BulkUpdaterImpl implements BulkUpdater {
	private static final int BATCH_SIZE=100;
	ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.txnHelper = this.serviceRegistry.getRetryingTransactionHelper();
	}

	RetryingTransactionHelper txnHelper;

	@Override
	public <T> int bulkUpdate(final List<T> items, final BulkUpdaterCallback<T> cb) {
		return bulkUpdate(items,cb,BATCH_SIZE);
	}

	@Override
	public <T> int bulkUpdate(final List<T> items,final BulkUpdaterCallback<T> cb,final int batch_size) {
		int startIdx=0;
		int pCount=0;
		for (startIdx=0;startIdx < items.size();startIdx += batch_size) {
			final int myStartIdx=startIdx;
			pCount = txnHelper.doInTransaction(new RetryingTransactionCallback<Integer>(){

				@Override
				public Integer execute() throws Throwable {
					int i;
					for (i=myStartIdx;i<Math.min(myStartIdx+batch_size,items.size());i++) {
						cb.executeUpdate(items.get(i));
					}
					return i;
				}
			},false,true);
		}
		return pCount;
	}

}
