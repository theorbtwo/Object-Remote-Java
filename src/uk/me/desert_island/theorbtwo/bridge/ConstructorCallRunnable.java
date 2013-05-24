package uk.me.desert_island.theorbtwo.bridge;

import java.lang.Runnable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorCallRunnable implements Runnable {
    private Class klass;
    private Object[] args;
    private Constructor constr;

    public volatile Object constructed = null;
    
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
        } catch (IllegalAccessException e) {
            // HELP ME, WHAT CAN I DO HERE?
        } catch (InvocationTargetException e) {
            // HELP ME, WHAT CAN I DO HERE?
        } catch (InstantiationException e) {
        }
    }

    public Object getResult() {
        return constructed;
    }
}