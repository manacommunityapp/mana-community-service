package com.manacommunity.api.commerce.customer.repository;

import com.manacommunity.api.commerce.customer.model.CommerceCustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommerceCustomerAddressRepository extends JpaRepository<CommerceCustomerAddress, Long> {

    List<CommerceCustomerAddress> findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(Long customerId);

    Optional<CommerceCustomerAddress> findByCustomerIdAndIsDefaultTrue(Long customerId);

    /** Clear all isDefault flags for a customer before setting a new default. */
    @Modifying
    @Query("UPDATE CommerceCustomerAddress a SET a.isDefault = false WHERE a.customer.id = :customerId")
    void clearDefaultsForCustomer(Long customerId);
}
