package examples;

public class Blah {
    public void a_method(Integer i) {
        if (i > 5) {
            b_method(99);
        } else {
            b_method(Math.random());
        }
    }

    public void b_method(Number x) {
        System.out.println("The value of x is: " + x.toString());
        Temporary t = new Temporary("some arg");
        t.printName();
    }

    public void c_method() {
        int sum = 0;
        Integer[] numbers = {1, 2, 3, 4}; // Creating an array of integers
        sum = -5;
        for (Integer i : numbers) {
            sum += i;
        }

        for (int i = 0 ; i < numbers.length; i++) {
            sum += numbers[i];
            System.out.println("cool!!");
        }

        System.out.println("After for loop");

        a_method(sum);
    }
}