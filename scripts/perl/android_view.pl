#!/usr/bin/perl
use strictures 1;
use Object::Remote;

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $service = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_service')->();
my $activity = android::app::Activity->new::on($conn);
$activity->setTitle("JB Test");

my $view = android::widget::EditText->new::on($conn, $service);

$activity->setContentView($view);

