package uk.me.desert_island.theorbtwo.bridge;

public class TestClass {
    public static final int ANSWER = 42;

    public String public_field = "Default Value";

    static public int staticReturnsInt() {
        return 4;
    }

    static public void throwsException() throws Exception {
        throw(new RuntimeException("test exception"));
    }

    public TestClass() {
        // Nullary constructor
    }

    public TestClass(String arg) {
        public_field = arg;
    }

    public String object_method(String arg) {
        return "object_method return";
    }

    public String object_multi_method(String arg) {
        return "object_multi_method got string";
    }

    public String object_multi_method(int arg) {
        return "object_multi_method got int";
    }
}