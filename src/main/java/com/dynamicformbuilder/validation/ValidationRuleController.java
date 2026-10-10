package com.dynamicformbuilder.validation;

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
@RequestMapping("/api/components/{componentId}/validations")
@AdminOnly
public class ValidationRuleController {

	private final ValidationRuleService validationRuleService;

	public ValidationRuleController(ValidationRuleService validationRuleService) {
		this.validationRuleService = validationRuleService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ValidationRuleResponse>> create(@PathVariable Long componentId,
			@Valid @RequestBody CreateValidationRuleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
				"Validation rule added successfully", validationRuleService.create(componentId, request)));
	}

	@PutMapping("/{ruleId}")
	public ResponseEntity<ApiResponse<ValidationRuleResponse>> update(@PathVariable Long componentId,
			@PathVariable Long ruleId, @Valid @RequestBody UpdateValidationRuleRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Validation rule updated successfully",
				validationRuleService.update(componentId, ruleId, request)));
	}

	@DeleteMapping("/{ruleId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long componentId, @PathVariable Long ruleId) {
		validationRuleService.delete(componentId, ruleId);
		return ResponseEntity.ok(ApiResponse.success("Validation rule deleted successfully", null));
	}
}
