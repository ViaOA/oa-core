package test.hifive;

@FunctionalInterface
public interface FunctionalTest {

    
    public int test(int x);

    public default int test2() {
            return 2;
    }
    
}
