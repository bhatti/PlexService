package com.plexobject.domain;

/**
 * This interface defines method to validate object
 * 
 * @author shahzad bhatti
 *
 */
public interface Validatable {
    void validate() throws ValidationException;
}
