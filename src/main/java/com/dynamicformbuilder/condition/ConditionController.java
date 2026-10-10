package com.dynamicformbuilder.condition;

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
@RequestMapping("/api/forms/{formId}/conditions")
@AdminOnly
public class ConditionController {

	private final ConditionService conditionService;

	public ConditionController(ConditionService conditionService) {
		this.conditionService = conditionService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ConditionResponse>> create(@PathVariable Long formId,
			@Valid @RequestBody ConditionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Condition added successfully", conditionService.create(formId, request)));
	}

	@PutMapping("/{conditionId}")
	public ResponseEntity<ApiResponse<ConditionResponse>> update(@PathVariable Long formId,
			@PathVariable Long conditionId, @Valid @RequestBody ConditionRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Condition updated successfully",
				conditionService.update(formId, conditionId, request)));
	}

	@DeleteMapping("/{conditionId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long formId, @PathVariable Long conditionId) {
		conditionService.delete(formId, conditionId);
		return ResponseEntity.ok(ApiResponse.success("Condition deleted successfully", null));
	}
}
