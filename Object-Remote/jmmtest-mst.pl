#!/usr/bin/perl
use strictures 1;
use Object::Remote;

# FIXME: Should Connection.pm use a plugin-like loader to load Object::Remote::Connector::*?
use Object::Remote::Connector::TCP;

my $conn = Object::Remote->connect('-');
my $test_class = Some::Class->new::on($conn);

