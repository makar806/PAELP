import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IOException | LanguageException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        int offset = args.length > 0 && (args[0].equals("eval") || args[0].equals("sm")) ? 1 : 0;
        boolean machine = offset == 0 || args[0].equals("sm");
        if (args.length < offset + 1 || args.length > offset + 2) {
            throw new LanguageException("Usage: java Main [eval|sm] <program.src> [input.txt]");
        }
        String source = Files.readString(Path.of(args[offset]));
        ReferenceResult result;
        if (args.length == offset + 2) {
            String input = Files.readString(Path.of(args[offset + 1]));
            result = machine ? ReferenceEngine.compileAndRun(source, input) : ReferenceEngine.evaluate(source, input);
        } else {
            result = machine ? ReferenceEngine.compileAndRun(source, List.of())
                    : ReferenceEngine.evaluate(source, List.of());
        }
        System.out.println("JSON representation:");
        System.out.println(result.json());
        System.out.println("Output:");
        System.out.println(result.output());
        if (!result.message().isEmpty()) {
            throw new LanguageException(result.message());
        }
    }
}
