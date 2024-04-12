package testClasses;

public class StaticallyDeterminableIfTest {
    public void basicIfStatement() {
        int a = 3;

        int b = a + 3;

        if(b > a) {
            System.out.println("b is greater than a");
            String c = "throwaway";
            int d = 5;
            int e = -1;
        } else {
            System.out.println("b is not greater than a");
            int d = 5;
            int e = 1;
        }

        System.out.println("afterwards");
    }
}