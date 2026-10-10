package com.dynamicformbuilder.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dynamicformbuilder.common.response.ApiResponse;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.security.AdminOnly;

@RestController
@AdminOnly
public class AuditController {

	private final AuditService auditService;

	public AuditController(AuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping("/api/forms/{formId}/audit")
	public ResponseEntity<ApiResponse<PageResponse<AuditResponse>>> list(@PathVariable Long formId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success("Audit log fetched successfully",
				auditService.list(formId, page, size)));
	}
}
