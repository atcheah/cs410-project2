package testClasses;

public class MethodCallWithArguments {

    public void foo() {
        int x = 0;
        boo(8, "Scary", false);
    }

    public void boo(int x, String str, boolean isScary) {
        if (x > 1) {
            System.out.println("X IS GREATER THAN 1!");
        }

        if (str.equals("Scary")) {
            System.out.println("BOOOOO!");
            isScary = true;
        } else {
            isScary = false;
        }

        if (!isScary) {
            System.out.println("DONT RUN AWAY");
        }
    }
}
