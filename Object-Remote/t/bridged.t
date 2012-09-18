use strictures 1;
use Test::More;
use Test::Fatal;
use FindBin;

use lib "$FindBin::Bin/lib";

use Object::Remote;

is exception {
  my $bridge = ORTestBridge->new::on('-'); #'localhost');
  is $bridge->call('counter'), 0;
  $bridge->call('increment');
  is $bridge->call('counter'), 1;
}, undef, 'no error during bridge access';

done_testing;
