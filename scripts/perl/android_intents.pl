#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use Time::HiRes 'sleep';

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

# my $edit_text = android::widget::EditText->new::on($conn, $activity);
# $editText->setLines(20);
# my $button = android::widget::Button->new::on($conn, $activity);
# $button->setText("");
# my $layout = android::widget::LinearLayout->new::on($conn, $activity);
# $layout->addView($edit_text);
# $layout->addView($button);

my $package_manager = $activity->getPackageManager;

my $package_infos = $package_manager->getInstalledPackages(0)->toArray();
for my $package_info (@$package_infos) {
  print "Package: ", $package_info->__get_property('packageName'), "\n";
  my $package_name = $package_info->__get_property('packageName');
}

# # A new, empty, filter, which hopefully matches everything.
# my $filter = android::content::IntentFilter->new::on($conn);
