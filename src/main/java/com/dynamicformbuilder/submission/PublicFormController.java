package com.dynamicformbuilder.submission;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;

import jakarta.validation.Valid;

// No @AdminOnly: these five routes are for anonymous end users and are opened in SecurityConfig
@RestController
@RequestMapping("/api/forms/{formCode}")
public class PublicFormController {

	private final SubmissionService submissionService;

	public PublicFormController(SubmissionService submissionService) {
		this.submissionService = submissionService;
	}

	@GetMapping("/published")
	public ResponseEntity<ApiResponse<PublishedFormResponse>> getPublished(@PathVariable String formCode) {
		return ResponseEntity.ok(ApiResponse.success("Form fetched successfully",
				submissionService.getPublished(formCode)));
	}

	@PostMapping("/submissions/start")
	public ResponseEntity<ApiResponse<StartSubmissionResponse>> start(@PathVariable String formCode) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Submission started", submissionService.start(formCode)));
	}

	@PostMapping("/submissions")
	public ResponseEntity<ApiResponse<SubmissionConfirmationResponse>> submit(@PathVariable String formCode,
			@Valid @RequestBody SubmitFormRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Form submitted successfully", submissionService.submit(formCode, request)));
	}

	@PostMapping("/submissions/rating")
	public ResponseEntity<ApiResponse<Void>> rate(@PathVariable String formCode,
			@Valid @RequestBody RateSubmissionRequest request) {
		submissionService.rate(formCode, request);
		return ResponseEntity.ok(ApiResponse.success("Rating saved", null));
	}

	@PostMapping(value = "/submissions/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<SubmissionFileResponse>> uploadFile(@PathVariable String formCode,
			@RequestParam UUID token, @RequestParam String fieldCode, @RequestParam MultipartFile file) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully",
				submissionService.uploadFile(formCode, token, fieldCode, file)));
	}
}
