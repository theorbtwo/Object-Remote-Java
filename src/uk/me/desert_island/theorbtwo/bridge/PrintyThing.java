package uk.me.desert_island.theorbtwo.bridge;

public abstract class PrintyThing {
    protected String LOGTAG = "JavaBridgeService::PrintyThing";
    public PrintyThing() {
    }

    abstract void print(String str);
}

