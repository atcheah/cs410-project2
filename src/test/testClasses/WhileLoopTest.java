package testClasses;

public class WhileLoopTest {
    public void WhileLoopy(boolean wheely) {
        int oddNumber = 1;
        while (oddNumber % 2 != 0) {
            // oddNumber++; TODO: figure out bug that happens when this replaces line below.
            oddNumber += 1;
        }
        // :(
        int noLongerOdd = oddNumber;
    }
}
