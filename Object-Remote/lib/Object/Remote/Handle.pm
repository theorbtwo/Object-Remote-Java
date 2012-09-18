package Object::Remote::Handle;

use Object::Remote::Proxy;
use Scalar::Util qw(weaken blessed);
use Object::Remote::Future;
use Module::Runtime qw(use_module);
use Moo;

has connection => (
  is => 'ro', required => 1,
  coerce => sub {
    blessed($_[0])
      ? $_[0]
      : use_module('Object::Remote::Connection')->new_from_spec($_[0])
  },
);

has id => (is => 'rwp');

has disarmed_free => (is => 'rwp');

sub disarm_free { $_[0]->_set_disarmed_free(1); $_[0] }

sub proxy {
  bless({ remote => $_[0], method => 'call' }, 'Object::Remote::Proxy');
}

sub BUILD {
  my ($self, $args) = @_;
  if ($self->id) {
    $self->disarm_free;
  } else {
    die "No id supplied and no class either" unless $args->{class};
    ref($_) eq 'HASH' and $_ = [ %$_ ] for $args->{args};
    $self->_set_id(
      await_future(
        $self->connection->send_class_call(
          0, $args->{class},
          $args->{constructor}||'new', @{$args->{args}||[]}
        )
      )->{remote}->disarm_free->id
    );
  }
  $self->connection->register_remote($self);
}

sub call {
  my ($self, $method, @args) = @_;
  my $w = wantarray;
  $method = "start::${method}" if (caller(0)||'') eq 'start';
  future {
    $self->connection->send(call => $self->id, $w, $method, @args)
  };
}

sub call_discard {
  my ($self, $method, @args) = @_;
  $self->connection->send_discard(call => $self->id, $method, @args);
}

sub call_discard_free {
  my ($self, $method, @args) = @_;
  $self->disarm_free;
  $self->connection->send_discard(call_free => $self->id, $method, @args);
}

sub DEMOLISH {
  my ($self, $gd) = @_;
  return if $gd or $self->disarmed_free;
  $self->connection->send_free($self->id);
}

1;
