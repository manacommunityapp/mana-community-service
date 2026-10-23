package com.manacommunity.api.pet.repository;

import com.manacommunity.api.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<Pet> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Pet> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
