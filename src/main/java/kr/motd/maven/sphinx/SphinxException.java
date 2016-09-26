package kr.motd.maven.sphinx;

public class SphinxException extends RuntimeException {
    private static final long serialVersionUID = 4257071990203862423L;

    public SphinxException(String message) {
        super(message);
    }

    public SphinxException(String message, Throwable cause) {
        super(message, cause);
    }
}
