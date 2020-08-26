package org.alfresco.consulting.accelerator.bulk.url;

import org.alfresco.consulting.accelerator.bulk.ContentUrlValidator;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3ContentUrlValidator implements ContentUrlValidator {

	private static final Log logger = LogFactory.getLog(S3ContentUrlValidator.class);
	private AmazonS3 s3client;
	String regionId;
	String kmsKey;
	String s3bucket;

	private ContentService contentService;
	
	public S3ContentUrlValidator(ContentService contentService, String s3bucket,String regionId, String kmsKey) {
		this.contentService = contentService;
		this.s3bucket = s3bucket;
		this.regionId = regionId;
		this.kmsKey = kmsKey;
		try {
			s3client = AmazonS3EncryptionClientBuilder
			        .standard()
			        .withRegion(regionId)
			        .withCryptoConfiguration(new CryptoConfiguration(CryptoMode.AuthenticatedEncryption))
			        // Can either be Key ID or alias (prefixed with 'alias/')
			        .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(kmsKey))
			        .build();
		} catch (Exception e) {
			logger.error("Could not Create Encrypted Client. Creating Default Client",e);
			s3client = AmazonS3ClientBuilder.defaultClient();
		}

	}

	@Override
	public boolean checkContentUrl(String url) {
		// Only Check S3 content URLs against S3
		if (url.toLowerCase().startsWith("s3")) {
			try {
				String objectKey=url.substring(url.lastIndexOf("://")+3);
				ObjectMetadata om = ((AmazonS3) s3client).getObjectMetadata(s3bucket, objectKey);
				return (om != null);
			} catch (RuntimeException e) {
				logger.debug("Trouble finding object",e);
				return false;
			}			
		}
		ContentReader cr =contentService.getRawReader(url);
		if (logger.isTraceEnabled()) {
			logger.trace("Content URL Exists= " + cr.exists());
		}
		return cr.exists();
	}

}
