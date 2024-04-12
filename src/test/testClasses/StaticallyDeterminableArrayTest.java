package testClasses;

public class StaticallyDeterminableArrayTest {
    public void arrayStatementCompact() {
        int[] arr = {1,2,3,4,5};

        if (arr.length > 2) {
            System.out.println("Greater than two");
        } else {
            System.out.println("Not greater than two");
        }
    }

    public void arrayStatementSeparated() {
        int[] arr = {1,2,3,4,5};
        int arrLen = arr.length;

        if (arrLen > 2) {
            System.out.println("Greater than two");
        } else {
            System.out.println("Not greater than two");
        }
    }
}
