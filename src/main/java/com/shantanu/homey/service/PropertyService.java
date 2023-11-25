package com.shantanu.homey.service;

import com.shantanu.homey.entity.Property;
import com.shantanu.homey.model.ResponseModel;
import com.shantanu.homey.repository.PropertyRepository;
import com.shantanu.homey.repository.UserRepository;
import com.shantanu.homey.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static com.shantanu.homey.config.Constants.STATUS_ACTIVE;
import static java.util.Objects.nonNull;


@Service
public class PropertyService {

    @Autowired
    PropertyRepository propertyRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private JavaMailSender emailSender;

    public List<Property> fetchProducts() {
        return propertyRepository.findByStatus(STATUS_ACTIVE);
    }

    public List<Property> fetchMyProducts(String userId) {
        return propertyRepository.findByStatusAndPostedBy(STATUS_ACTIVE, userId);
    }

    @Value("${s3ImageUrl}")
    private String s3ImageUrl;

    public ResponseModel saveProduct(Property property, String userName) {
        ResponseModel response = new ResponseModel();
        try {
            property.setId(UUID.randomUUID());
            property.setStatus("A");
            property.setPostedOn(LocalDateTime.now());
            property.setPostedBy(userName);
            propertyRepository.save(property);
            response.setStatus("Success");
            response.setMessage("Operation Successful");
            response.setCreationId(property.getId().toString());
        } catch (Exception e) {
            response.setException(e.getLocalizedMessage());
        }
        return response;
    }

    public ResponseModel updateProduct(UUID propertyId, Property property, String userName) {
        ResponseModel response = new ResponseModel();
        try {
            propertyRepository.findById(propertyId).ifPresent(
                    savedProperty -> {
                        if (savedProperty.getPostedBy().equals(userName)) {
                            setUpdatedValues(savedProperty, property);
                            propertyRepository.save(savedProperty);
                            response.setStatus("Success");
                            response.setMessage("Operation Successful");
                        } else {
                            response.setStatus("Unsuccessful");
                            response.setMessage("Operation Unsuccessful : Not Authorized To Update This Property");
                        }
                    }
            );
        } catch (Exception e) {
            response.setStatus("Unsuccessful");
            response.setMessage("Exception Occurred");
            response.setException(e.getLocalizedMessage());
        }
        return response;
    }

    private void setUpdatedValues(Property property, Property updatedProp) {
        if (nonNull(updatedProp.getAddress()))
            property.setAddress(updatedProp.getAddress());

        if (nonNull(updatedProp.getArea()))
            property.setArea(updatedProp.getArea());

        if (nonNull(updatedProp.getAmenities()))
            property.setAmenities(updatedProp.getAmenities());

        if (nonNull(updatedProp.getConstructorName()))
            property.setConstructorName(updatedProp.getConstructorName());

        if (nonNull(updatedProp.getBookingAmount()))
            property.setBookingAmount(updatedProp.getBookingAmount());

        if (nonNull(updatedProp.getPrice()))
            property.setPrice(updatedProp.getPrice());

        if (nonNull(updatedProp.getPostalCode()))
            property.setPostalCode(updatedProp.getPostalCode());

        if (nonNull(updatedProp.getEircode()))
            property.setEircode(updatedProp.getEircode());

        if (nonNull(updatedProp.getDescription()))
            property.setDescription(updatedProp.getDescription());

        if (nonNull(updatedProp.getAvailableFrom()))
            property.setAvailableFrom(updatedProp.getAvailableFrom());

        if (nonNull(updatedProp.getEnergyRatings()))
            property.setEnergyRatings(updatedProp.getEnergyRatings());

        if (nonNull(updatedProp.getBedrooms()))
            property.setBedrooms(updatedProp.getBedrooms());

        if (nonNull(updatedProp.getBathrooms()))
            property.setBathrooms(updatedProp.getBathrooms());

        property.setModifiedOn(LocalDateTime.now());
    }

    public ResponseModel inactiveProperty(UUID propertyId, String userName) {
        ResponseModel response = new ResponseModel();
        try {
            propertyRepository.findById(propertyId).ifPresent(
                    savedProperty -> {
                        if (savedProperty.getPostedBy().equals(userName)) {
                            savedProperty.setStatus("I");
                            propertyRepository.save(savedProperty);
                            response.setStatus("Success");
                            response.setMessage("Operation Successful");
                        } else {
                            response.setStatus("Unsuccessful");
                            response.setMessage("Operation Unsuccessful : Not Authorized To Update This Property");
                        }
                    }
            );
        } catch (Exception e) {
            response.setStatus("Unsuccessful");
            response.setMessage("Exception Occurred");
            response.setException(e.getLocalizedMessage());
        }
        return response;
    }

    @Transactional
    public void updateImageUrl(String propertyId, String fileKey) {
        propertyRepository.findById(UUID.fromString(propertyId)).ifPresent(
                product -> {
                    if (nonNull(product.getImages())) {
                        product.getImages().add(s3ImageUrl + fileKey);
                    } else {
                        product.setImages(List.of(s3ImageUrl + fileKey));
                    }
                    propertyRepository.save(product);
                }
        );
    }

    public ResponseModel emailPropertyOwner(UUID productId, String userName) {
        ResponseModel response = new ResponseModel();
        Property prod = propertyRepository.findById(productId).orElse(null);
        User productUser = null;
        if (nonNull(prod)) {
            productUser = userRepository.findByUsername(prod.getPostedBy()).orElse(null);
        }
        User enquiryUser = userRepository.findByUsername(userName).orElse(null);
        if (nonNull(productUser) && nonNull(enquiryUser)) {
            sendEmail(productUser, enquiryUser, prod);
        }
        response.setStatus("Success");
        response.setMessage("Operation Successful");
        return response;
    }

    private void sendEmail(User productUser, User enquiryUser, Property property) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("honeyspace.app@gmail.com");
        message.setTo(productUser.getEmail());
        message.setSubject("Enquiry for your cataloged property : " + property.getConstructorName());
        message.setText("A person has made an enquiry for your property : "
                + property.getProjectName() +
                ", kindly Email the potential buyer of your property. Here Is The Details Of Enquirer. " +
                " Name: " + enquiryUser.getFirstname() + " " + enquiryUser.getLastname() + " " +
                " Email ID: " + enquiryUser.getEmail());
        this.emailSender.send(message);
    }
}
