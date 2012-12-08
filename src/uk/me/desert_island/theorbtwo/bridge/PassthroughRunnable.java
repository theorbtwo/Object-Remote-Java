package uk.me.desert_island.theorbtwo.bridge;

public class PassthroughRunnable implements Runnable {
    private String code_id;
    private Core core;

    public PassthroughRunnable(String a_code_id, Core a_core) {
        code_id = a_code_id;
        core = a_core;
    }

    public void run_extended(Object... args) {
        // FIXME: figure out how to make this return something useful?
        core.run_remote_code(code_id, args);
    }

    public void run() {
        core.run_remote_code(code_id);
    }
}