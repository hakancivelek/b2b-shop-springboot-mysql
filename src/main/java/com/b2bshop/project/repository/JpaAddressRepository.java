package com.b2bshop.project.repository;

import com.b2bshop.project.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAddressRepository extends JpaRepository<Address, Long>, AddressRepository {

}