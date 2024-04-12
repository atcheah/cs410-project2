package testClasses;

public class NestedParametrizedIfTest {
    public void nestedIfStatement(int i) {
        if (i > 5) {
            System.out.println("greater than 5");

            if (i > 10) {
                System.out.println("also greater than 10");
            }

            Math.max(5, 5);
        } else {
            System.out.println("less than or equal to 5");
        }
        System.out.println("after both prints");
    }
}