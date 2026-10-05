public record Instruction(String opcode, Object argument) {
    public Instruction {
        if (opcode == null || opcode.isBlank()) {
            throw new IllegalArgumentException("opcode must not be blank");
        }
        if (argument != null && !(argument instanceof String) && !(argument instanceof Long)) {
            throw new IllegalArgumentException("instruction argument must be a string or a long");
        }
    }

    public static Instruction simple(String opcode) {
        return new Instruction(opcode, null);
    }

    public static Instruction withString(String opcode, String argument) {
        return new Instruction(opcode, argument);
    }

    public static Instruction withNumber(String opcode, long argument) {
        return new Instruction(opcode, argument);
    }
}
