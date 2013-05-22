package uk.me.desert_island.theorbtwo.bridge;

import java.io.PrintStream;
import java.io.IOException;
import java.lang.Class;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Core {
    private PrintStream out;
    public PrintyThing err;

    public Core (PrintStream out_str, PrintyThing err_str) {
        out = out_str;
        err = err_str;
    }

    private HashMap<String, Object> known_objects = new HashMap<String, Object>();
    private HashMap<String, Future> waiting_futures = new HashMap<String, Future>();

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private Object handle_call(JSONArray incoming, PrintyThing err) 
        throws JSONException, Exception
    {
        //  0      1          2                    3 4      5                                          6     7
        // ["call","31247544","class_call_handler",0,"call","Object::Remote::Java::java::lang::System","can","getProperties"]

        String call_type = incoming.getString(2);
        
        if (call_type.equals("class_call_handler")) {
            // wantarray may be undef/0/1, so call it an Object, not an int or a string.
            //Object wantarray = incoming.get(3);
            String call_type_again = incoming.getString(4);
            if (call_type_again.equals("call")) {
                String perl_class = incoming.getString(5);
                String perl_method = incoming.getString(6);
                
                if (perl_method.equals("can")) {
                    String perl_wanted_method = incoming.getString(7);
                    Class<?> klass = my_find_class(perl_class);
                    if (has_any_methods(klass, perl_wanted_method)) {
                        err.print("Need to return true for " + perl_class + "->can('" + perl_wanted_method +"')\n");
                        return (new CanResult(klass, perl_wanted_method));
                    } else {
                        err.print("Need to return false for " + perl_class + "->can('" + perl_wanted_method+"')\n");
                        return null;
                    }
                    
                } else if (perl_method.equals("new")) {
                    // Right.  This is a constructor, which in Java isn't a method call, but a special unique flower.
                    //  0      1           2                    3 4      5                           6     7
                    // ["call","136826936","class_call_handler",0,"call","android::widget::EditText","new",{"__local_object__":"uk.me.desert_island.theorbtwo.bridge.JavaBridgeService:410a5a10"}]
                    Class<?> klass = my_find_class(perl_class);
                    err.print("Calling 'new' on .. " + klass.getName());
                    ArrayList<Object> args = new ArrayList<Object>();
                    ArrayList<Class> arg_types = new ArrayList<Class>();
                    // magic number 7 = start extracting args at this index
                    convert_json_args_to_java(incoming, 7, arg_types, args, err);
                    Constructor con = my_find_constructor(klass, arg_types.toArray(new Class<?>[0]));
                    return con.newInstance(args.toArray());
                } else {
                    err.print("class call for " + perl_class +"->" + perl_method +"\n");
                }
            } else {
                err.print("Huh?\n");
                err.print("call_type_again: '" + call_type_again +"'\n");
            }
        } else if (known_objects.containsKey(call_type)) {
            // A normal method call.
            Object obj = known_objects.get(call_type);
            String do_what = incoming.getString(4);

            if (do_what.equals("__get_property")) {
                //  0       1           2                                             3   4                 5
                //                      call_type                                         do_what
                // ["call", "34244816", "android.content.res.Configuration:410a6598", "", "__get_property", "densityDpi"]
                
                String property_name = incoming.getString(5);
                
                Class klass = obj.getClass();
                return klass.getField(property_name).get(obj);
            } else if (do_what.equals("call") && obj instanceof CanResult) {
                // >>> ["call","139214384","class_call_handler",0,"call","uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash","can","get_service"]
                // >>> ["call","139097736","uk.me.desert_island.theorbtwo.bridge.CanResult:410c7850","","call"]

                //  0       1          2                                                         3  4      5                        6                                 7               8
                // ["call","139214400","uk.me.desert_island.theorbtwo.bridge.CanResult:410d8508","","call","android::widget::Toast",{"__remote_object__":"139315920"},"toast content",0]
                // ["call","27108224" ,"uk.me.desert_island.theorbtwo.bridge.CanResult:410ce078","","call","android.hardware.SensorEventListener",{"__remote_code__":"24416064"}]

                // A call to a coderef (a CanResult from ->can::on, most likely).
                // Object wantarray = incoming.getString(3);
                CanResult canresult = (CanResult)obj;
                //if (incoming.length() != 5) {
                //    throw(new Exception("calls with arguments not yet supported"));
                //}
                Class method_class = canresult.klass();
                String method_name = canresult.method_name();

                Boolean static_call;
                Object invocant = null;
                ArrayList<Object> args = new ArrayList<Object>();
                ArrayList<Class> arg_types = new ArrayList<Class>();

                // if(incoming.length() > 5) {
                //     if (incoming.get(5) instanceof String) {
                //         // FIXME: how do we tell the difference between a static method call and a call that happens to be on a string?
                //         static_call = true;
                //         invocant = null;
                //     } else {
                //         static_call = false;
                //         // JSONArray containing JSONObject with key "__local_object__", which contains the id of an object we fetched earlier (probably)
                //         invocant = known_objects.get(incoming.getJSONObject(5).get("__local_object__"));
                //         if(invocant == null) {
                //             err.print("Can't find invocant object from:" + incoming.get(5));
                //         }
                //     }                
                // }

                //  0      1          2                                                         3  4      5                                      6
                // ["call","36405984","uk.me.desert_island.theorbtwo.bridge.CanResult:410b0ae8","","call","android.hardware.SensorEventListener",{"__remote_code__":"33713760"}

                // magic number 5 = start extracting args at this index
                convert_json_args_to_java(incoming, 5, arg_types, args, err);
                
                Method meth = my_find_method(method_class, method_name, arg_types.toArray(new Class<?>[0]));
                return meth.invoke(invocant, args.toArray());

                
            } else {
                // Really, just a normal method call.
                //  0      1          2                              3   4             5
                // ["call","24740200","java.util.Properties:b4aa453","1","getProperty","java.class.path"]
                // ["call","139255176","android.hardware.Sensor:410d9670","","getMaximumRange"]
                ArrayList<Object> args = new ArrayList<Object>();
                ArrayList<Class> arg_types = new ArrayList<Class>();
                // magic number 5 = start extracting args at this index
                convert_json_args_to_java(incoming, 5, arg_types, args, err);

                Method meth = my_find_method(obj.getClass(), do_what, arg_types.toArray(new Class<?>[0]));
                return meth.invoke(obj, args.toArray());
                
            }
        } else {
            err.print("Huh, unknown call_type " + call_type +"\n");
        }

        return null;
    }
    
    public void handle_line(StringBuilder in_line) 
        throws JSONException, Exception
    {
        final JSONArray incoming = new JSONArray(in_line.toString());
        final String command = incoming.getString(0);
        final String future_objid = incoming.getString(1);
        
        err.print("command_string = '"+command+"', future_objid = '"+future_objid+"'\n");
        
        if (command.equals("call")) {
            executor.execute(new Runnable() {
                    public void run() {
                        Object retval;
                        Boolean is_fail;
        
                        try {
                            retval = handle_call(incoming, err);
                            is_fail = false;
                        } catch (Throwable e) {
                            is_fail = true;
                            // Good god, but this is ugly.
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            
                            if (e instanceof java.lang.reflect.InvocationTargetException) {
                                e = e.getCause();
                            }
                            
                            // FIXME: Framework (O::R) should do this?
                            e.printStackTrace();
                            e.printStackTrace(new PrintStream(baos));
                            retval = e.toString() + " message " + e.getMessage() + " stack trace: " + baos.toString();
                        }
                        
                        send_result(is_fail, future_objid, retval);
                    } /*run*/
                } /* runnable inline subclass */
                ); /* executor.execute() */
        } /* if command = call */
        else {
            err.print("Huh?\n");
            err.print("command: "+ command + "\n");
            throw(new Exception("Protocol error?"));
        }
    }

    public synchronized void send_result(Boolean is_fail, String future_objid, Object retval)  {
        JSONArray json_out = new JSONArray();
        json_out.put("call_free");
        json_out.put("NULL");
        json_out.put(future_objid);
        if (is_fail) {
            json_out.put("fail");
        } else {
            json_out.put("done");
        }

        if (retval == null) {
            err.print("Return is null\n");
        } else {
            err.print("Return (toString): " + retval + "\n");
            err.print("Return (class):    " + retval.getClass().toString() + "\n");
        }

        // FIXME: put this out to the user, instead of the catlog?
        try {
            json_out.put(convert_java_args_to_json(retval));
        } catch (JSONException e) {
            err.print("Error trying to convert_java_args_to_json: "+e);
        }

        out.println(json_out.toString());
    }

    public Future run_remote_code(Boolean wait, String remote_code_id, Object... args) {
        System.err.println("Trying to run remote code "+remote_code_id);

        // Now we need to construct a "call remote code" line to throw at the other side:
        // 11476 >>> ["call","38031432","id_from_remote_code",null,"call"]

        JSONArray code_request = new JSONArray();
        String my_id = obj_ident(code_request); // We will need to remember this for later, maybe?
        code_request.put("call");
        code_request.put(my_id);
        code_request.put(remote_code_id);
        code_request.put(JSONObject.NULL);
        code_request.put("call");

        if (args.length > 0) {
            for (Object a : args) {
                //err.print("Argument: "+a+"\n");
                try {
                    code_request.put(convert_java_args_to_json(a));
                } catch (JSONException e) {
                    err.print("WTF, got JSONException packing arguments for run_remote_code: "+e);
                }
            }
        }
        
        Future future = null;
        if (wait) {
            future = new NullFuture();
            this.waiting_futures.put(my_id, future);
        }
        out.println(code_request.toString());

        return future;
    }

    public void run_remote_code(String remote_code_id, Object... args) {
        run_remote_code(false, remote_code_id, args);
    }

    private Object convert_java_args_to_json(Object input) 
        throws JSONException
    {
        if (input == null) {
            return JSONObject.NULL;
        } else if (input instanceof CanResult) {
            JSONObject return_json = new JSONObject();
            String ret_objid = obj_ident(input);
            known_objects.put(ret_objid, input);
            return_json.put("__remote_code__", ret_objid);

            return return_json;
        } else if (input.getClass().isArray()) {
            JSONArray return_json = new JSONArray();

            int length = Array.getLength(input);
            for (int i=0; i < length; i++) {
                return_json.put(i, convert_java_args_to_json(Array.get(input, i)));
            }
            
            return return_json;
        } // For the wrapped basic types, we want the specific JSONObject.put for that basic type.
        else if (input.getClass() == String.class) {
            return (String)input;
        } else if (input.getClass() == Float.class) {
            return ((Float)input).doubleValue();
        } else if (input.getClass() == Double.class) {
            return ((Double)input).doubleValue();
        } else if (input.getClass() == Integer.class) {
            return ((Integer)input).intValue();
        } else if (input.getClass() == Long.class) {
            return ((Long)input).longValue();
        } else if (input.getClass() == Boolean.class) {
            // Use perl conventions for these
            if ((Boolean)input) {
                return 1;
            } else {
                // This probably won't have the special semantics of the one true false value, which is "" in string context and 0 in numeric context, all without warning.
                return JSONObject.NULL;
            }
        } else {
            JSONObject return_json = new JSONObject();
            String ret_objid = obj_ident(input);
            err.print("Store object: " + ret_objid);
            known_objects.put(ret_objid, input);
            return_json.put("__remote_object__", ret_objid);
            
            return return_json;
        }
    }


    private void convert_json_args_to_java
        (JSONArray incoming, int start_index, ArrayList<Class> arg_types, ArrayList<Object> args, PrintyThing err) throws JSONException {
        for (int i = start_index; i < incoming.length(); i++) {
            Object json_arg = incoming.get(i);
            err.print("JSON arg: " + json_arg.toString());

            if (json_arg instanceof JSONArray) {
                ArrayList<Object> inner_args = new ArrayList<Object>();
                ArrayList<Class>  inner_arg_types = new ArrayList<Class>(); 
                convert_json_args_to_java((JSONArray)json_arg, 0, inner_arg_types, inner_args, err);
                // This makes json_arg always be an array of object.  If this ends up being a problem, we'll have to find the least-restrictive class that is a superclass of all the elements?
                json_arg = inner_args.toArray();
            } else if (json_arg instanceof JSONObject) {
                JSONObject json_arg_obj = (JSONObject) json_arg;
                if (json_arg_obj.has("__local_object__")) {
                    err.print("Looking for known obj: " + json_arg_obj.get("__local_object__"));
                    json_arg = known_objects.get(json_arg_obj.get("__local_object__"));
                } else if (json_arg_obj.has("__remote_code__")) {
                    // this just contains the id of a coderef on the remote side, which we could then ask it to run..

                    final String code_id = (String)json_arg_obj.get("__remote_code__");
                    //final Core the_core = this;
                    json_arg = new PassthroughRunnable(code_id, this);
                } else {
                    err.print("WTF: JSOObject is not a local_object or a remote_code? "+json_arg_obj.toString());
                }
            }

            args.add(json_arg);
            arg_types.add(json_arg.getClass());
        }
    }

    
    protected static Class<?> my_find_class(String perl_name) throws ClassNotFoundException {
        String java_name;

        if (perl_name.startsWith("Object::Remote::Java::")) {
            java_name = perl_name.substring(22);
        } else {
            java_name = perl_name;
        }
        java_name = java_name.replaceAll("::", ".");
        System.err.printf("perl name %s is java name %s\n", perl_name, java_name);

        return Class.forName(java_name);
    }

    // Returns true if we have *any* method that might work for this name, when we need a result for ->can.
    private boolean has_any_methods(Class<?> klass, String name) {
        err.print("Looking for any methods like "+name);
        Method[] meths = klass.getMethods();
        for (Method meth : meths) {
            err.print("Trying method:"+meth.getName()+" against " + name);
            if (meth.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    // Java Language Specification, Java 7 edition, section 5.1.1
    static boolean check_identity_conversion(Class have_arg, Class want_arg) {
        return (have_arg.equals(want_arg));
    }

    // Java Langauge Specification, Java 7 edition, section 5.1.2
    static boolean check_widening_primitive_conversion(Class have_arg, Class want_arg) {
        if (have_arg == null) {
            return false;
        }

        if (have_arg.equals(byte.class) && (want_arg.equals(short.class) || 
                                            want_arg.equals(int.class) ||
                                            want_arg.equals(long.class) ||
                                            want_arg.equals(float.class) ||
                                            want_arg.equals(double.class))) {
            return true;
        }
        
        if (have_arg.equals(short.class) && (want_arg.equals(int.class) ||
                                             want_arg.equals(long.class) ||
                                             want_arg.equals(float.class) ||
                                             want_arg.equals(double.class))) {
            return true;
        }
        
        if (have_arg.equals(char.class) && (want_arg.equals(int.class) ||
                                            want_arg.equals(long.class) ||
                                            want_arg.equals(float.class) ||
                                            want_arg.equals(double.class))) {
            return true;
        }
        
        if (have_arg.equals(int.class) && (want_arg.equals(long.class) ||
                                           want_arg.equals(float.class) ||
                                           want_arg.equals(double.class))) {
            return true;
        }
        
        if (have_arg.equals(long.class) && (want_arg.equals(float.class) ||
                                            want_arg.equals(double.class))) {
            return true;
        }

        if (have_arg.equals(float.class) && (want_arg.equals(double.class))) {
            return true;
        }
        
        return false;
    }

    // Java Language Specification, Java 7 edition, section 5.1.5
    public static boolean check_widening_reference_conversion(Class have_arg, Class want_arg) {
        // Technically, this also returns true on an identity conversion, but I'm fairly certian that it never matters.
        return (want_arg.isAssignableFrom(have_arg));
    }
    
    // Java Language Specification, Java 7 edition, section 5.1.8
    public static Class do_unboxing_conversion(Class from) {
        if (from.equals(Boolean.class)) {
            return boolean.class;
        }
        if (from.equals(Byte.class)) {
            return byte.class;
        }
        if (from.equals(Short.class)) {
            return short.class;
        }
        if (from.equals(Character.class)) {
            return char.class;
        }
        if (from.equals(Integer.class)) {
            return int.class;
        }
        if (from.equals(Long.class)) {
            return long.class;
        }
        if (from.equals(Float.class)) {
            return float.class;
        }
        if (from.equals(Double.class)) {
            return double.class;
        }

        // FIXME: How do I want to signal that unboxing isn't approprate?
        return null;
    }

    private static boolean compare_method_args(Class<?>[] args, Class<?>[] m_args)
    {
        boolean found=true;
        
        for (int i=0; i<args.length; i++) {
            Class wanted_class = m_args[i];
            Class got_class = args[i];

            String wanted_name = wanted_class.getName();
            String got_name = got_class.getName();

            System.err.printf("Comparing method args, wanted=%s, got=%s\n", wanted_name, got_name);
            
            // Java Language Specification, 3rd edition, 5.3 -- method arguments can have...
            // http://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.3
            //  Java language specification, java 7 edition, section 5.3
            // • an identity conversion (§5.1.1)
            if (check_identity_conversion(got_class, wanted_class)) {
                System.err.printf("OK, identity conversion\n");
                continue;
            }
            
            // • a widening primitive conversion (§5.1.2)
            // (Not applicable; our arguments will always be boxed types.)
            
            // • a widening reference conversion (§5.1.5)
            if (check_widening_reference_conversion(got_class, wanted_class)) {
                System.err.printf("OK, widening reference conversion\n");
                continue;
            }
            
            // • a boxing conversion (§5.1.7) optionally followed by widening reference conversion
            // (For now, this isn't applicable; our arguments will always be boxed types, and we already did straight widening reference conversions above.)

            // • an unboxing conversion (§5.1.8) optionally followed by a widening primitive conversion.
            Class unboxed_got = do_unboxing_conversion(got_class);
            if (unboxed_got != null && unboxed_got.equals(wanted_class)) {
                System.err.printf("OK, unboxing (without widening primitive conversion)\n");
                continue;
            }
            if (check_widening_primitive_conversion(do_unboxing_conversion(got_class), wanted_class)) {
                System.err.printf("OK, unboxing, then widening primitive conversion\n");
                continue;
            }

            // "If, after the conversions listed above have been applied, the resulting type is a
            // raw type (§4.8), an unchecked conversion (§5.1.9) may then be applied."
            // This seems to be about parametic types, which we've been ignoring so far, and shall continue to ignore.
            
            // The specification notes that implicit narrowing of integers is allowed in
            // assignment contexts, but not in method call contexts.
            
            System.err.printf("Argument mismatch on wanted_name='%s' vs got_name='%s'\n", wanted_name, got_name);
            found = false;
            break;
        }

        return found;
    }
    
    protected static Constructor my_find_constructor(Class<?> klass, Class<?>[] args)
        throws SecurityException, NoSuchMethodException
    {
        System.err.printf("my_find_constructor, class = ", klass.getName(), ", args = ...");
        for (Class<?> arg_k : args) {
            System.err.printf("\n arg: "+arg_k.getName());
        }
        try {
            Constructor c;
            System.err.printf("Trying to find an obvious constructor\n");
            c = klass.getConstructor(args);
            System.err.printf("Still here after getConstructor() call\n");
            return c;
        } catch (NoSuchMethodException e) {
            // Do nothing (just don't return).
        }

        System.err.printf("Trying non-obvious matches\n");
        // We do not have a perfect match; try for a match where the
        // method has primitive types but args has corresponding boxed types.
        for (Constructor c : klass.getConstructors()) {
            Class<?>[] c_args;

            c_args = c.getParameterTypes();

            if (c_args.length != args.length) {
                continue;
            }

            System.err.printf("We have a strong candidate %s\n", c.toString());

            if (compare_method_args(args, c_args)) {
                System.err.printf("We got it: %s\n", c.toString());
                return c;
            }
        }

        String arguments_string = "";
        for (Class<?> arg_class : args) {
            arguments_string += arg_class.getName() + ", ";
        }

        throw new NoSuchMethodException("Cannot find constructor on class "+klass.getName()+" with arguments "+arguments_string);
    }
    
    public static Method my_find_method(Class<?> klass, String name, Class<?>[] args) 
        throws SecurityException, NoSuchMethodException
    {
    
        try {
            Method m;
            System.err.printf("Trying to find an obvious method for name=%s\n", name);
            m = klass.getMethod(name, args);
            System.err.printf("Still here after getMethod() call\n");
            return m;
        } catch (NoSuchMethodException e) {
            // Do nothing (just don't return).
        }

        System.err.printf("Trying non-obvious matches\n");
        // We do not have a perfect match; try for a match where the
        // method has primitive types but args has corresponding boxed types.
        for (Method m : klass.getMethods()) {
            Class<?>[] m_args;

            if (!m.getName().equals(name)) {
                continue;
            }

            m_args = m.getParameterTypes();

            if (m_args.length != args.length) {
                continue;
            }

            System.err.printf("We have a strong canidate %s\n", m.toString());

            if (compare_method_args(args, m_args)) {
                System.err.printf("We got it: %s\n", m.toString());
                return m;
            }
        }

        String arguments_string = "";
        for (Class<?> arg_class : args) {
            arguments_string += arg_class.getName() + ", ";
        }

        throw new NoSuchMethodException("Cannot find method named "+name+" on class "+klass.getName()+" with arguments "+arguments_string);
    }



    protected static String obj_ident(java.lang.Object obj) {
        StringBuilder ret = new StringBuilder();
        if (obj == null) {
            return "null";
        }
        ret = ret.append(obj.getClass().getName());
        ret = ret.append(":");
        ret = ret.append(Integer.toHexString(System.identityHashCode(obj)));

        return ret.toString();
    }
}
