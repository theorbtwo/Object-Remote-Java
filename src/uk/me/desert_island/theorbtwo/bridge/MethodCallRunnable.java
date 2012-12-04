package uk.me.desert_island.theorbtwo.bridge;

import java.lang.Runnable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class MethodCallRunnable implements Runnable {
    private Object invocant;
    private String method_name;
    private Object[] args;
    private Method meth;
    
    public MethodCallRunnable(Object a_invocant, String a_method_name, Object... a_args) 
        throws NoSuchMethodException
    {
        invocant = a_invocant;
        method_name = a_method_name;
        args = a_args;

        Class[] arg_types = new Class[a_args.length];
        for (int i = 0; i < a_args.length; i++) {
            System.err.println("Looking at arg: " + a_args[i].toString());
            arg_types[i] = a_args[i].getClass();
        }

        meth = Core.my_find_method(a_invocant.getClass(), a_method_name, arg_types);
    }
    
    public void run() 
    {
        try {
            meth.invoke(invocant, args);
        } catch (IllegalAccessException e) {
            // HELP ME, WHAT CAN I DO HERE?
        } catch (InvocationTargetException e) {
            // HELP ME, WHAT CAN I DO HERE?
        }
    }
}