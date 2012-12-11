#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use Time::HiRes 'sleep';

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

# http://developer.android.com/reference/android/location/LocationManager.html
my $loc_man = $activity->getSystemService('location');

sub nmea_event {
  my ($invocation_handler, $this, $method_info, $args) = @_;
  #my ($timestamp, $nmea) = @$args;
  my ($method, $decl_class, $short_name, $long_name) = @$method_info;

  print STDERR "nmea: $long_name\n";
  if ($long_name eq 'FOO') {
  } else {
    print "Unknown method called on nmea_event: $long_name\n";
    exit;
  }
}

my $nmea_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.location.GpsStatus$NmeaListener', \&nmea_event);
$loc_man->addNmeaListener($nmea_listener);

sub loc_event {
  my ($invocation_handler, $this, $method_info, $args) = @_;
  #my ($timestamp, $nmea) = @$args;
  my ($method, $decl_class, $short_name, $long_name) = @$method_info;

  print STDERR "loc_event: $long_name\n";
  if ($long_name eq 'FOO') {
  } else {
    print "Unknown method called on location_event: $long_name\n";
    exit;
  }
}

my $loc_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.location.LocationListener', \&loc_event);

my $gps_provider = $loc_man->getProvider('gps');
my $last_known_loc = $loc_man->getLastKnownLocation('gps');

sub loc_to_string {
  if (!$_[0]) {
    return "undef";
  }
  return "(". join(", ", $_[0]->getLatitude, $_[0]->getLongitude, $_[0]->getAltitude) .")";
}

print "Last_known_loc: ", loc_to_string($last_known_loc), "\n";

$loc_man->requestLocationUpdates('gps', # provider
                                 1000,  # long  - once a second.  Note that unlike sensors, this is the *most* often we will get updates.
                                 0,     # float - *and* we have moved at least 0 meters
                                 $loc_listener);

$loc_man->requestLocationUpdates('network',
                                 1000,
                                 0,
                                 $loc_listener);
                                 
END {
  $loc_man->removeUpdates($loc_listener);
  $loc_man->removeNmeaListener($nmea_listener);
}

$SIG{INT} = sub {
  # END blocks should run on C-c.
  exit;
};


# Object::Remote::MiniLoop
my $or_loop = Object::Remote->current_loop;
$or_loop->want_run;
$or_loop->run_while_wanted;
