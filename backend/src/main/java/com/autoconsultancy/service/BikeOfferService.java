package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.BikeOfferRequest;
import com.autoconsultancy.dto.request.OfferRespondRequest;
import com.autoconsultancy.dto.response.BikeOfferResponse;
import com.autoconsultancy.entity.*;
import com.autoconsultancy.entity.BikeOffer.OfferStatus;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.ResourceNotFoundException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BikeOfferService {

    private final BikeOfferRepository     offerRepository;
    private final BikeInventoryRepository bikeInventoryRepository;
    private final BikeInventoryImageRepository bikeInventoryImageRepository;
    private final CustomerRepository      customerRepository;
    private final UserRepository          userRepository;

    // ── Customer: Submit Offer ─────────────────────────────────────────────
    @Transactional
    public BikeOfferResponse submitOffer(String email, BikeOfferRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Customer customer = customerRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // Validate offer amount
        if (req.getOfferedPrice() == null || req.getOfferedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Offer price must be greater than ₹0");
        }

        // Load & validate bike
        BikeInventory bike = bikeInventoryRepository.findById(req.getBikeInventoryId())
                .filter(BikeInventory::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found"));

        if (bike.getSaleStatus() != BikeInventory.SaleStatus.AVAILABLE) {
            throw new BadRequestException("This bike is no longer available for sale");
        }

        if (bike.getPrice() == null) {
            throw new BadRequestException("This bike does not have a listed price yet");
        }

        // Prevent duplicate active offers
        if (offerRepository.existsActiveOffer(customer.getId(), bike.getId())) {
            throw new BadRequestException(
                "You already have an active offer on this bike. " +
                "Withdraw or wait for a response before submitting a new one.");
        }

        BikeOffer offer = BikeOffer.builder()
                .bikeInventory(bike)
                .customer(customer)
                .listedPrice(bike.getPrice())          // snapshot — never changes
                .offeredPrice(req.getOfferedPrice())
                .customerMessage(req.getCustomerMessage())
                .status(OfferStatus.PENDING)
                .build();

        return toResponse(offerRepository.save(offer));
    }

    // ── Customer: My Offers ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BikeOfferResponse> getMyOffers(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return offerRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Customer: Get active offer for a specific bike ─────────────────────
    @Transactional(readOnly = true)
    public BikeOfferResponse getMyOfferForBike(String email, Long bikeInventoryId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<BikeOffer> offers = offerRepository.findByCustomerAndBike(user.getId(), bikeInventoryId);
        if (offers.isEmpty()) return null;
        return toResponse(offers.get(0)); // most recent
    }

    // ── Customer: Withdraw Offer ───────────────────────────────────────────
    @Transactional
    public BikeOfferResponse withdrawOffer(Long offerId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BikeOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only withdraw your own offers");
        }
        if (offer.getStatus() != OfferStatus.PENDING && offer.getStatus() != OfferStatus.COUNTER_OFFER) {
            throw new BadRequestException("Only PENDING or COUNTER_OFFER offers can be withdrawn");
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        return toResponse(offerRepository.save(offer));
    }

    // ── Customer: Respond to Counter Offer ────────────────────────────────
    @Transactional
    public BikeOfferResponse respondToCounter(Long offerId, boolean accept, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BikeOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getCustomer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only respond to your own offers");
        }
        if (offer.getStatus() != OfferStatus.COUNTER_OFFER) {
            throw new BadRequestException("No counter offer to respond to");
        }

        offer.setStatus(accept ? OfferStatus.COUNTER_ACCEPTED : OfferStatus.COUNTER_REJECTED);
        // Record agreed price when customer accepts the counter
        if (accept && offer.getCounterOfferPrice() != null) {
            offer.setAgreedPrice(offer.getCounterOfferPrice());
        }
        return toResponse(offerRepository.save(offer));
    }

    // ── Admin/Worker: Get All Offers (paginated) ───────────────────────────
    @Transactional(readOnly = true)
    public Page<BikeOfferResponse> getAllOffers(String statusFilter, Pageable pageable) {
        Page<BikeOffer> page;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                OfferStatus status = OfferStatus.valueOf(statusFilter.toUpperCase());
                page = offerRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
            } catch (IllegalArgumentException e) {
                page = offerRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            page = offerRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    // ── Admin/Worker: Get Offer Detail ────────────────────────────────────
    @Transactional(readOnly = true)
    public BikeOfferResponse getOfferById(Long offerId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BikeOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        // Customer can only see their own offers
        if (user.getRole() == Role.CUSTOMER) {
            if (!offer.getCustomer().getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Access denied");
            }
        }
        return toResponse(offer);
    }

    // ── Admin/Worker: Respond (Accept / Reject / Counter) ────────────────
    @Transactional
    public BikeOfferResponse respondToOffer(Long offerId, OfferRespondRequest req, String email) {
        User responder = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BikeOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getStatus() != OfferStatus.PENDING && offer.getStatus() != OfferStatus.COUNTER_OFFER) {
            throw new BadRequestException(
                "Can only respond to offers in PENDING or COUNTER_OFFER status. Current: " + offer.getStatus());
        }

        String action = req.getAction() == null ? "" : req.getAction().toUpperCase().trim();
        switch (action) {
            case "ACCEPTED" -> {
                offer.setStatus(OfferStatus.ACCEPTED);
                // Record agreed price = customer's original offer price
                offer.setAgreedPrice(offer.getOfferedPrice());
            }
            case "REJECTED" -> offer.setStatus(OfferStatus.REJECTED);
            case "COUNTER_OFFER" -> {
                if (req.getCounterOfferPrice() == null || req.getCounterOfferPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Counter offer price must be greater than ₹0");
                }
                offer.setCounterOfferPrice(req.getCounterOfferPrice());
                offer.setStatus(OfferStatus.COUNTER_OFFER);
            }
            default -> throw new BadRequestException(
                "Invalid action '" + req.getAction() + "'. Use ACCEPTED, REJECTED, or COUNTER_OFFER");
        }

        offer.setAdminResponse(req.getAdminResponse());
        offer.setRespondedBy(responder);
        return toResponse(offerRepository.save(offer));
    }

    // ── Mapping ───────────────────────────────────────────────────────────
    private BikeOfferResponse toResponse(BikeOffer o) {
        BikeInventory bike = o.getBikeInventory();
        BikeModel     model = bike != null ? bike.getBikeModel() : null;

        // Primary image URL
        String imageUrl = null;
        if (bike != null) {
            imageUrl = bikeInventoryImageRepository
                    .findByBikeInventoryIdAndPrimaryTrue(bike.getId())
                    .or(() -> bikeInventoryImageRepository
                            .findByBikeInventoryIdOrderByPrimaryDescCreatedAtAsc(bike.getId())
                            .stream().findFirst())
                    .map(img -> "/api/bikes/" + bike.getId() + "/images/" + img.getId() + "/file")
                    .orElse(null);
        }

        String variantName = null;
        if (model != null && model.getVariants() != null && !model.getVariants().isEmpty()) {
            variantName = model.getVariants().get(0).getVariantName();
        }

        Customer cust = o.getCustomer();
        User custUser  = cust != null ? cust.getUser() : null;
        User respUser  = o.getRespondedBy();

        return BikeOfferResponse.builder()
                .id(o.getId())
                // bike
                .bikeInventoryId(bike != null ? bike.getId() : null)
                .bikeCode(bike != null ? bike.getBikeCode() : null)
                .manufacturerName(model != null && model.getManufacturer() != null ? model.getManufacturer().getName() : null)
                .modelName(model != null ? model.getModelName() : null)
                .variantName(variantName)
                .manufacturingYear(bike != null ? bike.getYear() : null)
                .registrationNumber(bike != null ? bike.getRegistrationNumber() : null)
                .colour(bike != null ? bike.getColor() : null)
                .fuelType(bike != null ? bike.getFuelType() : null)
                .conditionType(bike != null ? bike.getConditionType() : null)
                .bikeImageUrl(imageUrl)
                // prices
                .listedPrice(o.getListedPrice())
                .offeredPrice(o.getOfferedPrice())
                .counterOfferPrice(o.getCounterOfferPrice())
                .agreedPrice(o.getAgreedPrice())
                // status
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .customerMessage(o.getCustomerMessage())
                .adminResponse(o.getAdminResponse())
                // customer
                .customerId(cust != null ? cust.getId() : null)
                .customerName(custUser != null ? custUser.getFirstName() + " " + custUser.getLastName() : null)
                .customerEmail(custUser != null ? custUser.getEmail() : null)
                .customerPhone(custUser != null ? custUser.getPhone() : null)
                // responder
                .respondedByName(respUser != null ? respUser.getFirstName() + " " + respUser.getLastName() : null)
                // timestamps
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
