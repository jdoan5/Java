public class DataTypes {
    public static void main(String[] args) {
        //byte ranges from -128 to 127
        //use for handling file data in binary format
        byte b = 125;
        System.out.println("Byte value " + b);

        //short ranges from -32,768	to 32,767
        //Suitable for memory-constrained environments where integers are too large
        // Used for Game scores, sensor readings etc.,

        short points = 3000;
        System.out.println("Points scored as 'short' value " + points);

        //int  ranges from -2^31	to (2^31)-1
        //Suitable for all general purpose use of numbers
        // Suits values that are whole numbers

        int population = 4358000;
        System.out.println("Population as 'int' value " + population);

        //long ranges from -2^63	to (2^63)-1
        //Used for large numeric values, timestamps in milliseconds
        // It has to end with an l indcating it is a long integer
        long age_in_milliseconds = 788923800000l;
        System.out.println("Age of 25 years in milliseconds as 'long' value " + age_in_milliseconds);
        //char is used to store on character
        // Suitable for capturing keyboad input one key at a time
        // Used mostly as a group called arrays for words
        char keyboard_input = 'a';
        System.out.println("Key board input is "+keyboard_input);

        //boolean is used to true or false
        // Suitable for anything where the values can only be true or false
        boolean isLabCompleted = false;
        System.out.println("The lab is completed "+isLabCompleted);

        //float is used for numbers with decimals, with precision up to 7 decimal digits
        // Suitable for representing money, measurement of area, volume, etc.,
        // It has to end with an f
        float house_value = 4000000.58f;
        System.out.println("The value of the house is $"+house_value);

        //double  is used for numbers with decimal,  with precision up to 15 decimal digits
        // Suitable for representing statistical, astronimcal measurements
        double pi = 3.14159265359;
        System.out.println("The value of pi is "+pi);

        //Java convert
        char p2 = '6';
        System.out.println(p2);

        //Boolean
        char a = 'a';
        char c = 'a';
        System.out.println(a==c);

        //Operation
       int aa = 10;
       int d = 19;
       System.out.println(aa==d);
       System.out.println(a!=d);
       System.out.println(aa<=d);

        //Ternary Operators
        String hasArgs = args.length == 0 ? "No arguments passed" : args[0];
        System.out.println(hasArgs);
    }
}
