package com.manacommunity.api.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA attribute converter that transparently encrypts sensitive string columns.
 *
 * <p>Hibernate instantiates converters itself, so the {@link FieldEncryptionService} is
 * injected into a static field by Spring. If a conversion happens before that injection
 * has run, the converter has no way to encrypt — and must therefore fail loudly rather
 * than write medical data to Postgres in plaintext into a column that is documented
 * (V499) as AES-256-GCM encrypted.
 */
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
        if (attribute == null) return null;
        return require().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return require().decrypt(dbData);
    }

    private static FieldEncryptionService require() {
        FieldEncryptionService svc = encryptionService;
        if (svc == null) {
            throw new IllegalStateException(
                    "FieldEncryptionService has not been injected into EncryptedStringConverter yet. "
                            + "Refusing to read or write an encrypted column unencrypted — a silent "
                            + "plaintext write of health data is worse than a failed request.");
        }
        return svc;
    }
}
