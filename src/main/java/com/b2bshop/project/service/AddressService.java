package com.b2bshop.project.service;

import com.b2bshop.project.exception.ResourceNotFoundException;
import com.b2bshop.project.model.Address;
import com.b2bshop.project.model.Country;
import com.b2bshop.project.model.Customer;
import com.b2bshop.project.port.GenericPort;
import com.b2bshop.project.repository.AddressRepository;
import com.b2bshop.project.repository.CountryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AddressService {
    private final CountryRepository countryRepository;
    private final SecurityService securityService;
    private final CustomerService customerService;
    private final AddressRepository addressRepository;
    private final GenericPort genericPort;

    public AddressService(CountryRepository countryRepository, SecurityService securityService,
                          CustomerService customerService, AddressRepository addressRepository, GenericPort genericPort) {
        this.countryRepository = countryRepository;
        this.securityService = securityService;
        this.customerService = customerService;
        this.addressRepository = addressRepository;
        this.genericPort = genericPort;
    }

    public List<Map<String, Object>> getAllAddresses(HttpServletRequest request) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        Long tenantId = securityService.returnTenantIdByUsernameOrToken("token", token);

        Optional<List<Address>> optionalAddresses = genericPort.findByTenantId(tenantId);

        List<Map<String, Object>> resultList = new ArrayList<>();

        if (optionalAddresses.isPresent()) {
            List<Address> addresses = optionalAddresses.get();

            for (Address address : addresses) {
                Map<String, Object> addressMap = new HashMap<>();

                addressMap.put("id", address.getId());
                addressMap.put("countryName", address.getCountry() != null ? address.getCountry().getName() : null);
                addressMap.put("title", address.getTitle());
                addressMap.put("city", address.getCity());
                addressMap.put("addressLine", address.getAddressLine());

                resultList.add(addressMap);
            }
        } else {
            return null;
        }
        return resultList;
    }

    @Transactional
    public Address createAddress(HttpServletRequest request, JsonNode json) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        Long tenantId = securityService.returnTenantIdByUsernameOrToken("token", token);
        Long countryId = json.get("countryId").asLong();

        Address address = new Address();

        Customer customer;
        customer = customerService.findCustomerById(tenantId);
        address.setCustomer(customer);

        Country country;
        country = countryRepository.findById(countryId).orElseThrow(
                () -> new ResourceNotFoundException("Country could not find by id: " + countryId));
        address.setCountry(country);

        address.setTitle(json.get("title").asText());
        address.setCity(json.get("city").asText());
        address.setAddressLine(json.get("addressLine").asText());

        return addressRepository.save(address);
    }

    public Address findAddressById(Long id) {
        return addressRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Address could not find by id: " + id));
    }
}
