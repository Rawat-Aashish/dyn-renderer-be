package com.dynamicformbuilder.submission;

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

	public SubmissionController(SubmissionService submissionService) {
		this.submissionService = submissionService;
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
}
