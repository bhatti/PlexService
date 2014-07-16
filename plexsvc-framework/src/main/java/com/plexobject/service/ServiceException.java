package com.plexobject.service;

import java.util.HashSet;
import java.util.Set;

public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public static class Error {
        public final String errorCode;
        public final String fieldName;
        public final String errorMessage;

        public Error(String errorCode, String fieldName, String errorMessage) {
            this.errorCode = errorCode;
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
        }
    }

    public static class Builder {
        private final Set<Error> errors = new HashSet<>();
        private String message;
        private Throwable cause;

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder addError(String errorCode, String fieldName,
                String errorMessage) {
            errors.add(new Error(errorCode, fieldName, errorMessage));
            return this;
        }

        public Builder addErrorIfNull(Object obj, String errorCode,
                String fieldName, String errorMessage) {
            if (obj == null) {
                errors.add(new Error(errorCode, fieldName, errorMessage));
            }
            return this;
        }

        public Builder addErrorIfEmpty(String buffer, String errorCode,
                String fieldName, String errorMessage) {
            if (buffer == null || buffer.length() == 0) {
                errors.add(new Error(errorCode, fieldName, errorMessage));
            }
            return this;
        }

        public Builder addErrorIfFalse(boolean predicate, String errorCode,
                String fieldName, String errorMessage) {
            if (!predicate) {
                errors.add(new Error(errorCode, fieldName, errorMessage));
            }
            return this;
        }

        public void raiseIfHasErrors() {
            if (errors.size() > 0) {
                throw new ServiceException(message, cause, errors);
            }
        }

        public void raise() {
            throw new ServiceException(message, cause, errors);
        }
    }

    private final Set<Error> errors;

    public ServiceException(String message, Throwable cause,
            final Set<Error> errors) {
        super(message, cause);
        this.errors = errors;
    }

    public ServiceException(String message, final Set<Error> errors) {
        super(message);
        this.errors = errors;
    }

    public Set<Error> getErrors() {
        return errors;
    }

    public static Builder builder() {
        return new Builder();
    }
}
