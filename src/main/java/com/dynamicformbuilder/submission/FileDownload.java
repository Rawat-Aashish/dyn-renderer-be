package com.dynamicformbuilder.submission;

import org.springframework.core.io.Resource;

public record FileDownload(String name, String contentType, long sizeBytes, Resource resource) {
}
