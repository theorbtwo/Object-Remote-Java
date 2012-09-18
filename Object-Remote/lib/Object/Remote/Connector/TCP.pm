package Object::Remote::Connector::TCP;

use IO::Socket::INET;
use Moo;

with 'Object::Remote::Role::Connector';

has host => (is => 'ro', required => 1);
has port => (is => 'ro', required => 1);
has sock => (is => 'rw');

{
  no warnings 'once';
  
  push @Object::Remote::Connection::Guess, sub {
    for ($_[0]) {
      # FIXME: Use Regexp::Common regex for a hostname, or check this against spec.
      print STDERR "tcp guess for $_\n";
      if (defined and !ref and m!^tcp://([a-z0-9.]+):(\d+)/!i) {
	return __PACKAGE__->new(host => $1, port => $2);
      }
    }
    return;
  };
}

sub _open2_for {
  my ($self) = @_;
  my $sock = IO::Socket::INET->new(PeerHost => $self->host, PeerPort => $self->port)
    or die "Couldn't open connection: $!";
  
  ($sock, $sock, undef);
}
