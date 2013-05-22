package uk.me.desert_island.theorbtwo.bridge;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;

public class PassthroughRunnable implements Runnable, Callable<Object> {
    private String code_id;
    private Core core;

    public PassthroughRunnable(String a_code_id, Core a_core) {
        code_id = a_code_id;
        core = a_core;
    }

    public void run_extended(Object... args) {
        // FIXME: figure out how to make this return something useful?
        core.run_remote_code(false, code_id, args);
    }

    public void run() {
        run_extended();
    }

    public Object call_extended(Object... args) {
        Future future = core.run_remote_code(true, code_id, args);
        
        try {
            return future.get();
        } catch (Exception e) {
            core.err.print("Exception during call_extended: "+e);
            return null;
        }

    }
    
    public Object call() {
        return call_extended();
    }
}