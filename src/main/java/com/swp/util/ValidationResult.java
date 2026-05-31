package com.swp.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationResult {

    private final Map<String, String> fieldErrors = new LinkedHashMap<>();
    private String generalError;

    public void addFieldError(String field, String message) {
        fieldErrors.put(field, message);
    }

    public void setGeneralError(String generalError) {
        this.generalError = generalError;
    }

    public boolean isValid() {
        return fieldErrors.isEmpty() && (generalError == null || generalError.isBlank());
    }

    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    public String getGeneralError() {
        if (generalError != null && !generalError.isBlank()) {
            return generalError;
        }
        if (!fieldErrors.isEmpty()) {
            return "Vui lòng kiểm tra lại các trường được đánh dấu.";
        }
        return null;
    }
}
