HowTo use Object-Remote-Java (JavaBridge) with Perl on Android
==============================================================

Object::Remote
--------------

Object::Remote is a Perl module which can be used to run your Perl program or script on a remote host and return the results, without needing to run any install steps. It does this by implementing a JSON based protocol. Instead of sending objects or coderefs around, it stores an each with an id at both ends of the conversation, and just indicates the id to the other end.

Objects can be created, and methods called. The results are returned as either simple scalar data, or as IDs of the results, which can be further queried. All this detail is hidden to the actual user, so we can just treat objects as local. 

Object::Remote will also transport modules from one Perl host to the other (including those necessary to run itself), so backend hosts only need an installation of Perl (of v5.8.1 and above).

Object-Remote-Java
------------------

Object::Remote Connections can be made via SSH, to an instance of Perl installed on the remote host. In order to allow a connection to a non-Perl backend, we have added a patch to allow the protocol to be spoken over a straight TCP connection. As Java is not interpreted, the Java based backend comes as a prebuilt application, that needs to be started on the remote host.

Android specifics
-----------------

To run Object-Remote-Java on Android, we also packaged it as an .apk file (Android Package). You can also subclass the main [Activity][android-activity] if you wish to add functionality to it. The [RunPerl][run-perl] application does this, and also bundles a copy of Perl.

You can run the Perl script either on Android itself (using RunPerl or sl4a or however you like), or, as this is over TCP, from another host.

To use Android rather than plain Java, there are a few caveats: 

* You cannot update the GUI (create new UI controls etc) from outside of the thread that initially created the GUI. We provide a way to do this, see below.

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
    my $conn = Object::Remote->connect('tcp://192.168.1.2:9849/');


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

    my $button_event_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.view.View$OnClickListener', $button_click_event);



Interfaces
----------

Updating the GUI
----------------

[0]: http://git.shadowcat.co.uk/gitweb/gitweb.cgi?p=scpubgit/Object-Remote.git
[android-activity]: http://developer.android.com/guide/components/activities.html
[run-perl]: https://github.com/castaway/run-perl
