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

        public MyInvocationHandler(PassthroughRunnable a_callback, String a_interface_name) {
            callback = a_callback;
            interface_name = a_interface_name;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable
        {
            System.err.println("Invoke, method (long) name: "+method.toGenericString());
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
                    return "InterfaceImplementor["+interface_name+"]";
                } else {
                    // Just method.getName().equals("toString") ?
                    // Less junk over the wire please!
                    return method.invoke(proxy, args);
                }
            }
            
            // These will always return null immediately / ignore returns, do we care?
            callback.run_extended(this, proxy, method_info, args);
            return null;
        }
    }
}
