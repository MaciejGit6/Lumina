package dev.lumina.error;

// Not really an error — just a control-flow trick.
// Thrown when a return statement fires, caught in LuminaFunction.call().
public class Return extends RuntimeException {
    public final Object value;

    public Return(Object value) {
        // Skip filling in the stack trace — we're using this for control
        // flow, not debugging, and the overhead isn't worth it.
        super(null, null, true, false);
        this.value = value;
    }
}