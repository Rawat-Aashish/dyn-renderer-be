package com.dynamicformbuilder.component;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;
import com.dynamicformbuilder.security.AdminOnly;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/forms/{formId}/components")
@AdminOnly
public class ComponentController {

	private final ComponentService componentService;

	public ComponentController(ComponentService componentService) {
		this.componentService = componentService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ComponentResponse>> create(@PathVariable Long formId,
			@Valid @RequestBody ComponentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Component added successfully", componentService.create(formId, request)));
	}

	@PutMapping("/reorder")
	public ResponseEntity<ApiResponse<List<ComponentResponse>>> reorder(@PathVariable Long formId,
			@Valid @RequestBody ReorderComponentsRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Components reordered successfully",
				componentService.reorder(formId, request)));
	}

	@PutMapping("/{componentId}")
	public ResponseEntity<ApiResponse<ComponentResponse>> update(@PathVariable Long formId,
			@PathVariable Long componentId, @Valid @RequestBody ComponentRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Component updated successfully",
				componentService.update(formId, componentId, request)));
	}

	@DeleteMapping("/{componentId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long formId, @PathVariable Long componentId) {
		componentService.delete(formId, componentId);
		return ResponseEntity.ok(ApiResponse.success("Component deleted successfully", null));
	}
}
