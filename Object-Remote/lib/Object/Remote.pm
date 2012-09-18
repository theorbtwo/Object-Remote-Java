package Object::Remote;

use Object::Remote::MiniLoop;
use Object::Remote::Handle;
use Module::Runtime qw(use_module);

our $VERSION = '0.002003'; # 0.2.3

sub new::on {
  my ($class, $on, @args) = @_;
  my $conn = __PACKAGE__->connect($on);
  return $conn->remote_object(class => $class, args => \@args);
}

sub can::on {
  my ($class, $on, $name) = @_;
  my $conn = __PACKAGE__->connect($on);
  return $conn->remote_sub(join('::', $class, $name));
}

sub new {
  shift;
  Object::Remote::Handle->new(@_)->proxy;
}

sub connect {
  my ($class, $to) = @_;
  use_module('Object::Remote::Connection')->maybe::start::new_from_spec($to);
}

sub current_loop {
  our $Current_Loop ||= Object::Remote::MiniLoop->new
}

1;

=head1 NAME

Object::Remote - Call methods on objects in other processes or on other hosts

=head1 SYNOPSIS

Creating a connection:

  use Object::Remote;

  my $conn = Object::Remote->connect('myserver'); # invokes ssh

Calling a subroutine:

  my $capture = IPC::System::Simple->can::on($conn, 'capture');

  warn $capture->('uptime');

Using an object:

  my $eval = Eval::WithLexicals->new::on($conn);

  $eval->eval(q{my $x = `uptime`});

  warn $eval->eval(q{$x});

Importantly: 'myserver' only requires perl 5.8+ - no non-core modules need to
be installed on the far side, Object::Remote takes care of it for you!

=head1 DESCRIPTION

Object::Remote allows you to create an object in another process - usually
one running on another machine you can connect to via ssh, although there
are other connection mechanisms available.

The idea here is that in many cases one wants to be able to run a piece of
code on another machine, or perhaps many other machines - but without having
to install anything on the far side.

=head1 COMPONENTS

=head2 Object::Remote

The "main" API, which provides the L</connect> method to create a connection
to a remote process/host, L</new::on> to create an object on a connection,
and L</can::on> to retrieve a subref over a connection.

=head2 Object::Remote::Connection

The object representing a connection, which provides the
L<Object::Remote::Connection/remote_object> and
L<Object::Remote::Connection/remote_sub> methods that are used by
L</new::on> and L</can::on> to return proxies for objects and subroutines
on the far side.

=head2 Object::Remote::Future

Code for dealing with asynchronous operations, which provides the
L<Object::Remote::Future/start::method> syntax for calling a possibly
asynchronous method without blocking, and
L<Object::Remote::Future/await_future> and L<Object::Remote::Future/await_all>
to block until an asynchronous call completes or fails.

=head1 METHODS

=head2 connect

  my $conn = Object::Remote->connect('-'); # fork()ed connection

  my $conn = Object::Remote->connect('myserver'); # connection over ssh

  my $conn = Object::Remote->connect('user@myserver'); # connection over ssh

  my $conn = Object::Remote->connect('root@'); # connection over sudo

=head2 new::on

  my $eval = Eval::WithLexicals->new::on($conn);

  my $eval = Eval::WithLexicals->new::on('myserver'); # implicit connect

  my $obj = Some::Class->new::on($conn, %args); # with constructor arguments

=head2 can::on

  my $hostname = Sys::Hostname->can::on($conn, 'hostname');

  my $hostname = Sys::Hostname->can::on('myserver', 'hostname');

=head1 SUPPORT

IRC: #web-simple on irc.perl.org

=head1 AUTHOR

mst - Matt S. Trout (cpan:MSTROUT) <mst@shadowcat.co.uk>

=head1 CONTRIBUTORS

phaylon - Robert Sedlacek (cpan:PHAYLON) <r.sedlacek@shadowcat.co.uk>

=head1 SPONSORS

Parts of this code were paid for by

  Socialflow L<http://www.socialflow.com>

  Shadowcat Systems L<http://www.shadow.cat>

=head1 COPYRIGHT

Copyright (c) 2012 the Object::Remote L</AUTHOR>, L</CONTRIBUTORS> and
L</SPONSORS> as listed above.

=head1 LICENSE

This library is free software and may be distributed under the same terms
as perl itself.

=cut
