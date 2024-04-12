package testClasses;

public class StaticallyDeterminableStringTest {
    public void stringStatement() {
        String s = "wow";

        if (s.length() > 2) {
            System.out.println("Greater than two");
        } else {
            System.out.println("Not greater than two");
        }
    }
}
