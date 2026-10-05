package com.manacommunity.api.pet.controller;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.pet.dto.PetDto;
import com.manacommunity.api.pet.entity.Pet;
import com.manacommunity.api.pet.repository.PetRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/pets", "/pets"})
@RequiredArgsConstructor
public class PetController {

    private final PetRepository petRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<PetDto>> getCommunityPets(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            return ResponseEntity.ok(List.of());
        }
        List<PetDto> pets = petRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                        user.getCommunity().getId(), "ACTIVE")
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PetDto>> getMyPets(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(
                petRepo.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                        .stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDto> getPet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Pet pet = petRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
        return ResponseEntity.ok(toDto(pet));
    }

    @PostMapping
    public ResponseEntity<PetDto> registerPet(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            return ResponseEntity.badRequest().build();
        }
        Pet pet = Pet.builder()
                .community(user.getCommunity())
                .owner(user)
                .name(getString(body, "name", "Pet"))
                .species(getString(body, "species", "DOG"))
                .breed(getString(body, "breed", null))
                .color(getString(body, "color", null))
                .microchipId(getString(body, "microchipId", null))
                .imageUrl(getString(body, "imageUrl", null))
                .notes(getString(body, "notes", null))
                .vaccinated(Boolean.TRUE.equals(body.get("isVaccinated")))
                .status("ACTIVE")
                .registrationDate(LocalDate.now())
                .build();
        if (body.get("vaccineExpiryDate") instanceof String s && !s.isBlank()) {
            pet.setVaccineExpiryDate(LocalDate.parse(s));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(petRepo.save(pet)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetDto> updatePet(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Pet pet = petRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
        if (!pet.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body.containsKey("name"))        pet.setName(getString(body, "name", pet.getName()));
        if (body.containsKey("species"))     pet.setSpecies(getString(body, "species", pet.getSpecies()));
        if (body.containsKey("breed"))       pet.setBreed(getString(body, "breed", pet.getBreed()));
        if (body.containsKey("isVaccinated")) pet.setVaccinated(Boolean.TRUE.equals(body.get("isVaccinated")));
        if (body.containsKey("status"))      pet.setStatus(getString(body, "status", pet.getStatus()));
        return ResponseEntity.ok(toDto(petRepo.save(pet)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Pet pet = petRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
        if (!pet.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        pet.setStatus("INACTIVE");
        petRepo.save(pet);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PetDto toDto(Pet p) {
        return PetDto.builder()
                .id(p.getId())
                .name(p.getName())
                .species(p.getSpecies())
                .breed(p.getBreed())
                .color(p.getColor())
                .ageMonths(p.getAgeMonths())
                .weightKg(p.getWeightKg())
                .microchipId(p.getMicrochipId())
                .isVaccinated(p.isVaccinated())
                .vaccineExpiryDate(p.getVaccineExpiryDate())
                .registrationDate(p.getRegistrationDate())
                .status(p.getStatus())
                .imageUrl(p.getImageUrl())
                .ownerName(p.getOwner().getFullName())
                .ownerFlat(p.getOwner().getFlatNo())
                .communityId(p.getCommunity() != null ? p.getCommunity().getId() : null)
                .build();
    }

    private String getString(Map<String, Object> body, String key, String fallback) {
        Object val = body.get(key);
        return val instanceof String s ? s : fallback;
    }
}
