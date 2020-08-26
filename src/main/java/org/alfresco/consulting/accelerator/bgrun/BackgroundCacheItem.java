package org.alfresco.consulting.accelerator.bgrun;

import org.json.JSONObject;

public interface BackgroundCacheItem {
		String getHostName();

		String getIpAddress();

		String getId();

		String getTitle();

		Status getStatus();

		String getRunAsUser();

		JSONObject getContext();
		
		Long getStartTime();
		
		Long getEndTime();

		void setContext(JSONObject context);

		void run();

		void terminate();

		void complete();

		void distribute(); // for caches that can;

		boolean isReplica(); 
	}
