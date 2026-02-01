package com.android.vcard.exception;

public class VCardException extends Exception {
    public VCardException() { super(); }
    public VCardException(String message) { super(message); }
    public VCardException(String message, Throwable cause) { super(message, cause); }
}
