package uk.me.desert_island.theorbtwo.bridge;

import android.app.Activity;
import java.util.concurrent.Callable;
import java.lang.Runnable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.FutureTask;

public class ConstructorCallRunnable {
    private Class klass;
    private Object[] args;
    private Constructor constr;
    private Activity activity;
    private FutureTask<Object> doit;

    public volatile Object constructed = null;
    public volatile Exception exception = null;
    
    public ConstructorCallRunnable(String perl_class, 
                                   Activity my_activity, 
                                   Object... a_args) 
        throws NoSuchMethodException, ClassNotFoundException
    {
        args = a_args;
        activity = my_activity;

        klass = Core.my_find_class(perl_class);
        Class[] arg_types = new Class[a_args.length];
        for (int i = 0; i < a_args.length; i++) {
            System.err.println("Looking at arg: " + a_args[i].toString());
            arg_types[i] = a_args[i].getClass();
        }

        constr = Core.my_find_constructor(klass, arg_types);
        System.err.println("Found constructor "+constr);

        doit = new FutureTask(new Callable() {
                public Object call() {
                    System.err.println("FutureTask callable, called");

                    // We wrap this in synchronized as we need it
                    // to block until the construction is finished.
                    Runnable run_on_ui = new Runnable() {
                            public void run() {
                                System.err.println("Before synchronizing");
                                synchronized (this) {
                                    try {
                                        System.err.println("Attempting to create "+constr+" with " + args);
                                        constructed = constr.newInstance(args);
                                    } catch (Exception e) {
                                        exception = e;
                                    }
                                    this.notifyAll();
                                }
                            }
                        };

                    synchronized (run_on_ui) {
                        System.err.println("Sending to runonuithread");
                        activity.runOnUiThread(run_on_ui);
                        try {
                            run_on_ui.wait();
                        } catch (InterruptedException e) {
                            System.err.println("runOnUiThread attempt interrupted" +e);
                        }
                    }

                    return constructed;
                }
            }
            );
        doit.run();
    }

    public Object getResult()
        throws Exception
    {
        // We need to wait here until run() has actually finished!
        // finished.acquire();
        /*
        Boolean got_finished = finished.tryAcquire(1, 1, TimeUnit.SECONDS);
        if (got_finished) {
            System.err.println("Already finished the ConstructorCallRunnable");
        } else {
            System.err.println("ConstructorCallRunnable not yet finished, but continuing anyway");
        }
        */
        Object created = doit.get();
        if (exception != null) {
            throw exception;
        }

        return created;
    }
}
