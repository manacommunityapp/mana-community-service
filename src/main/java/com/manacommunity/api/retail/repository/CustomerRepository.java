package com.manacommunity.api.retail.repository;

import com.manacommunity.api.retail.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCommunityIdOrderByNameAsc(Long communityId);
}
