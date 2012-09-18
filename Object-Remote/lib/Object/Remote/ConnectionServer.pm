package Object::Remote::ConnectionServer;

use Scalar::Util qw(blessed weaken);
use Module::Runtime qw(use_module);
use Object::Remote;
use IO::Socket::UNIX;
use POSIX ();
use Moo;

has listen_on => (
  is => 'ro',
  coerce => sub {
    return $_[0] if blessed($_[0]);
    unlink($_[0]);
    IO::Socket::UNIX->new(
      Local => $_[0],
      Listen => 1
    ) or die "Couldn't liten to $_[0]: $!";
  },
  trigger => sub {
    my ($self, $fh) = @_;
    weaken($self);
    Object::Remote->current_loop
                  ->watch_io(
                      handle => $fh,
                      on_read_ready => sub { $self->_listen_ready($fh) }
                    );
  },
);

has connection_args => (
 is => 'ro', default => sub { [] }
);

has connection_callback => (
  is => 'ro', default => sub { sub { shift } }
);

sub BUILD {
  Object::Remote->current_loop->want_run;
}

sub run {
  Object::Remote->current_loop->run_while_wanted;
}

sub _listen_ready {
  my ($self, $fh) = @_;
  my $new = $fh->accept or die "Couldn't accept: $!";
  $new->blocking(0);
  my $f = CPS::Future->new;
  my $c = use_module('Object::Remote::Connection')->new(
    receive_from_fh => $new,
    send_to_fh => $new,
    on_close => $f, # and so will die $c
    @{$self->connection_args}
  )->${\$self->connection_callback};
  $f->on_ready(sub { undef($c) });
  $c->ready_future->done;
  print $new "Shere\n" or die "Couldn't send to new socket: $!";
  return $c;
}

sub DEMOLISH {
  my ($self, $gd) = @_;
  return if $gd;
  Object::Remote->current_loop
                ->unwatch_io(
                    handle => $self->listen_on,
                    on_read_ready => 1
                  );
  if ($self->listen_on->can('hostpath')) {
    unlink($self->listen_on->hostpath);
  }
  Object::Remote->current_loop->want_stop;
}

1;
