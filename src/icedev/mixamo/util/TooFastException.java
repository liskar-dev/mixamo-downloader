package icedev.mixamo.util;

@SuppressWarnings("serial")
public class TooFastException extends RuntimeException {
	public TooFastException() {
		super("429: Too Many Requests");
	}
}
