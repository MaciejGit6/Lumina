package dev.lumina.error;

// Thrown by the interpreter to unwind the stack when a continue is hit.
// Caught by visitWhileStmt so execution jumps back to the condition check.
public class Continue extends RuntimeException {
    public Continue() {
        super(null, null, true, false); // disable stack trace for performance
    }
}