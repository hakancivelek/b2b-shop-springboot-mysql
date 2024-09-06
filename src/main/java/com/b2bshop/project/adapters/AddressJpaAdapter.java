package com.b2bshop.project.adapters;

import com.b2bshop.project.model.Address;
import com.b2bshop.project.port.GenericPort;
import com.b2bshop.project.repository.AddressRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AddressJpaAdapter implements GenericPort<Address, Long> {

    private final AddressRepository addressRepository;
    private final EntityManager entityManagery;

    public AddressJpaAdapter(AddressRepository addressRepository, EntityManager entityManagery) {
        this.addressRepository = addressRepository;
        this.entityManagery = entityManagery;
    }

    @Override
    public Address save(Address entity) {
        return addressRepository.save(entity);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressRepository.findById(id);
    }

    @Override
    public Optional<List<Address>> findByTenantId(Long tenantId) {
        Session session = entityManagery.unwrap(Session.class);

        String hqlQuery = "SELECT address FROM Address as address " +
                " WHERE customer.id = :tenantId";

        Query query = session.createQuery(hqlQuery);
        query.setParameter("tenantId", tenantId);

        List<Address> addresses = query.list();
        return Optional.ofNullable(addresses.isEmpty() ? null : addresses);
    }


    @Override
    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        addressRepository.deleteById(id);
    }
}
