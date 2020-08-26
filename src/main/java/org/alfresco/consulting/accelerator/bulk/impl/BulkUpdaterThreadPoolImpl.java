package org.alfresco.consulting.accelerator.bulk.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.alfresco.consulting.accelerator.bulk.BulkUpdaterThreadPool;
import org.alfresco.consulting.util.transactions.impl.BulkUpdaterImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

public class BulkUpdaterThreadPoolImpl extends BulkUpdaterImpl implements BulkUpdaterThreadPool {
	private static final int BATCH_SIZE = 100;
	private static final int POOL_SIZE = 2;
	ServiceRegistry serviceRegistry;
	RetryingTransactionHelper txnHelper;
	int defaultPoolSize = BATCH_SIZE;
	int defaultBatchSize = POOL_SIZE;

	private static final Log logger = LogFactory.getLog(BulkUpdaterThreadPoolImpl.class);

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.txnHelper = this.serviceRegistry.getRetryingTransactionHelper();
		super.setServiceRegistry(serviceRegistry);
	}

	public void setDefaultPoolSize(int defaultPoolSize) {
		this.defaultPoolSize = defaultPoolSize;
	}

	public void setDefaultBatchSize(int defaultBatchSize) {
		this.defaultBatchSize = defaultBatchSize;
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb) {
		return threadedBulkUpdate(items, cb, defaultPoolSize, defaultBatchSize, false);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, boolean runAsSystem) {
		return threadedBulkUpdate(items, cb, defaultPoolSize, defaultBatchSize, runAsSystem);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize) {
		return threadedBulkUpdate(items, cb, poolSize, defaultBatchSize, false);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, boolean runAsSystem) {
		return threadedBulkUpdate(items, cb, poolSize, defaultBatchSize, runAsSystem);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size) {
		return threadedBulkUpdate(items, cb, poolSize, batch_size, false);
	}

	@Override
	public <T> int threadedBulkUpdate(final List<T> items, final BulkUpdaterCallback<T> cb, final int poolSize,
			final int batch_size, boolean runAsSystem) {
		return this.threadedBulkUpdate(items, cb, poolSize, batch_size, runAsSystem, null);
	}

	@Override
	public <T> int threadedBulkUpdate(final List<T> items, final BulkUpdaterCallback<T> cb, final int poolSize,
			final int batch_size, boolean runAsSystem, JSONObject ctx) {
		if (ctx != null) {
			try {
				ctx.put(FIELD_BATCH_SIZE, batch_size);
				ctx.put(FIELD_POOL_SIZE, poolSize);
				ctx.put(FIELD_TOTAL, items.size());
				ctx.put(FIELD_COUNT, 0);
			} catch (JSONException e) {
				logger.error("Error Setting up Context", e);
			}
		}
		ExecutorService es = Executors.newFixedThreadPool(poolSize, new CustomizableThreadFactory("BulkUpdater"));
		List<Future<Integer>> flist = new ArrayList<Future<Integer>>();
		int startIdx = 0;
		int pCount = 0;
		final String currentUser = runAsSystem ? AuthenticationUtil.getFullyAuthenticatedUser()
				: AuthenticationUtil.getSystemUserName();
		for (startIdx = 0; startIdx < items.size(); startIdx += batch_size) {
			final int myStartIdx = startIdx;

			Future<Integer> x = es.submit(new Callable<Integer>() {

				@Override
				public Integer call() throws Exception {

					return AuthenticationUtil.runAs(new RunAsWork<Integer>() {

						@Override
						public Integer doWork() throws Exception {

							return txnHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {

								@Override
								public Integer execute() throws Throwable {
									int i;
									for (i = myStartIdx; i < Math.min(myStartIdx + batch_size, items.size()); i++) {
										cb.executeUpdate(items.get(i));
									}
									return i - myStartIdx;
								}
							}, false, true);
						}
					}, currentUser);

				}

			});
			logger.debug("Started Thread " + x.toString());
			flist.add(x);
		}
		for (Future<Integer> f : flist) {
			try {
				int numDone = f.get();
				synchronized (this) {
					pCount += numDone;
					if (ctx != null) {
						try {
							ctx.put(FIELD_COUNT, pCount);
						} catch (JSONException e) {
							logger.error("Error Updating Context", e);
						}
					}
				}
				logger.debug("Thread: " + f.toString() + " " + numDone + " items processed ");
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Thread: " + f.toString() + " Had problems", e);
			}
		}
		es.shutdown();
		return pCount;
	}

	@Override
	public <T> int bulkUpdate(final List<T> items, final BulkUpdaterCallback<T> cb, final int batch_size,
			final JSONObject ctx) {
		if (ctx != null) {
			try {
				ctx.put(FIELD_BATCH_SIZE, batch_size);
				ctx.put(FIELD_POOL_SIZE, 1);
				ctx.put(FIELD_TOTAL, items.size());
				ctx.put(FIELD_COUNT, 0);
			} catch (JSONException e) {
				logger.error("Error Setting up Context", e);
			}
		}
		int startIdx = 0;
		int pCount = 0;
		for (startIdx = 0; startIdx < items.size(); startIdx += batch_size) {
			final int myStartIdx = startIdx;
			pCount = txnHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {

				@Override
				public Integer execute() throws Throwable {
					int i;
					for (i = myStartIdx; i < Math.min(myStartIdx + batch_size, items.size()); i++) {
						cb.executeUpdate(items.get(i));
					}
					try {
						ctx.put(FIELD_COUNT, ctx.optLong(FIELD_COUNT, 0) + i);
					} catch (JSONException e) {
						logger.error("Error Updating Context", e);
					}
					return i;
				}
			}, false, true);
		}
		return pCount;
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, JSONObject ctx) {
		return threadedBulkUpdate(items, cb, defaultPoolSize, defaultBatchSize, false, ctx);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, boolean runAsSystem, JSONObject ctx) {
		return threadedBulkUpdate(items, cb, defaultPoolSize, defaultBatchSize, runAsSystem, ctx);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, JSONObject ctx) {
		return threadedBulkUpdate(items, cb, poolSize, defaultBatchSize, false, ctx);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, boolean runAsSystem,
			JSONObject ctx) {
		return threadedBulkUpdate(items, cb, poolSize, defaultBatchSize, runAsSystem, ctx);
	}

	@Override
	public <T> int threadedBulkUpdate(List<T> items, BulkUpdaterCallback<T> cb, int poolSize, int batch_size,
			JSONObject ctx) {
		return threadedBulkUpdate(items, cb, poolSize, batch_size, false, ctx);
	}

}
