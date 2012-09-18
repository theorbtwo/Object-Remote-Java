#!/usr/bin/perl
use strictures 1;
use IO::Socket::INET;
use JSON;
use Data::Dump::Streamer 'Dump';
use 5.10.0;
$|=1;

my $listen_sock = IO::Socket::INET->new(
					Listen => 1,
				       ) or die;
say "Waiting for connection on port ", $listen_sock->sockport;

my $sock = $listen_sock->accept;
print "Connected\n";

$sock->print("Shere\n");

while (my $line = <$sock>) {
  chomp $line;
  print "Server got $line\n";
  my $message = decode_json($line);

  my $api_kind = shift @$message;
  my $future_objid = shift @$message;
  my $this_objid = shift @$message;
  my $wantarray = shift @$message;

  my $out_message = [];

  if ($api_kind eq 'call' and $this_objid eq 'class_call_handler') {
    my $class_call_kind = shift @$message;
    my $class = shift @$message;
    my $method = shift @$message;
    my @args = shift @$message;
    
    my $ret;
    
    if ($wantarray == 0) {
      $ret = $class->$method(@args);
    } elsif ($wantarray == 1) {
      $ret = [$class->$method(@args)];
    }
    
    $out_message = ['call_free', 'NULL', $future_objid, 'done', $ret];
  } else {
    die "Don't know api kind $api_kind";
  }

  my $out = encode_json($out_message);
  $out =~ s/\n//;

  print "OUT: $out\n";
  $sock->print($out, "\n");
}

package Some::Class;
use strictures 1;

sub new {
  my ($class) = @_;

  return 1234;
}
