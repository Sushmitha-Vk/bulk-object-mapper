package org.alfresco.consulting.accelerator.bulk;

import java.io.InputStream;
import org.json.JSONObject;

public interface BulkUpdateFilter {
	String filterName();
	String filter(JSONObject config,InputStream input);
}
