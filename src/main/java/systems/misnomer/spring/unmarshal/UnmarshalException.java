package systems.misnomer.spring.unmarshal;

/**
 * marker exception for {@link RuntimeException}s that occur while handling the {@link Unmarshal}
 * annotation.
 */
public class UnmarshalException extends RuntimeException {

    private static final long serialVersionUID = 5209803086287139373L;

    /**
     * Constructs an exception with no underlying cause.
     *
     * @param message detail describing the problem
     */
    public UnmarshalException(String message) {
        this(message, null);
    }

    /**
     * Constructs an exception wrapping an underlying cause.
     *
     * @param message detail describing the problem
     * @param cause underlying exception, may be {@code null}
     */
    public UnmarshalException(String message, Throwable cause) {
        super(message, cause);
    }

}
