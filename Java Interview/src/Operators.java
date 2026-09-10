import java.util.Arrays;

public class  Operators {
    public static void main(String args[]) {

            // Int array
            int intArray[] = {100, 10, 50, -8, 64, 1, 6, 9, 101, 300};
            for (int i=0;i<intArray.length - 1; i++) {
                String output = intArray[i] <= intArray[i+1] ? (intArray[i] < intArray[i+1]? " less than " : " equal to ") : " more than ";
                System.out.println(intArray[i]+output+intArray[i+1]);

            // Double array
            double doubleArray[] = {300.3, 511.1, 10.1, 33.3, 21.1, 44.1, 90.0};
            for (int j=0;j<doubleArray.length - 1; j++) {
                String output2 = doubleArray[j] <= doubleArray[j+1] ? (doubleArray[j] < doubleArray[j+1] ? " less than " : " equal to ") : " more than ";
                System.out.println(doubleArray[j]+output2+doubleArray[j+1]);
            }
        }
            // Print for loop
            int array1[] = new int[10];
            for (int i=0;i<array1.length;i++)
                array1[i] = i;
            System.out.println(Arrays.toString(array1));

            // Enhanced for loop
            char numbers[] = {'H', 'E', 'E', 'e'};
            for (char letter : numbers) {
            //System.out.println(numbers.length);
            System.out.println(letter);
            }

            // Array list
            char list[] = {'A', 'B', 'C'};
            System.out.println(Arrays.toString(list));

            // Multi-dimension array
           int multiArray[][] = {{1,2,3}, {4,5,6}, {7,8,9}};

        // Nested Enhanced for loop
            for (int[] row : multiArray) {        // Loops through each row array
                for (int num : row) {             // Loops through each number in that row
                    System.out.print(num + " ");
            }
            System.out.println();             // Moves to the next line after finishing a row
        }
            System.out.println(multiArray[1][2]);

        // 2-D Array
           //System.out.println((Arrays.deepToString(multiArray)));
	    

           }
    }
