package testClasses;

public class NestedStaticallyDeterminableIfTest {
    public void nestedIfStatements(String s) {
        if (s.length() > 5) {
            System.out.println("String is greater than 5");

            if (3 > 5) {
                System.out.println("This shouldn't happen");

                if (s.length() > 5) {
                    System.out.println("This statement doesn't matter my parent is dead!");
                }
            }
        }
        System.out.println("Outside");
    }
}