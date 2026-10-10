package com.dynamicformbuilder.formversion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.security.AdminOnly;

@RestController
@RequestMapping("/api/forms/{formId}/versions")
@AdminOnly
public class FormVersionController {

	private final FormVersionService formVersionService;

	public FormVersionController(FormVersionService formVersionService) {
		this.formVersionService = formVersionService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<FormVersionResponse>>> list(@PathVariable Long formId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success("Versions fetched successfully",
				formVersionService.list(formId, page, size)));
	}

	@GetMapping("/{version}")
	public ResponseEntity<ApiResponse<FormVersionDetailResponse>> get(@PathVariable Long formId,
			@PathVariable Integer version) {
		return ResponseEntity.ok(ApiResponse.success("Version fetched successfully",
				formVersionService.get(formId, version)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<FormVersionResponse>> createNewVersion(@PathVariable Long formId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
				"Version created successfully", formVersionService.createNewVersion(formId)));
	}

	@PostMapping("/{version}/publish")
	public ResponseEntity<ApiResponse<FormVersionDetailResponse>> publish(@PathVariable Long formId,
			@PathVariable Integer version) {
		return ResponseEntity.ok(ApiResponse.success("Version published successfully",
				formVersionService.publish(formId, version)));
	}
}
