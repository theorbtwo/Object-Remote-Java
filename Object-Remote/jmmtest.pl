#!/usr/bin/perl
use strictures 1;
use Object::Remote;

# FIXME: Should Connection.pm use a plugin-like loader to load Object::Remote::Connector::*?
use Object::Remote::Connector::TCP;

my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $properties = java::lang::System->can::on($conn, 'getProperties')->();

# java.lang.System.getProperties();

print "Classpath: ", $properties->getProperty('java.class.path'), "\n";
