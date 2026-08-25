package io.vessel.http;

import java.util.Arrays;

/**
 * Represents HTTP status codes as defined by the HTTP standard.
 * Each status code is associated with an integer code and a reason phrase.
 * This enum classifies status codes into families: informational, success,
 * redirection, client error, and server error.
 */
public enum HttpStatus {

    // 1xx Informational
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    PROCESSING(102, "Processing"),
    EARLY_HINTS(103, "Early Hints"),

    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),
    MULTI_STATUS(207, "Multi-Status"),
    ALREADY_REPORTED(208, "Already Reported"),
    IM_USED(226, "IM Used"),

    // 3xx Redirection
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    USE_PROXY(305, "Use Proxy"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // 4xx Client Error
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    URI_TOO_LONG(414, "URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    IM_A_TEAPOT(418, "I'm a teapot"),
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    LOCKED(423, "Locked"),
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    TOO_EARLY(425, "Too Early"),
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    // 5xx Server Error
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    LOOP_DETECTED(508, "Loop Detected"),
    NOT_EXTENDED(510, "Not Extended"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),

    // Fallback
    UNKNOWN_STATUS(0, "Unknown Status");

    private static final int INFORMATIONAL_MIN = 100;
    private static final int INFORMATIONAL_MAX = 199;
    private static final int SUCCESS_MIN = 200;
    private static final int SUCCESS_MAX = 299;
    private static final int REDIRECTION_MIN = 300;
    private static final int REDIRECTION_MAX = 399;
    private static final int CLIENT_ERROR_MIN = 400;
    private static final int CLIENT_ERROR_MAX = 499;
    private static final int SERVER_ERROR_MIN = 500;
    private static final int SERVER_ERROR_MAX = 599;

    private final int code;
    private final String reasonPhrase;

    HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    /** Returns the numeric HTTP status code. */
    public int code() {
        return code;
    }

    /** Returns the standard reason phrase (e.g. "Not Found" for 404). */
    public String reason() {
        return reasonPhrase;
    }

    /** Returns the {@link HttpStatusFamily} category for this status code. */
    public HttpStatusFamily family() {
        return switch (code / 100) {
            case 1 -> HttpStatusFamily.INFORMATIONAL;
            case 2 -> HttpStatusFamily.SUCCESS;
            case 3 -> HttpStatusFamily.REDIRECTION;
            case 4 -> HttpStatusFamily.CLIENT_ERROR;
            case 5 -> HttpStatusFamily.SERVER_ERROR;
            default -> throw new IllegalStateException("Unknown status family: " + code);
        };
    }

    public boolean isInformational() {
        return code >= INFORMATIONAL_MIN && code <= INFORMATIONAL_MAX;
    }

    public boolean isSuccess() {
        return code >= SUCCESS_MIN && code <= SUCCESS_MAX;
    }

    public boolean isRedirection() {
        return code >= REDIRECTION_MIN && code <= REDIRECTION_MAX;
    }

    public boolean isClientError() {
        return code >= CLIENT_ERROR_MIN && code <= CLIENT_ERROR_MAX;
    }

    public boolean isServerError() {
        return code >= SERVER_ERROR_MIN && code <= SERVER_ERROR_MAX;
    }

    /** Returns {@code true} if this is a 4xx or 5xx status. */
    public boolean isError() {
        return isClientError() || isServerError();
    }

    /**
     * Looks up the {@code HttpStatus} for the given numeric code.
     * Returns {@link #UNKNOWN_STATUS} if no match is found.
     */
    public static HttpStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN_STATUS);
    }
}
