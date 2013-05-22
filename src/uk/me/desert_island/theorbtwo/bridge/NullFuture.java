package uk.me.desert_island.theorbtwo.bridge;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

class NullFuture extends FutureTask<Object> {
    public NullFuture() {
        super(new Callable() {
                public Object call() {
                    return null;
                }
            });
    }

    public void setAndDone(Object result) {
        this.set(result);
        this.done();
    }
    
    
}