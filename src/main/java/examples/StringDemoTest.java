package examples;

public class StringDemoTest {

    public void kittyTest() {
        int kitty = 0;
        if (kitty == 0) {
            petKitty();
        } else {
            System.out.println("Be sad");
            petKitty();
        }

    }
    public void petKitty() {
        System.out.println("Purr");

    }

}