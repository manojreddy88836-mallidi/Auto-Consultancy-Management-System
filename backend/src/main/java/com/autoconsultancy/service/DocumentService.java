package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.DocumentStatusRequest;
import com.autoconsultancy.dto.response.DocumentResponse;
import com.autoconsultancy.entity.Application;
import com.autoconsultancy.entity.Document;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.ApplicationRepository;
import com.autoconsultancy.repository.DocumentRepository;
import com.autoconsultancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    // ── Allowlists ────────────────────────────────────────────────────────────

    /** Only these MIME types are accepted as document uploads. */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    /** Only these file extensions are accepted. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    /** Maximum upload size: 10 MB (Spring also enforces its own limit). */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    // ─────────────────────────────────────────────────────────────────────────

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final Tika tika = new Tika();   // Apache Tika for real MIME detection

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .applicationId(doc.getApplication().getId())
                .documentType(doc.getDocumentType())
                .originalFileName(doc.getOriginalFileName())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .status(doc.getStatus())
                .remarks(doc.getRemarks())
                .uploadedByName(doc.getUploadedByUser() != null
                        ? doc.getUploadedByUser().getFirstName() + " " + doc.getUploadedByUser().getLastName()
                        : null)
                .uploadedAt(doc.getUploadedAt())
                .reviewedAt(doc.getReviewedAt())
                .reviewedByName(doc.getReviewedBy() != null
                        ? doc.getReviewedBy().getFirstName() + " " + doc.getReviewedBy().getLastName()
                        : null)
                .build();
    }

    @Transactional
    public DocumentResponse uploadDocument(Long applicationId, MultipartFile file,
                                           String documentType, String userEmail) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        // IDOR — customers may only upload for their own application
        if (user.getRole().name().equals("CUSTOMER")
                && !application.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to upload for this application");
        }

        validateFile(file);

        try {
            // Detect real MIME type from file content (not from client header)
            String detectedMime;
            try (InputStream is = file.getInputStream()) {
                detectedMime = tika.detect(is);
            }
            if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
                throw new BadRequestException("File type not allowed. Accepted types: PDF, JPEG, PNG, WEBP, GIF");
            }

            // Generate a safe, random server-side filename; preserve only the extension
            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + ext;

            // Resolve upload directory and prevent path traversal
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path appDir  = baseDir.resolve(String.valueOf(applicationId)).normalize();

            // Ensure appDir is still inside baseDir
            if (!appDir.startsWith(baseDir)) {
                throw new BadRequestException("Invalid upload path");
            }

            Files.createDirectories(appDir);

            Path filePath = appDir.resolve(fileName).normalize();
            // Final check: filePath must still be inside appDir
            if (!filePath.startsWith(appDir)) {
                throw new BadRequestException("Invalid file path");
            }

            file.transferTo(filePath.toFile());

            // Store a sanitized original filename (strip any path components)
            String safeOriginalName = Paths.get(file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "upload").getFileName().toString();

            Document document = Document.builder()
                    .application(application)
                    .uploadedByUser(user)
                    .documentType(documentType)
                    .fileName(fileName)
                    .originalFileName(safeOriginalName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .mimeType(detectedMime)   // use server-detected MIME, not client-supplied
                    .status("UPLOADED")
                    .build();

            log.info("Document uploaded: applicationId={} userId={} type={} size={}",
                    applicationId, user.getId(), documentType, file.getSize());

            return mapToResponse(documentRepository.save(document));

        } catch (BadRequestException | UnauthorizedException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to store uploaded file for applicationId={}", applicationId);
            throw new BadRequestException("Failed to store the uploaded file. Please try again.");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByApplicationId(Long applicationId, String userEmail) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER")
                && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to view documents for this application");
        }
        if (user.getRole().name().equals("WORKER")
                && (app.getWorkerAssignment() == null
                || !app.getWorkerAssignment().getWorker().getUser().getId().equals(user.getId()))) {
            throw new UnauthorizedException("Not assigned to this application");
        }

        return documentRepository.findByApplicationId(applicationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, String userEmail) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Application app = doc.getApplication();
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER")
                && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to download");
        }
        if (user.getRole().name().equals("WORKER")
                && (app.getWorkerAssignment() == null
                || !app.getWorkerAssignment().getWorker().getUser().getId().equals(user.getId()))) {
            throw new UnauthorizedException("Not assigned to this application");
        }

        // Path traversal guard: ensure stored path is inside the upload directory
        Path baseDir    = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path storedPath = Paths.get(doc.getFilePath()).toAbsolutePath().normalize();

        if (!storedPath.startsWith(baseDir)) {
            log.error("Path traversal attempt detected for documentId={}", documentId);
            throw new ResourceNotFoundException("File not found");
        }

        File file = storedPath.toFile();
        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found on disk");
        }
        return new FileSystemResource(file);
    }

    @Transactional
    public DocumentResponse updateStatus(Long documentId, DocumentStatusRequest request, String userEmail) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        doc.setStatus(request.getStatus());
        doc.setRemarks(request.getRemarks());
        doc.setReviewedBy(user);
        doc.setReviewedAt(LocalDateTime.now());

        log.info("Document status updated: documentId={} status={} reviewedBy={}",
                documentId, request.getStatus(), user.getId());

        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long documentId, String userEmail) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER")) {
            if (!doc.getApplication().getCustomer().getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Not authorized");
            }
            if (!"UPLOADED".equals(doc.getStatus())) {
                throw new BadRequestException("Can only delete newly UPLOADED documents");
            }
        }

        // Path traversal guard
        Path baseDir    = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path storedPath = Paths.get(doc.getFilePath()).toAbsolutePath().normalize();

        if (storedPath.startsWith(baseDir)) {
            File file = storedPath.toFile();
            if (file.exists() && !file.delete()) {
                log.warn("Could not delete physical file at {}", storedPath);
            }
        } else {
            log.warn("Skipped deletion of file outside upload dir: {}", doc.getFilePath());
        }

        documentRepository.delete(doc);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Validates basic file constraints before touching the filesystem. */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("File size exceeds the 10 MB limit");
        }

        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BadRequestException(
                    "File extension not allowed. Accepted: .pdf, .jpg, .jpeg, .png, .webp, .gif");
        }
    }

    /** Safely extracts the lowercase extension from a filename. */
    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        // Strip path components first
        String name = Paths.get(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : "";
    }
}
