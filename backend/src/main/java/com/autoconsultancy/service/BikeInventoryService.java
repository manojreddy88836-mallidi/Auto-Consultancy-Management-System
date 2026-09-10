package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.BikeInventoryRequest;
import com.autoconsultancy.dto.response.BikeInventoryImageResponse;
import com.autoconsultancy.dto.response.BikeInventoryResponse;
import com.autoconsultancy.entity.BikeInventory;
import com.autoconsultancy.entity.BikeInventory.SaleStatus;
import com.autoconsultancy.entity.BikeInventoryImage;
import com.autoconsultancy.entity.BikeModel;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.repository.BikeInventoryImageRepository;
import com.autoconsultancy.repository.BikeInventoryRepository;
import com.autoconsultancy.repository.BikeModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BikeInventoryService {

    private final BikeInventoryRepository inventoryRepository;
    private final BikeInventoryImageRepository imageRepository;
    private final BikeModelRepository bikeModelRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BikeInventoryResponse create(BikeInventoryRequest req) {
        BikeModel model = bikeModelRepository.findById(req.getBikeModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Bike model not found: " + req.getBikeModelId()));

        BikeInventory inv = BikeInventory.builder()
                .bikeModel(model)
                .bikeCode(req.getBikeCode())
                .registrationNumber(req.getRegistrationNumber())
                .price(req.getPrice())
                .year(req.getYear())
                .color(req.getColor())
                .kmDriven(req.getKmDriven())
                .fuelType(req.getFuelType() != null ? req.getFuelType() : model.getFuelType())
                .conditionType(req.getConditionType())
                .description(req.getDescription())
                .saleStatus(SaleStatus.NOT_FOR_SALE)
                .active(true)
                .build();

        return mapToResponse(inventoryRepository.save(inv), false);
    }

    @Transactional
    public BikeInventoryResponse update(Long id, BikeInventoryRequest req) {
        BikeInventory inv = getActive(id);

        if (req.getBikeModelId() != null && !req.getBikeModelId().equals(inv.getBikeModel().getId())) {
            BikeModel model = bikeModelRepository.findById(req.getBikeModelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bike model not found"));
            inv.setBikeModel(model);
        }
        if (req.getBikeCode() != null)           inv.setBikeCode(req.getBikeCode());
        if (req.getRegistrationNumber() != null) inv.setRegistrationNumber(req.getRegistrationNumber());
        if (req.getPrice() != null)              inv.setPrice(req.getPrice());
        if (req.getYear() != null)               inv.setYear(req.getYear());
        if (req.getColor() != null)              inv.setColor(req.getColor());
        if (req.getKmDriven() != null)           inv.setKmDriven(req.getKmDriven());
        if (req.getFuelType() != null)           inv.setFuelType(req.getFuelType());
        if (req.getConditionType() != null)      inv.setConditionType(req.getConditionType());
        if (req.getDescription() != null)        inv.setDescription(req.getDescription());

        return mapToResponse(inventoryRepository.save(inv), false);
    }

    @Transactional
    public void delete(Long id) {
        BikeInventory inv = getActive(id);
        inv.setActive(false);
        inventoryRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public BikeInventoryResponse getById(Long id) {
        BikeInventory inv = inventoryRepository.findById(id)
                .filter(BikeInventory::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Bike inventory not found: " + id));
        return mapToResponse(inv, true);
    }

    /** Admin: paginated list with filters */
    @Transactional(readOnly = true)
    public Page<BikeInventoryResponse> getAll(Long modelId, Long manufacturerId, String saleStatus, String search, Pageable pageable) {
        return inventoryRepository.findAllAdmin(modelId, manufacturerId, saleStatus, search, pageable)
                .map(inv -> mapToResponse(inv, false));
    }

    /** Customer: only AVAILABLE bikes with at least one image */
    @Transactional(readOnly = true)
    public List<BikeInventoryResponse> getAvailableForCustomers(Long manufacturerId, String search) {
        List<BikeInventory> list = manufacturerId != null
                ? inventoryRepository.findAvailableWithImagesByManufacturer(manufacturerId)
                : inventoryRepository.findAvailableWithImages();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(inv ->
                    inv.getBikeModel().getModelName().toLowerCase().contains(q) ||
                    inv.getBikeModel().getManufacturer().getName().toLowerCase().contains(q) ||
                    (inv.getBikeCode() != null && inv.getBikeCode().toLowerCase().contains(q)) ||
                    (inv.getColor() != null && inv.getColor().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        return list.stream().map(inv -> mapToResponse(inv, false)).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALE STATUS — with backend image validation
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BikeInventoryResponse enableSale(Long id) {
        BikeInventory inv = getActive(id);

        // BACKEND VALIDATION: must have at least one image
        long imgCount = imageRepository.countByBikeInventoryId(id);
        if (imgCount == 0) {
            throw new BadRequestException(
                "Bike cannot be listed for sale. Please upload at least one bike image.");
        }

        inv.setSaleStatus(SaleStatus.AVAILABLE);
        return mapToResponse(inventoryRepository.save(inv), false);
    }

    @Transactional
    public BikeInventoryResponse disableSale(Long id) {
        BikeInventory inv = getActive(id);
        inv.setSaleStatus(SaleStatus.NOT_FOR_SALE);
        return mapToResponse(inventoryRepository.save(inv), false);
    }

    @Transactional
    public BikeInventoryResponse reserve(Long id) {
        BikeInventory inv = getActive(id);
        if (inv.getSaleStatus() == SaleStatus.SOLD) {
            throw new BadRequestException("Bike is already sold.");
        }
        inv.setSaleStatus(SaleStatus.RESERVED);
        return mapToResponse(inventoryRepository.save(inv), false);
    }

    @Transactional
    public BikeInventoryResponse markSold(Long id) {
        BikeInventory inv = getActive(id);
        inv.setSaleStatus(SaleStatus.SOLD);
        return mapToResponse(inventoryRepository.save(inv), false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMAGE UPLOAD / MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BikeInventoryImageResponse uploadImage(Long inventoryId, MultipartFile file, boolean setPrimary) {
        BikeInventory inv = getActive(inventoryId);

        // Validate
        if (file.isEmpty()) throw new BadRequestException("File is empty.");
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_TYPES.contains(mime.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Allowed: JPG, PNG, WEBP.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File too large. Max size is 10 MB.");
        }

        try {
            // Build storage path: uploads/bike-inventory/{inventoryId}/{uuid}.ext
            String ext = getExtension(file.getOriginalFilename());
            String uuid = UUID.randomUUID().toString();
            String stored = uuid + "." + ext;

            Path dir = Paths.get(uploadDir, "bike-inventory", String.valueOf(inventoryId));
            Files.createDirectories(dir);
            Path dest = dir.resolve(stored);
            file.transferTo(dest.toFile());

            // If setPrimary, clear old primary
            if (setPrimary) {
                imageRepository.findByBikeInventoryIdAndPrimaryTrue(inventoryId)
                        .ifPresent(img -> { img.setPrimary(false); imageRepository.save(img); });
            }

            BikeInventoryImage img = BikeInventoryImage.builder()
                    .bikeInventory(inv)
                    .filePath(dest.toString())
                    .fileName(stored)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(mime)
                    .primary(setPrimary)
                    .build();

            BikeInventoryImage saved = imageRepository.save(img);
            return mapImageToResponse(saved, inventoryId);

        } catch (IOException e) {
            log.error("Failed to store bike inventory image", e);
            throw new RuntimeException("Failed to store image: " + e.getMessage());
        }
    }

    public List<BikeInventoryImageResponse> getImages(Long inventoryId) {
        getActive(inventoryId); // verify exists
        return imageRepository.findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(inventoryId)
                .stream().map(img -> mapImageToResponse(img, inventoryId))
                .collect(Collectors.toList());
    }

    @Transactional
    public BikeInventoryImageResponse setPrimary(Long inventoryId, Long imageId) {
        getActive(inventoryId);
        // Clear old primary
        imageRepository.findByBikeInventoryIdAndPrimaryTrue(inventoryId)
                .ifPresent(img -> { img.setPrimary(false); imageRepository.save(img); });
        // Set new primary
        BikeInventoryImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        if (!img.getBikeInventory().getId().equals(inventoryId)) {
            throw new BadRequestException("Image does not belong to this bike.");
        }
        img.setPrimary(true);
        return mapImageToResponse(imageRepository.save(img), inventoryId);
    }

    @Transactional
    public void deleteImage(Long inventoryId, Long imageId) {
        BikeInventory inv = getActive(inventoryId);
        BikeInventoryImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        if (!img.getBikeInventory().getId().equals(inventoryId)) {
            throw new BadRequestException("Image does not belong to this bike.");
        }

        // Delete physical file
        try {
            Path p = Paths.get(img.getFilePath());
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Could not delete image file: {}", img.getFilePath());
        }

        boolean wasPrimary = img.isPrimary();
        imageRepository.delete(img);

        // If deleted image was primary, promote next image as primary
        if (wasPrimary) {
            imageRepository.findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(inventoryId)
                    .stream().findFirst().ifPresent(next -> {
                        next.setPrimary(true);
                        imageRepository.save(next);
                    });
        }

        // If no images remain, auto-disable sale
        long remaining = imageRepository.countByBikeInventoryId(inventoryId);
        if (remaining == 0 && inv.getSaleStatus() == SaleStatus.AVAILABLE) {
            inv.setSaleStatus(SaleStatus.NOT_FOR_SALE);
            inventoryRepository.save(inv);
            log.info("Bike {} auto-disabled for sale: no images remain", inventoryId);
        }
    }

    /** Serve image file as Resource for download */
    public Resource serveImage(Long inventoryId, Long imageId) {
        BikeInventoryImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        if (!img.getBikeInventory().getId().equals(inventoryId)) {
            throw new BadRequestException("Image does not belong to this bike.");
        }
        try {
            Path p = Paths.get(img.getFilePath());
            Resource res = new UrlResource(p.toUri());
            if (res.exists() && res.isReadable()) return res;
            throw new ResourceNotFoundException("Image file not found on disk");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read image file", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public BikeInventoryResponse mapToResponse(BikeInventory inv, boolean includeAllImages) {
        List<BikeInventoryImage> imgs = imageRepository
                .findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(inv.getId());

        String primaryUrl = imgs.stream()
                .filter(BikeInventoryImage::isPrimary)
                .findFirst()
                .or(() -> imgs.stream().findFirst())
                .map(img -> "/api/bikes/" + inv.getId() + "/images/" + img.getId() + "/file")
                .orElse(null);

        List<BikeInventoryImageResponse> imageResponses = includeAllImages
                ? imgs.stream().map(img -> mapImageToResponse(img, inv.getId())).collect(Collectors.toList())
                : null;

        BikeModel model = inv.getBikeModel();
        return BikeInventoryResponse.builder()
                .id(inv.getId())
                .bikeCode(inv.getBikeCode())
                .registrationNumber(inv.getRegistrationNumber())
                .bikeModelId(model.getId())
                .modelName(model.getModelName())
                .category(model.getCategory())
                .manufacturerId(model.getManufacturer().getId())
                .manufacturerName(model.getManufacturer().getName())
                .price(inv.getPrice())
                .year(inv.getYear())
                .color(inv.getColor())
                .kmDriven(inv.getKmDriven())
                .fuelType(inv.getFuelType())
                .conditionType(inv.getConditionType())
                .description(inv.getDescription())
                .saleStatus(inv.getSaleStatus().name())
                .active(inv.isActive())
                .imageCount((int) imgs.size())
                .primaryImageUrl(primaryUrl)
                .images(imageResponses)
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }

    private BikeInventoryImageResponse mapImageToResponse(BikeInventoryImage img, Long inventoryId) {
        return BikeInventoryImageResponse.builder()
                .id(img.getId())
                .bikeInventoryId(inventoryId)
                .fileName(img.getFileName())
                .originalFileName(img.getOriginalFileName())
                .fileSize(img.getFileSize())
                .mimeType(img.getMimeType())
                .primary(img.isPrimary())
                .imageUrl("/api/bikes/" + inventoryId + "/images/" + img.getId() + "/file")
                .createdAt(img.getCreatedAt())
                .build();
    }

    private BikeInventory getActive(Long id) {
        return inventoryRepository.findById(id)
                .filter(BikeInventory::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Bike inventory not found: " + id));
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "jpg";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
