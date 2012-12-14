HowTo use Object-Remote-Java (JavaBridge) with Perl on Android
==============================================================

Object::Remote
--------------

[Object::Remote][0] is a Perl module which can be used to run your Perl program or script on a remote host and return the results, without needing to run any install steps. It does this by implementing a JSON based protocol. Instead of sending objects or coderefs around, it stores an each with an id at both ends of the conversation, and just indicates the id to the other end.

Objects can be created, and methods called. The results are returned as either simple scalar data, or as IDs of the results, which can be further queried. All this detail is hidden to the actual user, so we can just treat objects as local. 

Object::Remote will also transport modules from one Perl host to the other (including those necessary to run itself), so backend hosts only need an installation of Perl (of v5.8.1 and above).

I'm told there will be a CPAN release of Object::Remote Real Soon Now(tm).

Object-Remote-Java
------------------

Object::Remote Connections can be made via SSH, to an instance of Perl installed on the remote host. In order to allow a connection to a non-Perl backend, we have added a patch to allow the protocol to be spoken over a straight TCP connection. As Java is not interpreted, the Java based backend comes as a prebuilt application, that needs to be started on the remote host. It can also be subclassed to include in your own application. Object-Remote-Java is [on github][object-remote-java-github].

Android specifics
-----------------

To run Object-Remote-Java on Android, we also packaged it as an .apk file (Android Package), called JavaBridge [which you can download from box.net here][java-bridge-download] You can also subclass the main [Activity][android-activity] if you wish to add functionality to it. The [RunPerl][run-perl] application does this, and also bundles a copy of Perl. [Download a pre-built copy here][run-perl-download].

You can run the Perl script either on Android itself (using RunPerl or sl4a or however you like), or, as this is over TCP, from another host.

To use Android rather than plain Java, there are a few caveats: 

* You [cannot update the GUI][main-thread] (create new UI controls etc) from outside of the thread that initially created the GUI. We provide a way to do this, see below.

The examples below are Android specific, with links to the Android developer documentation.

Class names
-----------

To represent a Java class name in Perl, simply Perlify it by replacing all the "." with "::":

    android.widget.EditText becomes android::widget::EditText

Inner classes can also be used, join the two parts together with a "$":

Initial connection
------------------

Create a connection object which will be used to communicate between Perl and Java. The Java side listens on port 9849:

    use Object::Remote::Connector::TCP;
    my $conn = Object::Remote->connect('tcp://10.1.1.1:9849/');


Functions / Static methods
--------------------------

To call a Java static method in Object::Remote, first use the "can::on" method to retrieve an object representing the method (in Perl this is a remote code ref) and pass it the connection object to communicate over.  Then call the resulting code ref.

For example "get\_actvity" is a static method on uk.me.desert_island.theorbtwo.bridge.AndroidServiceStash:

    my $get_actvity_sub = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity');
    my $activity = $get_activity->();

You can of course do this in one call:

    my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();


Objects and Methods
-------------------

To create a new Java object use the "new::on" method, passing in the connection object again:

    my $callbacks = java::util::HashMap->new::on($conn);

Objects created in this way are actually Object::Remote::Proxy objects, and remember which connection they are attached to. Thus later method calls do not need to be passed the connection object.

To call methods with simple scalar arguments, just call them:

    $edit_text->setLines(20);

To pass another object to a method, you need to have created it using
the Object::Remote connection first, or retrieved it from another
method:

    my $edit_text = android::widget::EditText->new::on($conn, $activity);

Constants
---------

Currently in order to get a class constant, which Android uses a lot, you need to have an instance of the class:

    my $overall_layout = android::widget::LinearLayout->new::on($conn, $activity);
    my $vertical = $overall_layout->__get_property('VERTICAL');
    $overall_layout->setOrientation($vertical);
    
(Or you can look up the actual value in the Android docs and use that..)

Interfaces, Coderefs/Callbacks
-----------------------------

In order to implement callbacks, for example the event code that runs when a user clicks a button in the UI, usually in Java you need to create a new class as which implements an interface. Object-Remote-Java provides a class which implemetns the interface for you, using a Proxy and Invocation Handler. For this we need to know what the inner class name of the interface is. For example, 'android.view.View.OnClickListener' means the inner class 'OnClickListener' in the 'android.view.View' class. To create the new instance:

Write a code reference containing the code to run, this will get called for every method invocation on your handler, that isn't a basic Object method:

    my $button_click_event = sub {
        my ($invocation_handler, $this, $method_info, $args) = @_;
        my ($method, $decl_class, $short_name, $long_name) = @$method_info;
        
        if($method_name eq 'onClick') {
            print "I was clicked!\n";
        }
    };

Create an interface creator, this is a static method on a special java bridge class:

    my $interface_creator = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance');

Create the interface subclass and assign the code reference to be called:

    my $button_event_listener = $interface_creator->('android.view.View$OnClickListener', $button_click_event);
    
Then tell the control to use the new listener:

    $button->setOnClickListener($button_event_listener);
    
    my $button_event_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.view.View$OnClickListener', $button_click_event);

The code reference will be wrapped in a [Runnable][runnable] on the Java side.

Updating the GUI
----------------

As mentioned above, in order to actually change the UI on the Android application, you need to run code in the same thread that the original UI was created from. Luckily the API provides us a method to do this "runOnUiThread", an Activity method. We can't pass it a code reference to call, as these run in their own threads. Currently we need to make a new single-method Runnable, and ask the Activity to call it:

    my $update_text = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $edit_text, 'setText', [$file_content]);
    $activity->runOnUiThread($update_text);

The upside of this is that it need to doesn't call back to the Perl layer in order to actually run. The downside is that so far it supports just one method call.

Activity events
---------------

There are a number of events that normally one would implement as methods in the Activity class itself. We have added stub methods, which will pull the actual Runnables to call out of a HashMap (which is similar to a Perl hash). Currently this covers "[onActivityResult][onactivityresult]", needed to respond to the result of a async activity, and "[onConfigurationChanged][onconfigurationchanged]" for responding to a screen rotation or similar.

To set these, call the "setEventCallbacks" method with a HashMap:

    my $callbacks = java::util::HashMap->new::on($conn);
    $callbacks.put('on_activity_result', sub { ... });
    $activity->setEventCallbacks($callbacks);



[0]: http://git.shadowcat.co.uk/gitweb/gitweb.cgi?p=scpubgit/Object-Remote.git
[android-activity]: http://developer.android.com/guide/components/activities.html
[run-perl]: https://github.com/castaway/run-perl
[main-thread]: http://developer.android.com/guide/components/processes-and-threads.html#Threads
[runnable]: http://developer.android.com/reference/java/lang/Runnable.html
[java-bridge-download]: https://www.box.com/s/c9p4yq3fdidpywnshi0c
[object-remote-github]: https://github.com/theorbtwo/Object-Remote-Java
[run-perl-download]: https://www.box.com/s/pbc1xcd6n88cykdomdh6
[onactivityresult]: http://developer.android.com/reference/android/app/Activity.html#onActivityResult(int, int, android.content.Intent)
[onconfigurationchanged]: http://developer.android.com/reference/android/app/Activity.html#onConfigurationChanged(android.content.res.Configuration)
