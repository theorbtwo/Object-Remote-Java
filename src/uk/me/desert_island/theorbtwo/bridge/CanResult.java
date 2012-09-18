package uk.me.desert_island.theorbtwo.bridge;

public class CanResult {
    private Class klass;
    private String method_name;

    public CanResult(Class k, String n) {
        klass = k;
        method_name = n;
    }

    public Class klass() { return klass; }
    public String method_name() { return method_name; }
}