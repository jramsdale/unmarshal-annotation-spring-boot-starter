package systems.misnomer.spring.unmarshal;

/**
 * marker exception for {@link RuntimeException}s that occur while handling the {@link Unmarshal}
 * annotation.
 */
public class UnmarshalException extends RuntimeException {

    private static final long serialVersionUID = 5209803086287139373L;

    public UnmarshalException(String message) {
        this(message, null);
    }

    public UnmarshalException(String message, Throwable cause) {
        super(message, cause);
    }

}
