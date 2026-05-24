package com.vinit.gymPartner.entity.converter;

import com.vinit.gymPartner.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts Gender enum to/from the database string column.
 * Handles blank or unknown values gracefully by mapping them to null
 * instead of crashing with IllegalArgumentException.
 */
@Converter(autoApply = false)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return gender != null ? gender.name() : null;
    }

    @Override
    public Gender convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(dbValue.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
