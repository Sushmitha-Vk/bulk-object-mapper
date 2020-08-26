package org.alfresco.consulting.accelerator.bgrun.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.alfresco.consulting.accelerator.bgrun.BackgroundCacheItem;
import org.alfresco.consulting.accelerator.bgrun.Status;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.GUID;
import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
class InMemBgCacheItemWithPersistence implements Serializable, BackgroundCacheItem {
	/**
	 *
	 */
	private static final String PERSISTANCE_NAMESPACE = "ALFRESCO_BOM";
	private static final long serialVersionUID = 1L;
	String hostName;
	String ipAddress;
	String id;
	Status status;
	@JsonSerialize(using = JsonObjectSerializer.class)
	transient JSONObject context;
	String title;
	String runAsUser;
	Long startTime = new Date().getTime();
	Long endTime = new Date().getTime();
	public boolean isDatabase = false;
	
	InMemBgCacheItemWithPersistence(InMemBgCacheWithPersistence inMemBgCacheWithPersistence, String title) {
		id = GUID.generate();
		status = Status.INITIALIZED;
		context = new JSONObject();
		this.title = title;
		startTime = new Date().getTime();
		endTime = new Date().getTime();
		isDatabase = inMemBgCacheWithPersistence.isDataBasePersistence();
		try {
			hostName = InetAddress.getLocalHost().getHostName();
			ipAddress = InetAddress.getLocalHost().getHostAddress();
			runAsUser = AuthenticationUtil.getFullyAuthenticatedUser();
		} catch (UnknownHostException e) {
			hostName = "unkown";
			ipAddress = "unknown";
			InMemBgCacheWithPersistence.logger.error("trouble getting Localhost", e);
		}
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	@Override
	public void distribute() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isReplica() {
		return (this.hostName != InMemBgCacheWithPersistence.hostName)
				|| (this.ipAddress != InMemBgCacheWithPersistence.ipAddress);
	}

	@Override
	public String getHostName() {
		return hostName;
	}

	@Override
	public String getIpAddress() {
		return ipAddress;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public JSONObject getContext() {
		return context;
	}

	@Override
	public void setContext(JSONObject context) {
		this.context = context;
	}

	@Override
	public void run() {
		if (status == Status.INITIALIZED) {
			status = Status.RUNNING;
			startTime = new Date().getTime();
			if(isDatabase) {
				InMemBgCacheWithPersistence.serviceRegistry.getAttributeService().setAttribute(this, new Serializable[] { PERSISTANCE_NAMESPACE, id });
			}
		} else {
			InMemBgCacheWithPersistence.logger.warn("Cannot move from " + status + " to " + Status.RUNNING);
		}
	}

	@Override
	public void terminate() {
		if (status == Status.RUNNING || status == Status.INITIALIZED) {
			status = Status.TERMINATED;
		} else {
			InMemBgCacheWithPersistence.logger.warn("Cannot move from " + status + " to " + Status.TERMINATED);
		}
	}

	@Override
	public void complete() {
		if (status == Status.RUNNING || status == Status.INITIALIZED) {
			status = Status.COMPLETE;
			endTime = new Date().getTime();
			if(isDatabase){
				InMemBgCacheWithPersistence.serviceRegistry.getAttributeService().setAttribute(this, new Serializable[] { PERSISTANCE_NAMESPACE, id });
			}
		} else {
			InMemBgCacheWithPersistence.logger.warn("Cannot move from " + status + " to " + Status.COMPLETE);
		}
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public String getRunAsUser() {
		return runAsUser;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.context.toString());
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException, JSONException {
		ois.defaultReadObject();
		this.context = new JSONObject(ois.readUTF());
    }
    
    
}
