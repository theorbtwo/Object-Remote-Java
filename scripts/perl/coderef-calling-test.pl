#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use lib '/mnt/shared/projects/android/java-bridge/scripts/perl/';
use Gah;

my $conn = Object::Remote->connect('-');

my $g = Gah->new::on($conn);
$g->marker();
$g->remember_callback(sub {print "$$ --- This is the callback\n"});
$g->run_callback();
