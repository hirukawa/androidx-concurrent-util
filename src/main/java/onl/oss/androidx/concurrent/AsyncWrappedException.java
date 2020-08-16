package onl.oss.androidx.concurrent;

@SuppressWarnings("serial")
public class AsyncWrappedException extends RuntimeException {
    public AsyncWrappedException(Exception cause) {
        super(cause);
    }
}
