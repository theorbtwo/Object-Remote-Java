Object-Remote-Java
==================

Object::Remote is a Perl module which can be used to run your Perl program or script on a remote host and return the results, without needing to run any install steps. The only thing that must exist on the host is Perl itself.

Object-Remote-Java (or JavaBridge) is an Object::Remote compatible backend written in Java. It can be used to call Java objects and methods by writing Perl code.

The backend can be built to run standalone, producing a set of Java classes that will run with any (most) Java interpreters, or as an Android application.

Example use with Android
-------------------------

    use Object::Remote;
    use Object::Remote::Connector::TCP;
    my $conn = Object::Remote->connect('tcp://192.168.1.2:9849/');

    # Fetch the Android Activity object:
    my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

    # Create some GUI objects:
    my $edit_text = android::widget::EditText->new::on($conn, $activity);
    my $button = android::widget::Button->new::on($conn, $activity);
    $button->setText("DO IT!");

    # Layout the GUI objects:
    my $layout = android::widget::LinearLayout->new::on($conn, $activity);
    $layout->addView($edit_text);
    $layout->addView($button);

    # We can only change the GUI in the GUI thread, so create a Runnable
    # and pass it to runOnUiThread:
    my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$layout]);
    $activity->runOnUiThread($runnable);

    # Set a callback coderef to call when the configuration changes:
    $activity->set_on_configuration_changed_callback(sub {
        print "Your configuration changed, and we actually managed to get here!\n";
    });


    # Kick off an event loop so that we can receive callbacks:
    my $or_loop = Object::Remote->current_loop;
    $or_loop->want_run;
    $or_loop->run_while_wanted;

More examples in _scripts/perl/_

This is a work in progress, so far we can
-----------------------------------------

* Create new objects
* Call methods on objects
* Pass objects as method arguments
* Run callbacks with no arguments

To build and install (on Android 4.0.3+)
----------------------------------------

* Download the Android SDK tools: http://developer.android.com/sdk/index.html and unpack it.
* Install the "ant" tool for your machine (available as a package in most linux distributions).
* Create a file named "local.properties" in the main checkout directory of this project, and add one line:

    sdk.dir=<path to the unpacked SDK>

* Run ant from this directory:

    ant -f build.android.xml debug

* Configure your Android device to allow installing packages outside of Google-Play, by checking the option "Unknown Sources" in the "Security" section of the "Settings" app.
* Copy the file bin/JavaBridgeActivity-debug.apk to your Android device and install and run it.
* Now you can run Perl scripts to connect to the server on <your device ip>:9849.

Older versions of Android
-------------------------

There is currently no real reason this is only building for Android 4.0.3+ except that we were tinkering with the "onConfigurationChanged" callback. To build for older versions, remove the "<android:configChanges>" tag from AndroidManifest.xml, and change the values in the "<use-sdk>" tag. See http://developer.android.com/guide/topics/manifest/uses-sdk-element.html#ApiLevels

