package com.manacommunity.api.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static FieldEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(FieldEncryptionService svc) {
        EncryptedStringConverter.encryptionService = svc;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionService == null || attribute == null) return attribute;
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionService == null || dbData == null) return dbData;
        return encryptionService.decrypt(dbData);
    }
}
