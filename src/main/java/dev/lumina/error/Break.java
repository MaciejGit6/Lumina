package dev.lumina.error;

// Thrown by the interpreter to unwind the stack when a break is hit.
// Same mechanism as Return — caught by visitWhileStmt.
public class Break extends RuntimeException {
    public Break() {
        super(null, null, true, false); // disable stack trace for performance
    }
}