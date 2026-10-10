package com.dynamicformbuilder.common.response;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** One place for the list rules: pages start at 0, size defaults to 20 and is capped at 100. */
public final class Paging {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private Paging() {
	}

	public static Pageable descending(int page, int size, String... sortBy) {
		int safePage = Math.max(page, 0);
		int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortBy));
	}
}
