import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Передай путь к JSON-файлу с AST.");
            System.out.println("Пример: java Main program.json");
            return;
        }

        String astJson = Files.readString(Path.of(args[0]));
        System.out.println("JSON получен. Символов: " + astJson.length());

    }
}
