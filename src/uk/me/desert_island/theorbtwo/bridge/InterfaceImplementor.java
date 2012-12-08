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
        InvocationHandler handler = new MyInvocationHandler(callback);
        // There's *got* to be a better way of doing this.
        Class[] ifaces = new Class[1];
        ifaces[0]=iface;

        return Proxy.newProxyInstance(loader, ifaces, handler);
    }
    
    public static class MyInvocationHandler implements InvocationHandler {
        private PassthroughRunnable callback;

        public MyInvocationHandler(PassthroughRunnable a_callback) {
            callback = a_callback;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) {
            System.err.println("Invoke, method (long) name: "+method.toGenericString());
            // FIXME: Return?
            callback.run_extended(this, proxy, method, args);
            return null;
        }
    }
}
