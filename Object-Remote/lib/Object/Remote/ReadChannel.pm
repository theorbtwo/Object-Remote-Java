package Object::Remote::ReadChannel;

use CPS::Future;
use Scalar::Util qw(weaken);
use Moo;

has fh => (
  is => 'ro', required => 1,
  trigger => sub {
    my ($self, $fh) = @_;
    weaken($self);
    Object::Remote->current_loop
                  ->watch_io(
                      handle => $fh,
                      on_read_ready => sub { $self->_receive_data_from($fh) }
                    );
  },
);

has on_close_call => (
  is => 'rw', default => sub { sub {} },
);

has on_line_call => (is => 'rw');

has _receive_data_buffer => (is => 'ro', default => sub { my $x = ''; \$x });

sub _receive_data_from {
  my ($self, $fh) = @_;
  my $rb = $self->_receive_data_buffer;
  my $len = sysread($fh, $$rb, 1024, length($$rb));
  my $err = defined($len) ? '' : ": $!";
  if (defined($len) and $len > 0) {
    while (my $cb = $self->on_line_call and $$rb =~ s/^(.*)\n//) {
      $cb->(my $line = $1);
    }
  } else {
    Object::Remote->current_loop
                  ->unwatch_io(
                      handle => $self->fh,
                      on_read_ready => 1
                    );
    $self->on_close_call->($err);
  }
}

sub DEMOLISH {
  my ($self, $gd) = @_;
  return if $gd;
  Object::Remote->current_loop
                ->unwatch_io(
                    handle => $self->fh,
                    on_read_ready => 1
                  );
}

1;
