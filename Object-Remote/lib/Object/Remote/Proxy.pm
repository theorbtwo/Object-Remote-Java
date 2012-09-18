package Object::Remote::Proxy;

use strictures 1;

sub AUTOLOAD {
  my $self = shift;
  (my $method) = (our $AUTOLOAD =~ /([^:]+)$/);
  my $to_fire = $self->{method};
  if ((caller(0)||'') eq 'start') {
    $to_fire = "start::${to_fire}";
  }
  $self->{remote}->$to_fire($method => @_);
}

sub DESTROY { }

1;
