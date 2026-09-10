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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

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
                .uploadedByName(doc.getUploadedByUser() != null ? doc.getUploadedByUser().getFirstName() + " " + doc.getUploadedByUser().getLastName() : null)
                .uploadedAt(doc.getUploadedAt())
                .reviewedAt(doc.getReviewedAt())
                .reviewedByName(doc.getReviewedBy() != null ? doc.getReviewedBy().getFirstName() + " " + doc.getReviewedBy().getLastName() : null)
                .build();
    }

    @Transactional
    public DocumentResponse uploadDocument(Long applicationId, MultipartFile file, String documentType, String userEmail) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER") && !application.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to upload for this application");
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            Path uploadPath = Paths.get(uploadDir, String.valueOf(applicationId));
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            Document document = Document.builder()
                    .application(application)
                    .uploadedByUser(user)
                    .documentType(documentType)
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .status("UPLOADED")
                    .build();

            return mapToResponse(documentRepository.save(document));
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file " + file.getOriginalFilename());
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByApplicationId(Long applicationId, String userEmail) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER") && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to view documents for this application");
        }
        if (user.getRole().name().equals("WORKER") && (app.getWorkerAssignment() == null || !app.getWorkerAssignment().getWorker().getUser().getId().equals(user.getId()))) {
            throw new UnauthorizedException("Not assigned to this application");
        }

        return documentRepository.findByApplicationId(applicationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, String userEmail) {
        Document doc = documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Application app = doc.getApplication();
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER") && !app.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to download");
        }
        if (user.getRole().name().equals("WORKER") && (app.getWorkerAssignment() == null || !app.getWorkerAssignment().getWorker().getUser().getId().equals(user.getId()))) {
            throw new UnauthorizedException("Not assigned to this application");
        }

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found on disk");
        }
        return new FileSystemResource(file);
    }

    @Transactional
    public DocumentResponse updateStatus(Long documentId, DocumentStatusRequest request, String userEmail) {
        Document doc = documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        doc.setStatus(request.getStatus());
        doc.setRemarks(request.getRemarks());
        doc.setReviewedBy(user);
        doc.setReviewedAt(LocalDateTime.now());
        
        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long documentId, String userEmail) {
        Document doc = documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getRole().name().equals("CUSTOMER")) {
            if (!doc.getApplication().getCustomer().getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Not authorized");
            }
            if (!"UPLOADED".equals(doc.getStatus())) {
                throw new BadRequestException("Can only delete newly UPLOADED documents");
            }
        }
        
        File file = new File(doc.getFilePath());
        if (file.exists()) {
            file.delete();
        }
        
        documentRepository.delete(doc);
    }
}
