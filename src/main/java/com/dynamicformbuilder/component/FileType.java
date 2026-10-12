package com.dynamicformbuilder.component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Every file format the server accepts in a FILE_UPLOAD field. An admin picks a subset per field by key
 * ("jpg", "png", "webp", "pdf"); nothing outside this list can be allowed.
 */
public enum FileType {

	JPG("image/jpeg", "jpg", "jpeg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp"),
	PDF("application/pdf", "pdf");

	/** How many leading bytes {@link #matchesContent} needs. */
	public static final int SIGNATURE_LENGTH = 12;

	private final String contentType;
	private final List<String> extensions;

	FileType(String contentType, String... extensions) {
		this.contentType = contentType;
		this.extensions = List.of(extensions);
	}

	public String key() {
		return name().toLowerCase();
	}

	public String contentType() {
		return contentType;
	}

	public static List<String> keys() {
		return Arrays.stream(values()).map(FileType::key).toList();
	}

	public static Optional<FileType> ofExtension(String extension) {
		return Arrays.stream(values()).filter(type -> type.extensions.contains(extension)).findFirst();
	}

	/** A file name can claim anything, so the first bytes of the file must carry the format's own signature. */
	public boolean matchesContent(byte[] head) {
		return switch (this) {
			case JPG -> startsWith(head, 0, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
			case PNG -> startsWith(head, 0, new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
			case WEBP -> startsWith(head, 0, ascii("RIFF")) && startsWith(head, 8, ascii("WEBP"));
			case PDF -> startsWith(head, 0, ascii("%PDF-"));
		};
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	private static boolean startsWith(byte[] head, int offset, byte[] signature) {
		return head.length >= offset + signature.length
				&& Arrays.equals(head, offset, offset + signature.length, signature, 0, signature.length);
	}
}
