package org.alfresco.consulting.accelerator.bulk;

import java.util.List;

import org.alfresco.consulting.util.transactions.BulkUpdater;
import org.json.JSONObject;

public interface BulkUpdaterThreadPool extends BulkUpdater {
	public final static String FIELD_POOL_SIZE = "poolSize";
	public final static String FIELD_BATCH_SIZE = "batch_size";
	public final static String FIELD_COUNT = "count";
	public final static String FIELD_TOTAL = "total";

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, boolean runAsSystem);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, boolean runAsSystem);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size,
			boolean runAsSystem);

	// Add Context for reporting
	<T> int bulkUpdate(final List<T> items, final BulkUpdaterCallback<T> cb, final int batch_size, JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size, JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, boolean runAsSystem, JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, boolean runAsSystem,
			JSONObject ctx);

	<T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size,
			boolean runAsSystem, JSONObject ctx);
}
