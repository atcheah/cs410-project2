package testClasses;

import examples.Temporary;

public class MethodInAnotherFileTest {
    public void someMethod() {
        Temporary t = new Temporary("some arg");
        t.printName();
    }
}
