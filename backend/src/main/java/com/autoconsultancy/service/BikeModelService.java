package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.BikeModelRequest;
import com.autoconsultancy.dto.request.BikeVariantRequest;
import com.autoconsultancy.dto.request.ManufacturingYearRequest;
import com.autoconsultancy.dto.response.BikeImageResponse;
import com.autoconsultancy.dto.response.BikeModelResponse;
import com.autoconsultancy.dto.response.BikeVariantResponse;
import com.autoconsultancy.entity.*;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BikeModelService {

    private final BikeModelRepository bikeModelRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final BikeVariantRepository bikeVariantRepository;
    private final ManufacturingYearRepository manufacturingYearRepository;
    private final BikeImageRepository bikeImageRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // ── Allowed image types ──────────────────────────────────────────────
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L; // 10 MB

    // ── URL builder ─────────────────────────────────────────────────────
    private String buildImageUrl(Long imageId) {
        return "/api/bike-models/images/" + imageId + "/file";
    }

    // ── Inventory-count helper ───────────────────────────────────────────
    /**
     * Build a modelId -> availableCount map from a single GROUP BY query.
     * Used by bulk list methods to avoid N+1.
     */
    private Map<Long, Long> buildInventoryCountMap() {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : bikeModelRepository.countAvailableInventoryPerModel()) {
            Long modelId = ((Number) row[0]).longValue();
            Long count   = ((Number) row[1]).longValue();
            map.put(modelId, count);
        }
        return map;
    }

    // ── Mapper ──────────────────────────────────────────────────────────
    private BikeImageResponse mapImage(BikeImage img) {
        return BikeImageResponse.builder()
                .id(img.getId())
                .bikeModelId(img.getBikeModel().getId())
                .fileName(img.getFileName())
                .originalFileName(img.getOriginalFileName())
                .primary(img.isPrimary())
                .fileSize(img.getFileSize())
                .mimeType(img.getMimeType())
                .imageUrl(buildImageUrl(img.getId()))
                .createdAt(img.getCreatedAt())
                .build();
    }

    /**
     * Map a BikeModel to a response.
     * availableInventoryCount: pass the pre-fetched count from the batch map.
     *   - Null means "not yet looked up" -> will be fetched individually.
     *   - 0L  means "looked up, none available".
     */
    private BikeModelResponse mapToResponse(BikeModel model, Long availableInventoryCount) {
        List<BikeImage> imgs = model.getImages() != null ? model.getImages() : List.of();
        String primaryUrl = imgs.stream()
                .filter(BikeImage::isPrimary)
                .findFirst()
                .or(() -> imgs.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(imgs.get(0)))
                .map(i -> buildImageUrl(i.getId()))
                .orElse(null);

        // ── Derived active status from inventory ──────────────────────────
        // If count not passed in, fetch it now (single-model scenario)
        long invCount = (availableInventoryCount != null)
                ? availableInventoryCount
                : bikeModelRepository.countAvailableInventoryForModel(model.getId());
        boolean derivedActive = invCount > 0;

        BikeModel.SaleStatus status = model.getSaleStatus() != null ? model.getSaleStatus() : BikeModel.SaleStatus.NOT_FOR_SALE;
        boolean forSale = model.getAvailableForSale() != null ? model.getAvailableForSale() : false;

        return BikeModelResponse.builder()
                .id(model.getId())
                .manufacturerId(model.getManufacturer().getId())
                .manufacturerName(model.getManufacturer().getName())
                .modelName(model.getModelName())
                .category(model.getCategory())
                .fuelType(model.getFuelType())
                .active(derivedActive)
                .availableInventoryCount(invCount)
                .availableForSale(forSale)
                .saleStatus(status.name())
                .price(model.getPrice())
                .images(imgs.stream().map(this::mapImage).collect(Collectors.toList()))
                .primaryImageUrl(primaryUrl)
                .variants(model.getVariants() != null ? model.getVariants().stream()
                        .map(v -> BikeVariantResponse.builder()
                                .id(v.getId())
                                .variantName(v.getVariantName())
                                .engineCC(v.getEngineCC())
                                .active(v.isActive())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .years(model.getManufacturingYears() != null ? model.getManufacturingYears().stream()
                        .map(ManufacturingYear::getYear)
                        .collect(Collectors.toList()) : List.of())
                .createdAt(model.getCreatedAt())
                .build();
    }

    /** Convenience overload: fetch count individually */
    private BikeModelResponse mapToResponse(BikeModel model) {
        return mapToResponse(model, null);
    }

    // ── Public: models by manufacturer ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<BikeModelResponse> getByManufacturerId(Long manufacturerId) {
        List<BikeModel> models = bikeModelRepository.findByManufacturerIdAndActiveTrue(manufacturerId);
        Map<Long, Long> countMap = buildInventoryCountMap();
        return models.stream()
                .map(m -> mapToResponse(m, countMap.getOrDefault(m.getId(), 0L)))
                .collect(Collectors.toList());
    }

    // ── Public: AVAILABLE bikes for sale ───────────────────────────────
    @Transactional(readOnly = true)
    public List<BikeModelResponse> getAvailableForSale(String search, Long manufacturerId) {
        List<BikeModel> all = bikeModelRepository.findBySaleStatusAndActiveTrueAndAvailableForSaleTrue(
                BikeModel.SaleStatus.AVAILABLE);

        // Optional in-memory filter
        if (manufacturerId != null) {
            all = all.stream().filter(m -> m.getManufacturer().getId().equals(manufacturerId)).collect(Collectors.toList());
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            all = all.stream().filter(m ->
                    m.getModelName().toLowerCase().contains(q) ||
                    m.getManufacturer().getName().toLowerCase().contains(q) ||
                    (m.getCategory() != null && m.getCategory().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        Map<Long, Long> countMap = buildInventoryCountMap();
        return all.stream()
                .map(m -> mapToResponse(m, countMap.getOrDefault(m.getId(), 0L)))
                .collect(Collectors.toList());
    }

    // ── Public: single bike detail ─────────────────────────────────────
    @Transactional(readOnly = true)
    public BikeModelResponse getPublicDetail(Long id) {
        BikeModel model = bikeModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found"));
        return mapToResponse(model);
    }

    // ── Admin: variants ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BikeVariantResponse> getVariants(Long modelId) {
        return bikeVariantRepository.findByBikeModelIdAndActiveTrue(modelId).stream()
                .map(v -> BikeVariantResponse.builder()
                        .id(v.getId())
                        .variantName(v.getVariantName())
                        .engineCC(v.getEngineCC())
                        .active(v.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Integer> getYears(Long modelId) {
        return manufacturingYearRepository.findByBikeModelIdAndActiveTrue(modelId).stream()
                .map(ManufacturingYear::getYear)
                .collect(Collectors.toList());
    }

    // ── Admin: all models (paginated) ───────────────────────────────────
    @Transactional(readOnly = true)
    public Page<BikeModelResponse> getAll(Long manufacturerId, String search, Pageable pageable) {
        Page<BikeModel> models = bikeModelRepository.findAll(pageable);
        Map<Long, Long> countMap = buildInventoryCountMap();
        return models.map(m -> mapToResponse(m, countMap.getOrDefault(m.getId(), 0L)));
    }

    // ── Admin: create ───────────────────────────────────────────────────
    @Transactional
    public BikeModelResponse create(BikeModelRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));

        BikeModel.SaleStatus status = resolveSaleStatus(request.getSaleStatus(), request.getAvailableForSale());

        BikeModel model = BikeModel.builder()
                .manufacturer(manufacturer)
                .modelName(request.getModelName())
                .category(request.getCategory())
                .fuelType(request.getFuelType())
                .active(request.getActive() != null ? request.getActive() : true)
                .availableForSale(request.getAvailableForSale() != null ? request.getAvailableForSale() : false)
                .saleStatus(status)
                .price(request.getPrice())
                .build();

        return mapToResponse(bikeModelRepository.save(model));
    }

    // ── Admin: update ───────────────────────────────────────────────────
    @Transactional
    public BikeModelResponse update(Long id, BikeModelRequest request) {
        BikeModel model = bikeModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));

        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer not found"));

        model.setManufacturer(manufacturer);
        model.setModelName(request.getModelName());
        model.setCategory(request.getCategory());
        model.setFuelType(request.getFuelType());
        if (request.getActive() != null) model.setActive(request.getActive());
        if (request.getPrice() != null) model.setPrice(request.getPrice());

        if (request.getAvailableForSale() != null) {
            model.setAvailableForSale(request.getAvailableForSale());
        }
        if (request.getSaleStatus() != null) {
            Boolean avail = model.getAvailableForSale();
            model.setSaleStatus(resolveSaleStatus(request.getSaleStatus(), avail != null ? avail : false));
        }

        return mapToResponse(bikeModelRepository.save(model));
    }

    // ── Admin: update sale status ────────────────────────────────────────
    @Transactional
    public BikeModelResponse updateSaleStatus(Long id, String statusStr) {
        BikeModel model = bikeModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));

        try {
            BikeModel.SaleStatus status = BikeModel.SaleStatus.valueOf(statusStr.toUpperCase());
            model.setSaleStatus(status);
            model.setAvailableForSale(status == BikeModel.SaleStatus.AVAILABLE);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid sale status: " + statusStr);
        }

        return mapToResponse(bikeModelRepository.save(model));
    }

    // ── Admin: deactivate ────────────────────────────────────────────────
    @Transactional
    public void deactivate(Long id) {
        BikeModel model = bikeModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));
        model.setActive(false);
        bikeModelRepository.save(model);
    }

    // ── Image: upload ────────────────────────────────────────────────────
    @Transactional
    public BikeImageResponse uploadImage(Long modelId, MultipartFile file, boolean setPrimary) {
        BikeModel model = bikeModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));

        validateImageFile(file);

        try {
            String originalFileName = file.getOriginalFilename();
            String ext = getExtension(originalFileName);
            String fileName = UUID.randomUUID().toString() + ext;

            Path uploadPath = Paths.get(uploadDir, "bike-images", String.valueOf(modelId));
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // If this is the first image or setPrimary requested, unset any existing primary
            boolean isFirstImage = bikeImageRepository.countByBikeModelId(modelId) == 0;
            if (setPrimary || isFirstImage) {
                // Unset existing primary
                bikeImageRepository.findByBikeModelIdAndPrimaryTrue(modelId)
                        .ifPresent(img -> { img.setPrimary(false); bikeImageRepository.save(img); });
            }

            BikeImage img = BikeImage.builder()
                    .bikeModel(model)
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .primary(setPrimary || isFirstImage)
                    .build();

            return mapImage(bikeImageRepository.save(img));
        } catch (IOException e) {
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        }
    }

    // ── Image: list ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BikeImageResponse> getImages(Long modelId) {
        return bikeImageRepository.findByBikeModelIdOrderByPrimaryDescCreatedAtAsc(modelId)
                .stream().map(this::mapImage).collect(Collectors.toList());
    }

    // ── Image: serve file ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Resource getImageFile(Long imageId) {
        BikeImage img = bikeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        File file = new File(img.getFilePath());
        if (!file.exists()) throw new ResourceNotFoundException("Image file not found on disk");
        return new FileSystemResource(file);
    }

    // ── Image: set primary ───────────────────────────────────────────────
    @Transactional
    public BikeImageResponse setPrimaryImage(Long modelId, Long imageId) {
        BikeImage img = bikeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        if (!img.getBikeModel().getId().equals(modelId))
            throw new BadRequestException("Image does not belong to this bike model");

        // Unset existing primary
        bikeImageRepository.findByBikeModelIdAndPrimaryTrue(modelId)
                .ifPresent(p -> { p.setPrimary(false); bikeImageRepository.save(p); });

        img.setPrimary(true);
        return mapImage(bikeImageRepository.save(img));
    }

    // ── Image: delete ────────────────────────────────────────────────────
    @Transactional
    public void deleteImage(Long modelId, Long imageId) {
        BikeImage img = bikeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        if (!img.getBikeModel().getId().equals(modelId))
            throw new BadRequestException("Image does not belong to this bike model");

        File file = new File(img.getFilePath());
        if (file.exists()) file.delete();

        boolean wasPrimary = img.isPrimary();
        bikeImageRepository.delete(img);

        // If deleted image was primary, auto-assign the next available as primary
        if (wasPrimary) {
            bikeImageRepository.findByBikeModelIdOrderByPrimaryDescCreatedAtAsc(modelId)
                    .stream().findFirst().ifPresent(next -> {
                        next.setPrimary(true);
                        bikeImageRepository.save(next);
                    });
        }
    }

    // ── Variant helpers ──────────────────────────────────────────────────
    @Transactional
    public BikeVariantResponse addVariant(Long modelId, BikeVariantRequest request) {
        BikeModel model = bikeModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));

        BikeVariant variant = BikeVariant.builder()
                .bikeModel(model)
                .variantName(request.getVariantName())
                .engineCC(request.getEngineCC())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        variant = bikeVariantRepository.save(variant);
        return BikeVariantResponse.builder()
                .id(variant.getId())
                .variantName(variant.getVariantName())
                .engineCC(variant.getEngineCC())
                .active(variant.isActive())
                .build();
    }

    @Transactional
    public BikeVariantResponse updateVariant(Long variantId, BikeVariantRequest request) {
        BikeVariant variant = bikeVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.setVariantName(request.getVariantName());
        variant.setEngineCC(request.getEngineCC());
        if (request.getActive() != null) variant.setActive(request.getActive());
        variant = bikeVariantRepository.save(variant);
        return BikeVariantResponse.builder()
                .id(variant.getId())
                .variantName(variant.getVariantName())
                .engineCC(variant.getEngineCC())
                .active(variant.isActive())
                .build();
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        bikeVariantRepository.deleteById(variantId);
    }

    @Transactional
    public void addYear(Long modelId, ManufacturingYearRequest request) {
        BikeModel model = bikeModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike Model not found"));
        ManufacturingYear year = ManufacturingYear.builder()
                .bikeModel(model)
                .year(request.getYear())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        manufacturingYearRepository.save(year);
    }

    @Transactional
    public void deleteYear(Long yearId) {
        manufacturingYearRepository.deleteById(yearId);
    }

    // ── Private helpers ──────────────────────────────────────────────────
    private BikeModel.SaleStatus resolveSaleStatus(String statusStr, Boolean availableForSale) {
        if (statusStr != null && !statusStr.isBlank()) {
            try { return BikeModel.SaleStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        if (Boolean.TRUE.equals(availableForSale)) return BikeModel.SaleStatus.AVAILABLE;
        return BikeModel.SaleStatus.NOT_FOR_SALE;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("Image file is empty");
        if (file.getSize() > MAX_IMAGE_SIZE)
            throw new BadRequestException("Image exceeds 10 MB limit");

        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIME_TYPES.contains(mime.toLowerCase()))
            throw new BadRequestException("Invalid file type. Allowed: JPG, JPEG, PNG, WEBP");

        String name = file.getOriginalFilename();
        if (name == null) throw new BadRequestException("Invalid file name");
        String ext = getExtension(name).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new BadRequestException("Invalid file extension. Allowed: .jpg, .jpeg, .png, .webp");
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
