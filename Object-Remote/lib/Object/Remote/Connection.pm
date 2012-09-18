package Object::Remote::Connection;

use Object::Remote::Future;
use Object::Remote::Null;
use Object::Remote::Handle;
use Object::Remote::CodeContainer;
use Object::Remote::GlobProxy;
use Object::Remote::GlobContainer;
use Object::Remote;
use Symbol;
use IO::Handle;
use Module::Runtime qw(use_module);
use Scalar::Util qw(weaken blessed refaddr);
use JSON::PP qw(encode_json);
use Moo;

our $DEBUG = !!$ENV{OBJECT_REMOTE_DEBUG};

has send_to_fh => (
  is => 'ro', required => 1,
  trigger => sub { $_[1]->autoflush(1) },
);

has read_channel => (
  is => 'ro', required => 1,
  trigger => sub {
    my ($self, $ch) = @_;
    weaken($self);
    $ch->on_line_call(sub { $self->_receive(@_) });
    $ch->on_close_call(sub { $self->on_close->done(@_) });
  },
);

has on_close => (
  is => 'ro', default => sub { CPS::Future->new },
  trigger => sub { 
    my ($self, $f) = @_;
    weaken($self);
    $f->on_done(sub {
      $self->_fail_outstanding("Connection lost: ".($f->get)[0]);
    });
  }
);

has child_pid => (is => 'ro');

has local_objects_by_id => (
  is => 'ro', default => sub { {} },
  coerce => sub { +{ %{$_[0]} } }, # shallow clone on the way in
);

has remote_objects_by_id => (
  is => 'ro', default => sub { {} },
  coerce => sub { +{ %{$_[0]} } }, # shallow clone on the way in
);

has outstanding_futures => (is => 'ro', default => sub { {} });

sub _fail_outstanding {
  my ($self, $error) = @_;
  my $outstanding = $self->outstanding_futures;
  $_->fail($error) for values %$outstanding;
  %$outstanding = ();
  return;
}

has _json => (
  is => 'lazy',
  handles => {
    _deserialize => 'decode',
    _encode => 'encode',
  },
);

sub _id_to_remote_object {
  my ($self, $id) = @_;
  return bless({}, 'Object::Remote::Null') if $id eq 'NULL';
  (
    $self->remote_objects_by_id->{$id}
    or Object::Remote::Handle->new(connection => $self, id => $id)
  )->proxy;
}

sub _build__json {
  weaken(my $self = shift);
  JSON::PP->new->filter_json_single_key_object(
    __remote_object__ => sub {
      $self->_id_to_remote_object(@_);
    }
  )->filter_json_single_key_object(
    __remote_code__ => sub {
      my $code_container = $self->_id_to_remote_object(@_);
      sub { $code_container->call(@_) };
    }
  )->filter_json_single_key_object(
    __scalar_ref__ => sub {
      my $value = shift;
      return \$value;
    }
  )->filter_json_single_key_object(
    __glob_ref__ => sub {
      my $glob_container = $self->_id_to_remote_object(@_);
      my $handle = Symbol::gensym;
      tie *$handle, 'Object::Remote::GlobProxy', $glob_container;
      return $handle;
    }
  );
}

BEGIN {
  unshift our @Guess, sub { blessed($_[0]) ? $_[0] : undef };
  eval { require Object::Remote::Connector::Local };
  eval { require Object::Remote::Connector::LocalSudo };
  eval { require Object::Remote::Connector::SSH };
  eval { require Object::Remote::Connector::UNIX };
}

sub new_from_spec {
  my ($class, $spec) = @_;
  return $spec if blessed $spec;
  foreach my $poss (do { our @Guess }) {
    if (my $conn = $poss->($spec)) {
      return $conn->maybe::start::connect;
    }
  }
  die "Couldn't figure out what to do with ${spec}";
}

sub remote_object {
  my ($self, @args) = @_;
  Object::Remote::Handle->new(
    connection => $self, @args
  )->proxy;
}

sub connect {
  my ($self, $to) = @_;
  return await_future(
    $self->send_class_call(0, 'Object::Remote', connect => $to)
  );
}

sub remote_sub {
  my ($self, $sub) = @_;
  my ($pkg, $name) = $sub =~ m/^(.*)::([^:]+)$/;
  return await_future($self->send_class_call(0, $pkg, can => $name));
}

sub send_class_call {
  my ($self, $ctx, @call) = @_;
  $self->send(call => class_call_handler => $ctx => call => @call);
}

sub register_class_call_handler {
  my ($self) = @_;
  $self->local_objects_by_id->{'class_call_handler'} ||= do {
    my $o = $self->new_class_call_handler;
    $self->_local_object_to_id($o);
    $o;
  };
}

