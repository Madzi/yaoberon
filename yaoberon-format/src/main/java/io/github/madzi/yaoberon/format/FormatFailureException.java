package io.github.madzi.yaoberon.format;

public class FormatFailureException extends RuntimeException {

    public FormatFailureException(final String message) {
        super(message);
    }

    public FormatFailureException(final Throwable cause) {
        super(cause);
    }

    public FormatFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
