package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.DocumentStatusRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.DocumentResponse;
import com.autoconsultancy.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload/{applicationId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            Authentication authentication) {
        DocumentResponse response = documentService.uploadDocument(applicationId, file, documentType, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByApplicationId(
            @PathVariable Long applicationId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentsByApplicationId(applicationId, authentication.getName()), 
                "Fetched documents"
        ));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'WORKER', 'ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId, Authentication authentication) {
        Resource resource = documentService.downloadDocument(documentId, authentication.getName());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping("/{documentId}/status")
    @PreAuthorize("hasAnyAuthority('WORKER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateStatus(
            @PathVariable Long documentId,
            @RequestBody DocumentStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.updateStatus(documentId, request, authentication.getName()), 
                "Status updated"
        ));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long documentId, Authentication authentication) {
        documentService.deleteDocument(documentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}
