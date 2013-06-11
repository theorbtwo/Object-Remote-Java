package uk.me.desert_island.theorbtwo.bridge;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.ClassLoader;
import uk.me.desert_island.theorbtwo.bridge.PassthroughRunnable;

public class InterfaceImplementor {
    public static Object make_instance(String interface_name, PassthroughRunnable callback) 
        throws ClassNotFoundException
    {
        Class iface = Class.forName(interface_name);
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        InvocationHandler handler = new MyInvocationHandler(callback, interface_name);
        // There's *got* to be a better way of doing this.
        Class[] ifaces = new Class[1];
        ifaces[0]=iface;

        return Proxy.newProxyInstance(loader, ifaces, handler);
    }
    
    public static class MyInvocationHandler implements InvocationHandler {
        private PassthroughRunnable callback;
        private String interface_name;
        private int this_serial;
        static int next_serial = 0;

        public MyInvocationHandler(PassthroughRunnable a_callback, String a_interface_name) {
            callback = a_callback;
            interface_name = a_interface_name;
            this_serial = next_serial++;

            System.err.println("Serial of this one: "+this_serial);
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable
        {
            System.err.println("Invoke, method (long) name: "+method.toGenericString());
            for (Object arg : args) {
                System.err.println("arg: " + arg);
            }
            // FIXME: Return?
            
            String decl_class = method.getDeclaringClass().getCanonicalName();
            String short_name = method.getName();
            String long_name = method.toGenericString();

            Object[] method_info = new Object[4];
            method_info[0] = method;
            method_info[1] = decl_class;
            method_info[2] = short_name;
            method_info[3] = long_name;

            if(decl_class.equals("java.lang.Object")) {
                if (short_name.equals("toString")) {
                    return "InterfaceImplementor["+interface_name+"("+this_serial+")]";
                //} else if (long_name.equals("public native int java.lang.Object.hashCode()")) {
                //  return this.hashCode();
                } else if(short_name.equals("equals")) {
                    if(proxy.hashCode() == args[0].hashCode()) {
                        System.err.println("returning: true");
                        return true;
                    }
                    System.err.println("returning: false");
                    return false;
                } else {
                    // Just method.getName().equals("toString") ?
                    // Less junk over the wire please!
                    // Can't use this for equals as we're comparing proxy obj (the arg) to this (the invocation handler..)
                    // there oughta be a saner way to default these!
                    Object result = method.invoke(this, args);
                    System.err.println("returning: "+result);
                    return result;
                    
                    //throw new RuntimeException("Unhandled Object method on InterfaceImplementor["+interface_name+"]: "+long_name);
                }
            }

            System.err.println("Calling:"+long_name);
            // These will always return null immediately / ignore returns, do we care?
            Object result = callback.call_extended(this, proxy, method_info, args);

            if (result == null) {
                return null;
            }

            Class<?> wanted_result_type = method.getReturnType();
            Class<?> got_result_type = result.getClass();

            System.err.println("Result class, wanted = "+wanted_result_type+" got = "+got_result_type);

            if(wanted_result_type.equals(Boolean.TYPE) && got_result_type.equals(Integer.class)) {
                System.err.println("Trying to convert Integer to boolean " + result);
                if((Integer)result == 0) {
                    result = false;
                } else {
                    result = true;
                }
            }

            for (Class<?> almost_primitive : new Class[] {Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class}) {
                // While all of these have a TYPE, Class does not, and there's no closer subclass, so Java fights me.
                Class<?> primitive = (Class)(almost_primitive.getField("TYPE").get(null));
                if (wanted_result_type.equals(primitive) && got_result_type.equals(String.class)) {
                    System.err.println("Coercing String "+result+"to "+almost_primitive);
                    /* Hopefully, passing an Integer to something wanting an int is close enough -- it should be... */
                    // Similarly, while all of these have a constructor that takes a single string (I think), Java doesn't know that.
                    result = almost_primitive.getConstructor(String.class).newInstance((String)result);
                }
            }

            System.err.println("Call to "+long_name+" returned "+result);
            return result;
        }
    }
}
