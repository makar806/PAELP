import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IOException | LanguageException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java Main <program.src> [input.txt]");
            return;
        }

        String source = Files.readString(Path.of(args[0]));
        List<Long> input = args.length == 2
                ? InputValues.parse(Files.readString(Path.of(args[1])))
                : List.of();

        EngineResult result = LanguageEngine.compileAndRun(source, input);

        System.out.println("JSON representation:");
        System.out.println(result.json());
        System.out.println("Output:");
        System.out.println(result.output().stream()
                .map(String::valueOf)
                .collect(Collectors.joining("; ")));
    }
}
