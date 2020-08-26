package org.alfresco.consulting.accelerator.bulk;

import org.alfresco.service.namespace.QName;

public class BulkObjectMapperConstants {
	public enum UrlCheckMode {
		TRUE, FALSE, STORE, S3, DIRECTED
	}

	// PREFIX REMOVED Not Needed
	private static String BULK_OBJECT_MODEL_1_0_URI = "http://www.alfresco.org/model/consulting/buklobject/1.0";
	private static QName ASPECT_MISSING_CONTENT = null;
	private static QName PROP_MISSING_CONTENT_INFO = null;
	private static QName LOCK_HANGING_URL_LOCK_NAME = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "hangingUrlLock");
	private static QName KEY_HANGING_URL_STATUS_LOC = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "status");
	private static QName APP_HANGING_URL_STATUS_APP = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "application");
	private static QName PROP_IMPORT_OBJECT_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "itemId");
	private static QName PROP_IMPORT_GROUP_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "groupId");
	private static QName PROP_IMPORT_RUN_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "runId");
	private static QName ASPECT_RENAMED_CONTENT_URL = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "renamedContentUrl");
	private static QName PROP_ORIGINAL_CONTENT_URL = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "originalContentUrl");
	private static QName PROP_URL_RENAME_REASON = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "urlRenameReason");
	private static QName ASPECT_RENAMED_OBJECT = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "renamedObject");
	private static QName PROP_ORIGINAL_NAME = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "originalName");
	private static QName PROP_RENAME_REASON = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "renameReason");
	public static String OVERRIDE_VERSION_LABEL = "OverrideVersionLabel";

	public static void setUri(String uri) {
		BulkObjectMapperConstants.BULK_OBJECT_MODEL_1_0_URI = uri;

	}

	// Use lazy initialization of the QNames to allow content model to be loaded
	// first

	public static QName aspectMissingContent() {
		if (ASPECT_MISSING_CONTENT == null) {
			BulkObjectMapperConstants.ASPECT_MISSING_CONTENT = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"missingContent");
		}
		return ASPECT_MISSING_CONTENT;
	}

	public static QName propMissingContentInfo() {
		if (PROP_MISSING_CONTENT_INFO == null) {
			BulkObjectMapperConstants.PROP_MISSING_CONTENT_INFO = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"missingContentInfo");
		}
		return PROP_MISSING_CONTENT_INFO;
	}

	public static QName hangingUrlLock() {
		if (LOCK_HANGING_URL_LOCK_NAME == null) {
			BulkObjectMapperConstants.LOCK_HANGING_URL_LOCK_NAME = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"hangingUrlLock");
		}
		return LOCK_HANGING_URL_LOCK_NAME;
	}

	public static QName hangingUrlStatus() {
		if (KEY_HANGING_URL_STATUS_LOC == null) {
			BulkObjectMapperConstants.KEY_HANGING_URL_STATUS_LOC = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"status");
		}
		return KEY_HANGING_URL_STATUS_LOC;
	}

	public static QName AhangingUrlApplication() {
		if (APP_HANGING_URL_STATUS_APP == null) {
			BulkObjectMapperConstants.APP_HANGING_URL_STATUS_APP = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"application");
		}
		return APP_HANGING_URL_STATUS_APP;
	}

	public static QName propItemId() {
		if (PROP_IMPORT_OBJECT_ID == null) {
			BulkObjectMapperConstants.PROP_IMPORT_OBJECT_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "itemId");
		}
		return PROP_IMPORT_OBJECT_ID;
	}

	public static QName propGroupId() {
		if (PROP_IMPORT_GROUP_ID == null) {
			BulkObjectMapperConstants.PROP_IMPORT_GROUP_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "groupId");
		}
		return PROP_IMPORT_GROUP_ID;
	}

	public static QName propRunId() {
		if (PROP_IMPORT_RUN_ID == null) {
			BulkObjectMapperConstants.PROP_IMPORT_RUN_ID = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "runId");
		}
		return PROP_IMPORT_RUN_ID;
	}

	public static QName aspectRenamedContentUrl() {
		if (ASPECT_RENAMED_CONTENT_URL == null) {
			BulkObjectMapperConstants.ASPECT_RENAMED_CONTENT_URL = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"renamedContentUrl");
		}
		return ASPECT_RENAMED_CONTENT_URL;
	}

	public static QName propOriginalContentUrl() {
		if (PROP_ORIGINAL_CONTENT_URL == null) {
			BulkObjectMapperConstants.PROP_ORIGINAL_CONTENT_URL = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"originalContentUrl");
		}
		return PROP_ORIGINAL_CONTENT_URL;
	}

	public static QName propUrlRenameReason() {
		if (PROP_URL_RENAME_REASON == null) {
			BulkObjectMapperConstants.PROP_URL_RENAME_REASON = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"urlRenameReason");
		}
		return PROP_URL_RENAME_REASON;
	}

	public static QName aspectRenamedObject() {
		if (ASPECT_RENAMED_OBJECT == null) {
			BulkObjectMapperConstants.ASPECT_RENAMED_OBJECT = QName.createQName(BULK_OBJECT_MODEL_1_0_URI,
					"renamedObject");
		}
		return ASPECT_RENAMED_OBJECT;
	}

	public static QName propOriginalName() {
		if (PROP_ORIGINAL_NAME == null) {
			BulkObjectMapperConstants.PROP_ORIGINAL_NAME = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "originalName");
		}
		return PROP_ORIGINAL_NAME;
	}

	public static QName propRenameReason() {
		if (PROP_RENAME_REASON == null) {
			BulkObjectMapperConstants.PROP_RENAME_REASON = QName.createQName(BULK_OBJECT_MODEL_1_0_URI, "renameReason");
		}
		return PROP_RENAME_REASON;
	}

}
