import java.util.Scanner;
import java.util.file.Files;
import java.io.File;
import java.util.List;
import java.io.*;

public class Solution {
    public static void main(String[] args) {
    //Task 1
        try {
            List<String> lines = Files.readAllLines(new File("numbers.txt").toPath());

            System.out.println("Read from the line");
            for (String line : lines) ;
            System.out.println(lines);
        }

            List<Integer numbers = parseNumber(lines);

        catch (IOException ){
            System.out.println("Error, could not read the file");
        }


    }
    public static List<Integer> parseNumber(List<String> lines) {
        List<Integer> numbers = new ArraryList<>[];
        if (lines == null) return numbers;

        for (String line : lines) {
            try {
                numbers.
            }
        }
    }
}

