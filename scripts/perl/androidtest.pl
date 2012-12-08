#!/usr/bin/perl
use strictures 1;
use Object::Remote;

# FIXME: Should Connection.pm use a plugin-like loader to load Object::Remote::Connector::*?
use Object::Remote::Connector::TCP;

my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

my $toast_make_text = android::widget::Toast->can::on($conn, 'makeText');
#my $toast = android::widget::Toast->$toast_make_text($activity,
#                                                    "toast content",
#                                                    0
#                                                   );
my $toast = $toast_make_text->($activity,
                               "toast content",
                               0
                              );

$toast->show();

# java.lang.System.getProperties();

#print "Classpath: ", $properties->getProperty('java.class.path'), "\n";
