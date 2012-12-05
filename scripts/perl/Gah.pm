package Gah;
use strictures 1;

my $callback;

sub new {
  return bless {}, shift;
}

sub remember_callback {
  shift;
  $callback = shift;
}

sub run_callback {
  $callback->();
}

sub marker {
}

'Aaargh!';

