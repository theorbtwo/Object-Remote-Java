#!/usr/bin/perl
use strictures 1;
use Object::Remote;

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $service = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_service')->();
my $wifi_manager = $service->getSystemService("wifi");
# http://developer.android.com/reference/android/net/wifi/WifiManager.html
while (1) {
  print scalar localtime, "\n";
  my $con_info = $wifi_manager->getConnectionInfo;
  print "BSSID: ", $con_info->getBSSID, "\n";
  print "Link speed: ", $con_info->getLinkSpeed, "Mbit/sec\n";
  my $rssi = $con_info->getRssi();
  print "RSSI: ", $rssi, " (dBm?)\n";
  my $level = $wifi_manager->calculateSignalLevel($rssi, 100);
  print "Signal strength: $level / 100\n";
  sleep 30;
}

