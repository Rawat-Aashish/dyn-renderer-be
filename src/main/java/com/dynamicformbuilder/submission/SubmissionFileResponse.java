package com.dynamicformbuilder.submission;

/** Also the shape stored in a submission's data for a FILE_UPLOAD field. */
public record SubmissionFileResponse(
		Long fileId,
		String fieldCode,
		String name,
		String contentType,
		Long sizeBytes) {

	public static SubmissionFileResponse from(SubmissionFile file) {
		return new SubmissionFileResponse(file.getId(), file.getFieldCode(), file.getOriginalName(),
				file.getContentType(), file.getSizeBytes());
	}
}
