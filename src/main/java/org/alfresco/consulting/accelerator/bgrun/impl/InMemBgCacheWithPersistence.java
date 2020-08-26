package org.alfresco.consulting.accelerator.bgrun.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
@PropertySource(value = "classpath:alfresco-global.properties")

public class InMemBgCacheWithPersistence implements BackgroundCache {

	private static final String PERSISTANCE_NAMESPACE = "ALFRESCO_BOM";
	static final Log logger = LogFactory.getLog(InMemBgCacheWithPersistence.class);
	static String hostName;
	static String ipAddress;
	
	@Value("${bulk.object.memory.persistence.retention.period:300000}") int retentionPeriod;	
	@Value("#{alfrescoProperties['bulk.object.status.persistence.target'] ?: 'memory'}") private String persistenceTarget;
	
	public static ServiceRegistry serviceRegistry;
	private AttributeService attributeService;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		InMemBgCacheWithPersistence.serviceRegistry = serviceRegistry;
		this.attributeService = InMemBgCacheWithPersistence.serviceRegistry.getAttributeService();
	}

	Map<String, InMemBgCacheItemWithPersistence> cache = new HashMap<String, InMemBgCacheItemWithPersistence>();
	static {
		try {
			hostName = InetAddress.getLocalHost().getHostName();
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			hostName = "unkown";
			ipAddress = "unknown";
			logger.error("trouble getting static Localhost", e);
		}
	}

	@Override
	public String newItem(String title) {
		InMemBgCacheItemWithPersistence item = new InMemBgCacheItemWithPersistence(this, title);
		if(isMemoryPersistence()) {
			cache.put(item.getId(), item);
		} else if(isDataBasePersistence()) {
			attributeService.setAttribute(item, this.makePersistanceKey(item.getId()));
		}
		return item.getId();
	}

	@Override
	public String newItem(String title, JSONObject context) {
		InMemBgCacheItemWithPersistence item = new InMemBgCacheItemWithPersistence(this, title);
		item.setContext(context);
		if(isMemoryPersistence()) {
			cache.put(item.getId(), item);
		} else if(isDataBasePersistence()) {
			attributeService.setAttribute((Serializable) item, this.makePersistanceKey(item.getId()));
		}
		return item.getId();
	}

	@Override
	public InMemBgCacheItemWithPersistence getItem(String id) {
		InMemBgCacheItemWithPersistence item = null;
		if(isMemoryPersistence()) {
		    item = cache.get(id);
		} else if(isDataBasePersistence()) {
			item = (InMemBgCacheItemWithPersistence) attributeService.getAttribute(this.makePersistanceKey(id));
		}
		return item;
	}

	@Override
	public JSONObject getItemAsJSON(String id) {
		InMemBgCacheItemWithPersistence item = this.getItem(id);
		JSONObject ret = new JSONObject();
		try {
			ret.put("id", item.getId());
			ret.put("status", item.getStatus());
			ret.put("title", item.getTitle());
			ret.put("runAsUser", item.getRunAsUser());
			ret.put("context", item.getContext());
			ret.put("hostName", item.getHostName());
			ret.put("ipAddress", item.getIpAddress());
			ret.put("startTime", item.getStartTime());
			ret.put("endTime", item.getEndTime());
		} catch (JSONException e) {
			logger.error("Trouble Creating JSON", e);
		}
		return ret;
	}

	@Override
	public Set<String> getBackgroundCacheItemIds() {
		return cache.keySet();
	}
	
	@Override
	public String getAllBackgroundItems() throws JsonProcessingException, IOException, Exception, Exception {
		String response = "";
		ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		if(isMemoryPersistence()) {
			Collection<InMemBgCacheItemWithPersistence> cacheItemValues = cache.values();
			List<InMemBgCacheItemWithPersistence> cacheListItems = new ArrayList<InMemBgCacheItemWithPersistence>(cacheItemValues);
	        response = mapper.writeValueAsString(cacheListItems);
		} else if(isDataBasePersistence()) {
		    	final Set<InMemBgCacheItemWithPersistence> persistItems = new HashSet<InMemBgCacheItemWithPersistence>();
			    this.attributeService.getAttributes(new AttributeService.AttributeQueryCallback() {	
					@Override
					public boolean handleAttribute(Long id, Serializable value, Serializable[] keys) {						
			             InMemBgCacheItemWithPersistence item = (InMemBgCacheItemWithPersistence)value;
			             persistItems.add(item);
			     		 return true;
					}
				}, PERSISTANCE_NAMESPACE);
			    response = mapper.writeValueAsString(persistItems);
		}
		return response;
	}
	
	@Override
	public String removeObjectsFromPersistance(){
		this.attributeService.removeAttributes(PERSISTANCE_NAMESPACE);
		return "Removed persistance items successfully";
	}

	private Serializable[] makePersistanceKey(String id) {
		return new Serializable[] { PERSISTANCE_NAMESPACE, id };
	}
	
	void clearCacheWithDelay(final String key) {
		Runnable runnable= new Runnable() {
			@Override
			public void run() {
				try {
					TimeUnit.MILLISECONDS.sleep(retentionPeriod);
				} catch (InterruptedException e) {
					logger.error("Exception while removing cached object",e);	
				}
				cache.remove(key);
			}
		};
		runnable.run();
	}
	
	public boolean isMemoryPersistence() {
	    return persistenceTarget.equalsIgnoreCase("memory");
	}
	
	public boolean isDataBasePersistence() {
		return persistenceTarget.equalsIgnoreCase("database");
	}

	public void removeItem(String id) {
		if(this.isMemoryPersistence()) {
			this.clearCacheWithDelay(id);
		}
	}
}
