package testClasses;

public class ContinueWrappedTest {
    public void SimpleContinueUseCase() {
        int i = 0;
        while (i < 8) {
            if (true) {
                if (i == -1) {
                    System.out.println("DEAD1");
                } else {
                    continue;
                }
            }
            System.out.println("DEAD2");
        }
        System.out.println("ALIVE");
    }
}
