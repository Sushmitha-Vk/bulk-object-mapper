package org.alfresco.consulting.accelerator.bulk.url;

import org.alfresco.consulting.accelerator.bulk.ContentUrlValidator;

public class ConstantContentUrlValidator implements ContentUrlValidator {
	
	private boolean constantValue;

	public ConstantContentUrlValidator(boolean constantValue) {
		this.constantValue = constantValue;
	}

	@Override
	public boolean checkContentUrl(String url) {
		return constantValue;
	}

}
