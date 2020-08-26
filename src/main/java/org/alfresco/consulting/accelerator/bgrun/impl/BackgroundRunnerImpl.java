package org.alfresco.consulting.accelerator.bgrun.impl;

import org.alfresco.consulting.accelerator.bgrun.BackgroundRunner;

public class BackgroundRunnerImpl implements BackgroundRunner {

	@Override
	public void process(BackgroundCachedRunnable bcr) {
		Thread backgroundThread = new Thread(bcr, bcr.getCacheItem().getTitle() + "-" + bcr.getCacheItem().getId());
		backgroundThread.start();
	}

}
