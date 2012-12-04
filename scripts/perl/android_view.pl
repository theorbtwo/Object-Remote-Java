#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use Time::HiRes 'sleep';

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();
# my $activity = android::app::Activity->new::on($conn);
# $activity->setTitle("JB Test");

my $edit_text = android::widget::EditText->new::on($conn, $activity);
my $button = android::widget::Button->new::on($conn, $activity);
$button->setText("DO IT!");
my $layout = android::widget::LinearLayout->new::on($conn, $activity);
$layout->addView($edit_text);
$layout->addView($button);

$activity->set_on_configuration_changed_callback(sub {
                                                   print "Your configuration changed, and we actually managed to get here!\n";
                                                 });

# 21:29 <@mst> $helper->callMethodOnUiThreadOf($activity, setContentView => $view);
my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$layout]);
$activity->runOnUiThread($runnable);

while (1) {
  # this is horrible.
  sleep 0.1;
}

#$activity->runOnUiThread(sub{ $activity->setContentView($view) });
