package icedev.mixamo.util;
import java.io.IOException;

@SuppressWarnings("serial")
public class RuntimeIOException extends RuntimeException {
	public RuntimeIOException(IOException cause) {
		super(cause);
	}
}
