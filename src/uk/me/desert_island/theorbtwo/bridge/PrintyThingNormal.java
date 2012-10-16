package uk.me.desert_island.theorbtwo.bridge;

import java.io.PrintStream;

public class PrintyThingNormal extends PrintyThing {
    private PrintStream out_stream;
    public PrintyThingNormal(PrintStream out_str) {
        out_stream = out_str;

    }

    void print(String str) {
        out_stream.print(str);
    }
}

