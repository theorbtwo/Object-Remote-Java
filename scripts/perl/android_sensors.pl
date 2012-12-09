#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use 5.10.1;

use Data::Dump::Streamer;
use Time::HiRes 'sleep';

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

my $edit_text = android::widget::EditText->new::on($conn, $activity);
my $button = android::widget::Button->new::on($conn, $activity);
$button->setText("DO IT!");
my $layout = android::widget::LinearLayout->new::on($conn, $activity);
$layout->addView($edit_text);
$layout->addView($button);

my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$layout]);
$activity->runOnUiThread($runnable);

my $sensor_manager = $activity->getSystemService($activity->__get_property('SENSOR_SERVICE'));
# FIXME: SensorManager.SENSOR_ACCELERMOETER is marked as depreciated,
# use Sensor.SENSOR_ACCELEROMETER instead.
# my $accel_sensor = $sensor_manager->getDefaultSensor($sensor_manager->__get_property('SENSOR_ACCELEROMETER'));
my $accel_sensor = $sensor_manager->getDefaultSensor(1); ## Sensor.TYPE_ACCELERMETER
# my $accel_sensor = $sensor_manager->getDefaultSensor(5); ## Sensor.TYPE_LIGHT

my $accel_listener;

my $accel_event_callback = sub {
  my ($invocation_handler, $this, $method, $args) = @_;
  print "In accel_event_callback: \n";
  my $method_long_name = $method->toGenericString();
  print " Method long name: $method_long_name\n";
  print " Args: ", $args, "\n";
  print " Number of args: ", 0+@$args, "\n";
  for my $i (0..@$args-1) {
    print " Argument $i: ", $args->[$i], "\n";
  }

  #Dump $args;

  if ($method_long_name eq 'public java.lang.String java.lang.Object.toString()') {
    return "".$this;
  } elsif ($method_long_name eq 'public abstract void android.hardware.SensorEventListener.onSensorChanged(android.hardware.SensorEvent)') {
    $sensor_manager->unregisterListener($accel_listener);

    my $sensor_event = $args->[0];
    say ">>Accuracy:  ", $sensor_event->__get_property('accuracy');
    say ">>Timestamp: ", $sensor_event->__get_property('timestamp');
    my $accel = $sensor_event->__get_property('values');
    printf ">>Values:    (%02f, %02f, %02f)\n", @$accel;
    print "\n";
    ## done reading, re-register.
    $sensor_manager->registerListener($accel_listener, $accel_sensor, 10*1000000);
  } else {
    say "Called method: $method_long_name";
#    die;
  }

};

#my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();
# 16265 >>> ["call","23625056","class_call_handler",0,"call","uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash","can","get_activity"]             |
# 16265 >>> ["call","23616824","class_call_handler",0,"call","uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor","can","make_instance"]           |
$accel_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.hardware.SensorEventListener', $accel_event_callback);

# 1000000 microseconds = 1 second
$sensor_manager->registerListener($accel_listener, $accel_sensor, 10*1000000);

# Object::Remote::MiniLoop
my $or_loop = Object::Remote->current_loop;
$or_loop->want_run;
$or_loop->run_while_wanted;