sub new_class_call_handler {
  Object::Remote::CodeContainer->new(
    code => sub {
      my ($class, $method) = (shift, shift);
      use_module($class)->$method(@_);
    }
  );
}

sub register_remote {
  my ($self, $remote) = @_;
  weaken($self->remote_objects_by_id->{$remote->id} = $remote);
  return $remote;
}

sub send_free {
  my ($self, $id) = @_;
  delete $self->remote_objects_by_id->{$id};
  $self->_send([ free => $id ]);
}

sub send {
  my ($self, $type, @call) = @_;

  my $future = CPS::Future->new;
  my $remote = $self->remote_objects_by_id->{$call[0]};

  unshift @call, $type => $self->_local_object_to_id($future);

  my $outstanding = $self->outstanding_futures;
  $outstanding->{$future} = $future;
  $future->on_ready(sub {
    undef($remote);
    delete $outstanding->{$future}
  });

  $self->_send(\@call);

  return $future;
}

sub send_discard {
  my ($self, $type, @call) = @_;

  unshift @call, $type => 'NULL';

  $self->_send(\@call);
}

sub _send {
  my ($self, $to_send) = @_;

  print { $self->send_to_fh } $self->_serialize($to_send)."\n";
}

sub _serialize {
  my ($self, $data) = @_;
  local our @New_Ids = (-1);
  return eval {
    my $flat = $self->_encode($self->_deobjectify($data));
    warn "$$ >>> ${flat}\n" if $DEBUG;
    $flat;
  } || do {
    my $err = $@; # won't get here if the eval doesn't die
    # don't keep refs to new things
    delete @{$self->local_objects_by_id}{@New_Ids};
    die "Error serializing: $err";
  };
}

sub _local_object_to_id {
  my ($self, $object) = @_;
  my $id = refaddr($object);
  $self->local_objects_by_id->{$id} ||= do {
    push our(@New_Ids), $id if @New_Ids;
    $object;
  };
  return $id;
}

sub _deobjectify {
  my ($self, $data) = @_;
  if (blessed($data)) {
    return +{ __remote_object__ => $self->_local_object_to_id($data) };
  } elsif (my $ref = ref($data)) {
    if ($ref eq 'HASH') {
      return +{ map +($_ => $self->_deobjectify($data->{$_})), keys %$data };
    } elsif ($ref eq 'ARRAY') {
      return [ map $self->_deobjectify($_), @$data ];
    } elsif ($ref eq 'CODE') {
      my $id = $self->_local_object_to_id(
                 Object::Remote::CodeContainer->new(code => $data)
               );
      return +{ __remote_code__ => $id };
    } elsif ($ref eq 'SCALAR') {
      return +{ __scalar_ref__ => $$data };
    } elsif ($ref eq 'GLOB') {
      return +{ __glob_ref__ => $self->_local_object_to_id(
        Object::Remote::GlobContainer->new(handle => $data)
      ) };
    } else {
      die "Can't collapse reftype $ref";
    }
  }
  return $data; # plain scalar
}

sub _receive {
  my ($self, $flat) = @_;
  warn "$$ <<< $flat\n" if $DEBUG;
  my ($type, @rest) = eval { @{$self->_deserialize($flat)} }
    or do { warn "Deserialize failed for ${flat}: $@"; return };
  eval { $self->${\"receive_${type}"}(@rest); 1 }
    or do { warn "Receive failed for ${flat}: $@"; return };
  return;
}

sub receive_free {
  my ($self, $id) = @_;
  delete $self->local_objects_by_id->{$id}
    or warn "Free: no such object $id";
  return;
}

sub receive_call {
  my ($self, $future_id, $id, @rest) = @_;
  my $future = $self->_id_to_remote_object($future_id);
  $future->{method} = 'call_discard_free';
  my $local = $self->local_objects_by_id->{$id}
    or do { $future->fail("No such object $id"); return };
  $self->_invoke($future, $local, @rest);
}

sub receive_call_free {
  my ($self, $future, $id, @rest) = @_;
  $self->receive_call($future, $id, undef, @rest);
  $self->receive_free($id);
}

sub _invoke {
  my ($self, $future, $local, $ctx, $method, @args) = @_;
  if ($method =~ /^start::/) {
    my $f = $local->$method(@args);
    $f->on_done(sub { undef($f); $future->done(@_) });
    return unless $f;
    $f->on_fail(sub { undef($f); $future->fail(@_) });
    return;
  }
  my $do = sub { $local->$method(@args) };
  eval {
    $future->done(
      defined($ctx)
        ? ($ctx ? $do->() : scalar($do->()))
        : do { $do->(); () }
    );
    1;
  } or do { $future->fail($@); return; };
  return;
}

1;

=head1 NAME

Object::Remote::Connection - An underlying connection for L<Object::Remote>

=head1 LAME

Shipping prioritised over writing this part up. Blame mst.

=cut
