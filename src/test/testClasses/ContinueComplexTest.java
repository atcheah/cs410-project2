package testClasses;

public class ContinueComplexTest {
    public void NestedLoops() {
        while (true) {
            for (int i = 0; i < -9; i++) {
                if (true) {
                    continue;
                }
                System.out.println("DEAD1");
            }
            System.out.println("ALIVE1");

            if (false) {
            } else {
                continue;
            }
            System.out.println("DEAD2");
        }
    }
}
