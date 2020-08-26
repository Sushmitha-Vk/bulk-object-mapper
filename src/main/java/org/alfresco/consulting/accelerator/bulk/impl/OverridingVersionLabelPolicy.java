package org.alfresco.consulting.accelerator.bulk.impl;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.consulting.accelerator.bulk.BulkObjectMapperConstants;
import org.alfresco.repo.version.VersionServicePolicies.CalculateVersionLabelPolicy;
import org.alfresco.repo.version.common.versionlabel.SerialVersionLabelPolicy;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OverridingVersionLabelPolicy extends SerialVersionLabelPolicy implements CalculateVersionLabelPolicy {
	private static final Log logger = LogFactory.getLog(CalculateVersionLabelPolicy.class);

	public String calculateVersionLabel(QName classRef, Version preceedingVersion, int versionNumber,
			Map<String, Serializable> versionProperties) {
		String versionLabel = (String) versionProperties.get(BulkObjectMapperConstants.OVERRIDE_VERSION_LABEL);
		if (logger.isDebugEnabled() && versionLabel != null) {
			logger.debug("Setting Version Label to: " + versionLabel);
		} else if (logger.isTraceEnabled()) {
			logger.trace("Calling with null Version Label -- pass thru to SerialVersionLabelPolicy");
		}
		return (versionLabel != null) ? versionLabel
				: super.calculateVersionLabel(classRef, preceedingVersion, versionNumber, versionProperties);
	}
}
