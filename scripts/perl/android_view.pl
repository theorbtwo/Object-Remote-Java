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

my $old_config = $activity->getResources->getConfiguration;

dump_config("orig config: ", $old_config);

$activity->set_on_configuration_changed_callback(sub {
                                                   print "Your configuration changed, and we actually managed to get here!\n";
                                                 });

# 21:29 <@mst> $helper->callMethodOnUiThreadOf($activity, setContentView => $view);
my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$layout]);
$activity->runOnUiThread($runnable);

# Object::Remote::MiniLoop
my $or_loop = Object::Remote->current_loop;
$or_loop->want_run;
$or_loop->run_while_wanted;

sub dump_config {
  my ($prefix, $config) = @_;

  for my $field (qw<fontScale hardKeyboardHidden keyboard keyboardHidden locale mcc mnc navigation navigationHidden orientation screenLayout touchscreen uiMode>) {
    my $val = $config->__get_property($field);
    print "$prefix$field: $val\n"
  }
}

