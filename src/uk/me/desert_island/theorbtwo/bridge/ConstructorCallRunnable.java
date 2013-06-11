package uk.me.desert_island.theorbtwo.bridge;

import java.lang.Runnable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class ConstructorCallRunnable implements Runnable {
    private Class klass;
    private Object[] args;
    private Constructor constr;

    public volatile Object constructed = null;
    public volatile Exception exception = null;

    public Semaphore finished = new Semaphore(0);
    
    public ConstructorCallRunnable(String perl_class, Object... a_args) 
        throws NoSuchMethodException, ClassNotFoundException
    {
        args = a_args;

        klass = Core.my_find_class(perl_class);
        Class[] arg_types = new Class[a_args.length];
        for (int i = 0; i < a_args.length; i++) {
            System.err.println("Looking at arg: " + a_args[i].toString());
            arg_types[i] = a_args[i].getClass();
        }

        constr = Core.my_find_constructor(klass, arg_types);
    }
    
    public void run() 
    {
        try {
            constructed = constr.newInstance(args);
        } catch (Exception e) {
            exception = e;
        }
        finished.release();
    }

    public Object getResult()
        throws Exception
    {
        // We need to wait here until run() has actually finished!
        // finished.acquire();
        Boolean got_finished = finished.tryAcquire(1, 1, TimeUnit.SECONDS);
        if (got_finished) {
            System.err.println("Already finished the ConstructorCallRunnable");
        } else {
            System.err.println("ConstructorCallRunnable not yet finished, but continuing anyway");
        }
        if (exception != null) {
            throw exception;
        }

        return constructed;
    }
}
