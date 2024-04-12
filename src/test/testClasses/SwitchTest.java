package testClasses;

public class SwitchTest {

    public void SimpleSwitch(int x) {
        int i = x;
        switch(i) {
            case 1:
                i = 1;
                break;
            case 2:
                i = 2;
            default:
                i = 0;
        }
        i = 10;
    }
}