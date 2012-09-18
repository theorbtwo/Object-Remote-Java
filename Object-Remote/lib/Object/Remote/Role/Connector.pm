package Object::Remote::Role::Connector;

use Module::Runtime qw(use_module);
use Object::Remote::Future;
use Moo::Role;

requires '_open2_for';

has timeout => (is => 'ro', default => sub { { after => 10 } });

sub connect {
  my $self = shift;
  my ($send_to_fh, $receive_from_fh, $child_pid) = $self->_open2_for(@_);
  my $channel = use_module('Object::Remote::ReadChannel')->new(
    fh => $receive_from_fh
  );
  return future {
    my $f = shift;
    $channel->on_line_call(sub {
      if ($_[0] eq "Shere") {
        $f->done(
          use_module('Object::Remote::Connection')->new(
            send_to_fh => $send_to_fh,
            read_channel => $channel,
            child_pid => $child_pid,
          )
        );
      } else {
        $f->fail("Expected Shere from remote but received: $_[0]");
      }
      undef($channel);
    });
    $channel->on_close_call(sub {
      $f->fail("Channel closed without seeing Shere: $_[0]");
      undef($channel);
    });
    Object::Remote->current_loop
                  ->watch_time(
                      %{$self->timeout},
                      code => sub {
                        $f->fail("Connection timed out") unless $f->is_ready;
                        undef($channel);
                      }
                    );
    $f;
  }
}

1;
