package Object::Remote::Node;

use strictures 1;
use Object::Remote::Connector::STDIO;
use Object::Remote;
use CPS::Future;

sub run {
  my $c = Object::Remote::Connector::STDIO->new->connect;

  $c->register_class_call_handler;

  my $loop = Object::Remote->current_loop;

  $c->on_close->on_ready(sub { $loop->want_stop });

  print { $c->send_to_fh } "Shere\n";

  $loop->want_run;
  $loop->run_while_wanted;
}

1;
