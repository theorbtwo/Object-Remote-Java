#!/usr/bin/perl
use strictures 1;
use Object::Remote;

# FIXME: Should Connection.pm use a plugin-like loader to load Object::Remote::Connector::*?
use Object::Remote::Connector::TCP;

my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $service = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_service')->();
## There are constants for these values but we don't support constants yet..
my $sensor_manager = $service->getSystemService("sensor");
my $accel = $sensor_manager->getDefaultSensor(1);

my $max_range = $accel->getMaximumRange() ||0;
print "Max range is: $max_range\n";

my $power = $accel->getPower();
print "Power used: $power\n";

