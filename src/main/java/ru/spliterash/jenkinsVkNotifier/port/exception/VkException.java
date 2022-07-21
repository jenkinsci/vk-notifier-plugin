package ru.spliterash.jenkinsVkNotifier.port.exception;

public class VkException extends RuntimeException{
    public VkException() {
    }

    public VkException(String message) {
        super(message);
    }

    public VkException(String message, Throwable cause) {
        super(message, cause);
    }

    public VkException(Throwable cause) {
        super(cause);
    }

    public VkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
