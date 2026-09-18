import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Solution {
    public static void main(String[] args) {
    //Task 1
        try {
            List<String> lines = Files.readAllLines(new File("numbers.txt").toPath());

            System.out.println("Read from the line");
            for (String line : lines) {
                System.out.println(line);
            }

            List<Integer> numbers = parseNumber(lines);
            System.out.println("Parsed " + numbers.size() + " of " + lines.size() + " lines: " + numbers);
        }
        catch (IOException e) {
            System.out.println("Error, could not read the file: " + e.getMessage());
        }


    }
    public static List<Integer> parseNumber(List<String> lines) {
        List<Integer> numbers = new ArrayList<>();
        if (lines == null) return numbers;

        for (String line : lines) {
            try {
                numbers.add(Integer.parseInt(line.trim()));
            } catch (NumberFormatException e) {
                //skip anything that is not a whole number
            }
        }

        return numbers;
    }
}
