package com.dynamicformbuilder.audit;

import java.time.LocalDateTime;

import com.dynamicformbuilder.user.User;

public record AuditResponse(
		Long id,
		AuditAction action,
		Integer versionNumber,
		Long userId,
		String userName,
		String userEmail,
		LocalDateTime createdAt) {

	public static AuditResponse from(FormAudit audit, User user) {
		String name = user.getLastName() == null ? user.getFirstName()
				: user.getFirstName() + " " + user.getLastName();
		return new AuditResponse(audit.getId(), audit.getAction(), audit.getVersionNumber(), user.getId(), name,
				user.getEmail(), audit.getCreatedAt());
	}
}
