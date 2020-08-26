package org.alfresco.consulting.accelerator.bgrun;


public interface BackgroundRunner {
	public interface BackgroundCachedRunnable extends Runnable {
		BackgroundCacheItem getCacheItem();
	}

	void process(BackgroundCachedRunnable bcr);
}
