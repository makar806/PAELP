import java.util.ArrayList;
import java.util.List;

final class InputValues {
    private InputValues() {
    }

    static List<Long> parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        for (String item : trimmed.split("[,\\s]+")) {
            try {
                values.add(Long.parseLong(item));
            } catch (NumberFormatException exception) {
                throw new LanguageException("input value is not an integer: " + item);
            }
        }
        return List.copyOf(values);
    }
}
