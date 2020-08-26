package org.alfresco.consulting.accelerator.bgrun;

import java.io.IOException;
import java.util.Set;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface BackgroundCache {

	String newItem(String title);

	String newItem(String title, JSONObject context);

	BackgroundCacheItem getItem(String id);
	
	void removeItem(String id);

	JSONObject getItemAsJSON(String id);

	Set<String> getBackgroundCacheItemIds();
	
	String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception;

	String removeObjectsFromPersistance();
}
