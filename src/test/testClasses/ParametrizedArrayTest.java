package testClasses;

public class ParametrizedArrayTest {
    public void parametrizedArrayMethod(int[] arr) {
        if (arr.length > 2) {
            System.out.println("Greater than two");
        } else {
            System.out.println("Not greater than two");
        }
        System.out.println("afterwards");
    }
}
