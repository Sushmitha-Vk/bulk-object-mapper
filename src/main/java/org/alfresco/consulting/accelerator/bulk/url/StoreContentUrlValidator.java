package org.alfresco.consulting.accelerator.bulk.url;

import org.alfresco.consulting.accelerator.bulk.ContentUrlValidator;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StoreContentUrlValidator implements ContentUrlValidator {
	
	private static final Log logger = LogFactory.getLog(StoreContentUrlValidator.class);
	private ContentService contentService;
	
	public StoreContentUrlValidator(ContentService contentService) {
		this.contentService = contentService;
	}

	@Override
	public boolean checkContentUrl(String url) {
		ContentReader cr =contentService.getRawReader(url);
		if (logger.isTraceEnabled()) {
			logger.trace("Content URL Exists= " + cr.exists());
		}
		return cr.exists();
	}

}
