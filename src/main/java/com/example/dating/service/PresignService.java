package com.example.dating.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;

@Service
public class PresignService {
	@Value("${app.s3.bucket}")
	String bucket;

	public URI presignUpload(String filename, String contentType) {
		return URI.create("https://example-s3/" + bucket + "/" + filename + "?presigned");
	}
}
