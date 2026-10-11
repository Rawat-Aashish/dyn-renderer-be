package com.dynamicformbuilder.form;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.security.AdminOnly;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/forms")
@AdminOnly
public class FormController {

	private final FormService formService;

	public FormController(FormService formService) {
		this.formService = formService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<FormResponse>> create(@Valid @RequestBody CreateFormRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Form created successfully", formService.create(request)));
	}

	@PostMapping("/{id}/clone")
	public ResponseEntity<ApiResponse<FormResponse>> cloneForm(@PathVariable Long id,
			@Valid @RequestBody CloneFormRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Form cloned successfully", formService.cloneForm(id, request)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<FormResponse>>> list(
			@RequestParam(defaultValue = "false") boolean archived,
			@RequestParam(required = false) FormStatus status,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success("Forms fetched successfully",
				formService.list(archived, status, search, page, size)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<FormDetailResponse>> get(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Form fetched successfully", formService.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<FormResponse>> update(@PathVariable Long id,
			@Valid @RequestBody UpdateFormRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Form updated successfully", formService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		formService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Form deleted successfully", null));
	}

	@PostMapping("/{id}/archive")
	public ResponseEntity<ApiResponse<FormResponse>> archive(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Form archived successfully", formService.archive(id)));
	}
}
