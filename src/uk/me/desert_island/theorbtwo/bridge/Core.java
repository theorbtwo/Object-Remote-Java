package uk.me.desert_island.theorbtwo.bridge;

import java.io.PrintStream;
import java.io.IOException;
import java.lang.Class;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;

public class Core {
    private static HashMap<String, Object> known_objects = new HashMap<String, Object>();
    
    private static Object handle_call(JSONArray incoming, PrintyThing err) 
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
                    }
                    
                    
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
            if (do_what.equals("call") && obj instanceof CanResult) {
                // >>> ["call","139214384","class_call_handler",0,"call","uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash","can","get_service"]
                // <<< ["call_free","NULL","139214384","done",{"__remote_code__":"uk.me.desert_island.theorbtwo.bridge.CanResult:410c7850"}]
                // >>> ["call","139097736","uk.me.desert_island.theorbtwo.bridge.CanResult:410c7850","","call"]
                // <<< ["call_free","NULL","139097736","done",{"__remote_object__":"uk.me.desert_island.theorbtwo.bridge.JavaBridgeService:410b83f0"}]

                //  0       1          2                                                         3  4      5                        6                                 7               8
                // ["call","139214400","uk.me.desert_island.theorbtwo.bridge.CanResult:410d8508","","call","android::widget::Toast",{"__remote_object__":"139315920"},"toast content",0]
                // ["call","139175944","uk.me.desert_island.theorbtwo.bridge.CanResult:410d9db8","","call","android::widget::Toast",{"__remote_object__":"uk.me.desert_island.theorbtwo.bridge.JavaBridgeService:410b9d40"},"toast content",0]
                // ["call","139178632","uk.me.desert_island.theorbtwo.bridge.CanResult:41093298","","call","android::widget::Toast",{"__local_object__":"uk.me.desert_island.theorbtwo.bridge.JavaBridgeService:410b0f88"},"toast content",0]


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

                if(incoming.length() > 5) {
                    if (incoming.get(5) instanceof String) {
                        // FIXME: how do we tell the difference between a static method call and a call that happens to be on a string?
                        static_call = true;
                        invocant = null;
                    } else {
                        static_call = false;
                        // JSONArray containing JSONObject with key "__local_object__", which contains the id of an object we fetched earlier (probably)
                        invocant = known_objects.get(incoming.getJSONObject(5).get("__local_object__"));
                        if(invocant == null) {
                            err.print("Can't find invocant object from:" + incoming.get(5));
                        }
                    }
                    
                    for (int i = 6; i < incoming.length(); i++) {
                        Object json_arg = incoming.get(i);
                        if(json_arg instanceof JSONObject) {
                            // FIXME: Doesn't handle __remote_code__ yet.. 
                            json_arg = known_objects.get(((JSONObject) json_arg).get("__local_object__"));
                        }
                        // if(known_objects.containsKey((String)json_arg)) {
                        //    json_arg = known_objects.get((String)json_arg);
                        // }
                        args.add(json_arg);
                        arg_types.add(json_arg.getClass());
                    }
                }
                
                Method meth = my_find_method(method_class, method_name, arg_types.toArray(new Class<?>[0]));
                return meth.invoke(invocant, args.toArray());
                
            } else {
                // Really, just a normal method call.
                //  0      1          2                              3   4             5
                // ["call","24740200","java.util.Properties:b4aa453","1","getProperty","java.class.path"]
                // ["call","139255176","android.hardware.Sensor:410d9670","","getMaximumRange"]
                ArrayList<Object> args = new ArrayList<Object>();
                ArrayList<Class> arg_types = new ArrayList<Class>();
                for (int i = 5; i < incoming.length(); i++) {
                    Object json_arg = incoming.get(i);
                    args.add(json_arg);
                    arg_types.add(json_arg.getClass());
                }

                Method meth = my_find_method(obj.getClass(), do_what, arg_types.toArray(new Class<?>[0]));
                return meth.invoke(obj, args.toArray());
                
            }
        } else {
            err.print("Huh, unknown call_type " + call_type +"\n");
        }

        return null;        
    }
    
    public static void handle_line(StringBuilder in_line, PrintStream out, PrintyThing err) 
        throws JSONException, Exception
    {
        JSONArray incoming = new JSONArray(in_line.toString());
        String command = incoming.getString(0);
        String future_objid = incoming.getString(1);
        
        Object retval = null;
        
        err.print("command_string = "+command+"', future_objid = '"+future_objid+"'\n");
        
        if (command.equals("call")) {
            retval = handle_call(incoming, err);
        } else {
            err.print("Huh?\n");
            err.print("command: "+ command + "\n");
        }

        JSONArray json_out = new JSONArray();
        json_out.put("call_free");
        json_out.put("NULL");
        json_out.put(future_objid);
        json_out.put("done");

        err.print("Return (toString): " + retval + "\n");
        err.print("Return (class):    " + retval.getClass().toString() + "\n");

        if (retval == null) {
            json_out.put(JSONObject.NULL);
        } else if (retval instanceof CanResult) {
            JSONObject return_json = new JSONObject();
            String ret_objid = obj_ident(retval);
            known_objects.put(ret_objid, retval);
            return_json.put("__remote_code__", ret_objid);
            json_out.put(return_json);
        
        } // For the wrapped basic types, we want the specific JSONObject.put for that basic type.
        else if (retval.getClass() == String.class) {
            json_out.put((String)retval);
        } else if (retval.getClass() == Float.class) {
            json_out.put(((Float)retval).doubleValue());
        } else if (retval.getClass() == Double.class) {
            json_out.put(((Double)retval).doubleValue());
        } else if (retval.getClass() == Integer.class) {
            json_out.put(((Integer)retval).intValue());
        } else {
            // FIXME: Quite possibly we shouldn't return all of these as objects, but rather as plain strings or numbers.
            JSONObject return_json = new JSONObject();
            String ret_objid = obj_ident(retval);
            known_objects.put(ret_objid, retval);
            return_json.put("__remote_object__", ret_objid);
            json_out.put(return_json);
        }
            
        out.println(json_out.toString());
        
        /*        if (command_string.equals("create")) {
            java.lang.Class klass;
            java.lang.Object obj;
            
            try {
                klass = Class.forName(split[2]);
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            try {
                obj = klass.newInstance();
                known_objects.put(obj_ident(obj), obj);
                out.printf("%s %s\n", command_id, obj_ident(obj));
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s", command_id, e.toString());
                return;
            }
            
        } else if (command_string.equals("DESTROY")) {
            known_objects.remove(split[2]);
            out.printf("%s DESTROYed\n", command_id);
            
        } else if (command_string.equals("SHUTDOWN")) {
            err.print("Got SHUTDOWN, shutting down.\n");
            
            if (!known_objects.isEmpty()) {
                for (String key : known_objects.keySet()) {
                    out.printf("Leaked %s: %s\n", key, known_objects.get(key));
                }
            }
            
            out.printf("%s SHUTDOWN\n", command_id);
            
            System.exit(3);
            
        } else if (command_string.equals("call_method")) {
            String obj_ident   = split[2];
            String method_name = split[3];
            Object obj;
            Method meth;
            Object ret;
            
            Class<?>[] argument_classes = new Class<?>[split.length - 4];
            Object[] arguments = new Object[split.length - 4];
            
            // err.printf("call_method, obj_ident='%s', method_name='%s'\n", obj_ident, method_name);
            
            obj = known_objects.get(obj_ident);
            
            for (int i = 4; i < split.length; i++) {
                arguments[i-4] = known_objects.get(split[i]);
                argument_classes[i-4] = arguments[i-4].getClass();
            }
            
            try {
                meth = my_find_method(obj.getClass(), method_name, argument_classes);
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            try {
                ret = meth.invoke(obj, arguments);
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            out.printf("%s call_method return: %s\n", command_id, obj_ident(ret));
            known_objects.put(obj_ident(ret), ret);
            
        } else if (command_string.equals("call_static_method")) {
            Class<?> klass;
            Object ret;
            
            Class<?>[] argument_classes = new Class<?>[split.length - 4];
            Object[] arguments = new Object[split.length - 4];
            
            for (int i = 4; i < split.length; i++) {
                arguments[i-4] = known_objects.get(split[i]);
                argument_classes[i-4] = arguments[i-4].getClass();
            }
            
            try {
                klass = Class.forName(split[2]);
                ret = my_find_method(klass, split[3], argument_classes).invoke(null, arguments);
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            out.printf("%s call_static_method return: %s\n", command_id, obj_ident(ret));
            known_objects.put(obj_ident(ret), ret);
            
        } else if (command_string.equals("fetch_static_field")) {
            Class klass;
            Object ret;
            
            try {
                klass = Class.forName(split[2]);
                ret = klass.getField(split[3]).get(null);
            } catch (java.lang.Throwable e) {
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            out.printf("%s call_static_method return: %s\n", command_id, obj_ident(ret));
            known_objects.put(obj_ident(ret), ret);
            
        } else if (command_string.equals("fetch_field")) {
            Object obj;
            Object ret;
            
            //  0 1           2                                   3
            //  7 fetch_field [Ljava.lang.reflect.Method;>1b67f74 length
            try {
                obj = known_objects.get(split[2]);
                System.err.printf("fetch_field on %s for %s\n", obj.getClass().toString(), split[3]);
                System.err.printf("isArray? %s\n", obj.getClass().isArray());
                ret = obj.getClass().getField(split[3]).get(obj);
            } catch (java.lang.Throwable e) {
                e.printStackTrace();
                out.printf("%s thrown: %s\n", command_id, e.toString());
                return;
            }
            
            out.printf("%s call_static_method return: %s\n", command_id, obj_ident(ret));
            known_objects.put(obj_ident(ret), ret);
            
        } else if (command_string.equals("get_array_length")) {
            // fixme: why does fetch_field of an array not work?
            
            Object obj;
            obj = known_objects.get(split[2]);
            System.out.printf("%s num return: %d\n", command_id, java.lang.reflect.Array.getLength(obj));
            
        } else if (command_string.equals("fetch_array_element")) {
            Object obj[];
            Integer index;
            Object ret;
            
            obj = (Object[]) known_objects.get(split[2]);
            index = Integer.decode(split[3]);
            ret = obj[index];
            
            out.printf("%s call_static_method return: %s\n", command_id, obj_ident(ret));
            known_objects.put(obj_ident(ret), ret);
            
            
        } else if (command_string.equals("dump_string")) {
            Object obj = known_objects.get(split[2]);
            String out_string = (String)obj;
            Pattern backslash_pattern = Pattern.compile("\\\\");
            Pattern newline_pattern = Pattern.compile("\n");
            
            out_string = backslash_pattern.matcher(out_string).replaceAll("\\\\");
            out_string = newline_pattern.matcher(out_string).replaceAll("\\n");
            
            out.printf("%s dump_string: '%s'\n", command_id, out_string);
            
        } else if (command_string.equals("make_string")) {
            String the_string = split[2];
            
            the_string = Pattern.compile("\\x20").matcher(the_string).replaceAll(" ");
            the_string = Pattern.compile("\\n").matcher(the_string).replaceAll("\n");
            the_string = Pattern.compile("\\\\").matcher(the_string).replaceAll("\\\\");
            
            known_objects.put(obj_ident(the_string), the_string);
            out.printf("%s %s\n", command_id, obj_ident(the_string));
            
        */
    }
    
    private static Class<?> my_find_class(String perl_name) {
        String java_name;

        if (perl_name.startsWith("Object::Remote::Java::")) {
            java_name = perl_name.substring(22);
        } else {
            java_name = perl_name;
        }
        java_name = java_name.replaceAll("::", ".");
        System.err.printf("perl name %s is java name %s\n", perl_name, java_name);

        try {
            return Class.forName(java_name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    // Returns true if we have *any* method that might work for this name, when we need a result for ->can.
    private static boolean has_any_methods(Class<?> klass, String name) {
        Method[] meths = klass.getMethods();
        for (Method meth : meths) {
            if (meth.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Method my_find_method(Class<?> klass, String name, Class<?>[] args) 
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
            boolean args_match = true;
            Class<?>[] m_args;

            if (!m.getName().equals(name)) {
                continue;
            }

            m_args = m.getParameterTypes();

            if (m_args.length != args.length) {
                continue;
            }

            System.err.printf("We have a strong canidate %s\n", m.toString());

            for (int i=0; i<args.length; i++) {
        
                String wanted_name = args[i].getName();
                String got_name = m_args[i].getName();

                // Java Language Specification, 3rd edition, 5.3 -- method arguments can have...
                // • an identity conversion (§5.1.1)
                if (args[i].equals(m_args[i])) {
                    continue;
                }

                // • a widening primitive conversion (§5.1.2)
                // (Not applicable; our arguments will always be boxed types.)

                // • a widening reference conversion (§5.1.5)
                if (m_args[i].isAssignableFrom(args[i])) {
                    System.err.printf("%s vs %s is OK (isAssignableFrom / a widening reference conversion\n",
                                      wanted_name, got_name
                                      );
                    continue;
                }

                // • a boxing conversion (§5.1.7) optionally followed by widening reference conversion
                // • an unboxing conversion (§5.1.8) optionally followed by a widening primitive conversion.

                // Java Language Specification, 3rd edition, 5.1.8
                if (wanted_name.equals("java.lang.Boolean") && got_name.equals("boolean")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Byte") && got_name.equals("byte")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Character") && got_name.equals("char")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Short") && got_name.equals("short")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Integer") && got_name.equals("int")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Long") && got_name.equals("long")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Float") && got_name.equals("float")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Double") && got_name.equals("double")) {
                    continue;
                }
        
                if (wanted_name.equals("java.lang.Integer") && got_name.equals("int")) {
                    continue;
                }
        
                System.err.printf("Argument mismatch on wanted_name='%s' vs got_name='%s'\n", wanted_name, got_name);
                args_match = false;
                break;
            }

            if (args_match) {
                System.err.printf("We got it: %s\n", m.toString());
                return m;
            }
        }

        throw new NoSuchMethodException();
    }

    private static String obj_ident(java.lang.Object obj) {
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
