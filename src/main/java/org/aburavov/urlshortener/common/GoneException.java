package org.aburavov.urlshortener.common;

public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
