package com.dynamicformbuilder.submission;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dynamicformbuilder.common.exception.FieldErrorsException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.component.FileType;
import com.dynamicformbuilder.config.AppProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Keeps uploaded files on the local disk under app.upload.dir (Laravel: the "local" filesystem disk) and
 * their metadata in form_submission_files. A file is never stored under the name the user gave it.
 */
@Service
public class SubmissionFileService {

	private static final Logger log = LoggerFactory.getLogger(SubmissionFileService.class);

	private final SubmissionFileRepository fileRepository;
	private final SubmissionRepository submissionRepository;
	private final ObjectMapper objectMapper;
	private final Path root;

	public SubmissionFileService(SubmissionFileRepository fileRepository, SubmissionRepository submissionRepository,
			ObjectMapper objectMapper, AppProperties appProperties) {
		this.fileRepository = fileRepository;
		this.submissionRepository = submissionRepository;
		this.objectMapper = objectMapper;
		this.root = Path.of(appProperties.upload().dir()).toAbsolutePath().normalize();
	}

	/** Checks the file against the field's settings, saves it, and replaces any file uploaded earlier for the field. */
	@Transactional
	public SubmissionFileResponse store(FormSubmission submission, ComponentResponse field, MultipartFile upload) {
		String code = field.fieldCode();
		if (upload.isEmpty()) {
			throw invalid(code, "Choose a file to upload");
		}
		String name = cleanName(upload.getOriginalFilename());
		String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
		List<String> allowed = field.properties().allowedTypes();
		FileType type = FileType.ofExtension(extension)
				.filter(found -> allowed.contains(found.key()))
				.orElseThrow(() -> invalid(code, "File type is not allowed. Allowed types: " + String.join(", ", allowed)));
		int maxSizeMb = field.properties().maxSizeMb();
		if (upload.getSize() > maxSizeMb * 1024L * 1024L) {
			throw invalid(code, "File is too large. Maximum size is " + maxSizeMb + " MB");
		}

		String storedPath = submission.getFormId() + "/" + submission.getId() + "/" + UUID.randomUUID() + "." + type.key();
		Path target = root.resolve(storedPath);
		try {
			try (InputStream content = upload.getInputStream()) {
				if (!type.matchesContent(content.readNBytes(FileType.SIGNATURE_LENGTH))) {
					throw invalid(code, "File content does not match its type");
				}
			}
			Files.createDirectories(target.getParent());
			try (InputStream content = upload.getInputStream()) {
				Files.copy(content, target);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("Could not store the uploaded file", ex);
		}

		fileRepository.findBySubmissionIdAndFieldCode(submission.getId(), code).ifPresent(previous -> {
			remove(previous);
			// The unique key on (submission, field) needs the old row gone before the new one is inserted
			fileRepository.flush();
		});
		SubmissionFile file = new SubmissionFile();
		file.setSubmissionId(submission.getId());
		file.setFieldCode(code);
		file.setOriginalName(name);
		file.setStoredPath(storedPath);
		file.setContentType(type.contentType());
		file.setSizeBytes(upload.getSize());
		return SubmissionFileResponse.from(fileRepository.save(file));
	}

	/** The files uploaded for a submission, keyed by field code, in the shape stored as the field's answer. */
	public Map<String, JsonNode> answers(Long submissionId) {
		Map<String, JsonNode> answers = new HashMap<>();
		for (SubmissionFile file : fileRepository.findBySubmissionId(submissionId)) {
			answers.put(file.getFieldCode(), objectMapper.valueToTree(SubmissionFileResponse.from(file)));
		}
		return answers;
	}

	/** Deletes the uploads that did not become part of the stored answers (hidden, disabled or left out fields). */
	@Transactional
	public void removeUnused(Long submissionId, Set<String> keptFieldCodes) {
		fileRepository.findBySubmissionId(submissionId).stream()
				.filter(file -> !keptFieldCodes.contains(file.getFieldCode()))
				.forEach(this::remove);
	}

	@Transactional(readOnly = true)
	public FileDownload download(Long submissionId, Long fileId) {
		SubmissionFile file = submissionRepository.findByIdAndStatus(submissionId, SubmissionStatus.SUBMITTED)
				.flatMap(submission -> fileRepository.findByIdAndSubmissionId(fileId, submissionId))
				.orElseThrow(() -> new ResourceNotFoundException("File not found"));
		FileSystemResource resource = new FileSystemResource(root.resolve(file.getStoredPath()));
		if (!resource.exists()) {
			throw new ResourceNotFoundException("File not found");
		}
		return new FileDownload(file.getOriginalName(), file.getContentType(), file.getSizeBytes(), resource);
	}

	private void remove(SubmissionFile file) {
		fileRepository.delete(file);
		try {
			Files.deleteIfExists(root.resolve(file.getStoredPath()));
		} catch (IOException ex) {
			log.warn("Could not delete uploaded file {}", file.getStoredPath(), ex);
		}
	}

	// Only the last path segment is kept, so a name like "..\..\x.pdf" cannot say anything about folders
	private String cleanName(String original) {
		String name = original == null ? "" : original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
		name = name.replaceAll("\\p{Cntrl}", "").trim();
		if (name.isEmpty()) {
			return "file";
		}
		return name.length() > 255 ? name.substring(name.length() - 255) : name;
	}

	private FieldErrorsException invalid(String fieldCode, String message) {
		return new FieldErrorsException("Validation failed", Map.of(fieldCode, message));
	}
}
