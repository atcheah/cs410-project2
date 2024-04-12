package testClasses;

public class ParametrizedStringTest {
    public void parametrizedStringMethod(String s) {
        if (s.length() > 2) {
            System.out.println("Greater than two");
        } else {
            System.out.println("Not greater than two");
        }
        System.out.println("afterwards");
    }
}
