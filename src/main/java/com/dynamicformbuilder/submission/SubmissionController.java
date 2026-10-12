package com.dynamicformbuilder.submission;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;
import com.dynamicformbuilder.security.AdminOnly;

@RestController
@AdminOnly
public class SubmissionController {

	private final SubmissionService submissionService;
	private final SubmissionFileService fileService;

	public SubmissionController(SubmissionService submissionService, SubmissionFileService fileService) {
		this.submissionService = submissionService;
		this.fileService = fileService;
	}

	@GetMapping("/api/forms/{formCode}/submissions")
	public ResponseEntity<ApiResponse<SubmissionListResponse>> list(@PathVariable String formCode,
			@RequestParam(required = false) Integer version,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success("Submissions fetched successfully",
				submissionService.list(formCode, version, page, size)));
	}

	@GetMapping("/api/submissions/{submissionId}")
	public ResponseEntity<ApiResponse<SubmissionResponse>> get(@PathVariable Long submissionId) {
		return ResponseEntity.ok(ApiResponse.success("Submission fetched successfully",
				submissionService.get(submissionId)));
	}

	// The one endpoint that answers with the file itself instead of the JSON envelope (errors still use it)
	@GetMapping("/api/submissions/{submissionId}/files/{fileId}")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long submissionId, @PathVariable Long fileId) {
		FileDownload download = fileService.download(submissionId, fileId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.contentType()))
				.contentLength(download.sizeBytes())
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(download.name(), StandardCharsets.UTF_8).build().toString())
				.body(download.resource());
	}
}
