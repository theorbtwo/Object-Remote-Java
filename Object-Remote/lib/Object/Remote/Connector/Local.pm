package Object::Remote::Connector::Local;

use Moo;

with 'Object::Remote::Role::Connector::PerlInterpreter';

no warnings 'once';

push @Object::Remote::Connection::Guess, sub {
  if (($_[0]||'') eq '-') { __PACKAGE__->new }
};

1;
