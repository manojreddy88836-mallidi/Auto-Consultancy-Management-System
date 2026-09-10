package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.ManufacturerRequest;
import com.autoconsultancy.entity.Manufacturer;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.repository.ManufacturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    public List<Manufacturer> getAllActive() {
        return manufacturerRepository.findByActiveTrue();
    }

    public Page<Manufacturer> getAll(Pageable pageable) {
        return manufacturerRepository.findAll(pageable);
    }

    public Manufacturer getById(Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    public Manufacturer create(ManufacturerRequest request) {
        Manufacturer manufacturer = Manufacturer.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .country(request.getCountry())
                .active(request.isActive())
                .build();
        return manufacturerRepository.save(manufacturer);
    }

    public Manufacturer update(Long id, ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        
        manufacturer.setName(request.getName());
        manufacturer.setLogoUrl(request.getLogoUrl());
        manufacturer.setCountry(request.getCountry());
        manufacturer.setActive(request.isActive());
        
        return manufacturerRepository.save(manufacturer);
    }

    public void deactivate(Long id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        manufacturer.setActive(false);
        manufacturerRepository.save(manufacturer);
    }
}
