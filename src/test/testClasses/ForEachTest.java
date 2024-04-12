package testClasses;

public class ForEachTest {
    public void forEachMethod() {
        int[] arr = {1,2,3};
        int sum = 0;
        for(int a : arr) {
            sum = sum + a;
            System.out.println(a);
        }
        System.out.println("after loop");

        if (sum == 0) {
            System.out.println("Greater than zero");
        } else {
            System.out.println("Unknown");
        }
    }
}
