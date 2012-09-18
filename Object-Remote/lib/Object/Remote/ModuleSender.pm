package Object::Remote::ModuleSender;

use Config;
use File::Spec;
use List::Util qw(first);
use Moo;

has dir_list => (is => 'lazy');

sub _build_dir_list {
  my %core = map +($_ => 1), grep $_, @Config{
    qw(privlibexp archlibexp vendorarchexp sitearchexp)
  };
  [ grep !/\Q$Config{archname}/, grep !/\Q$Config{myarchname}/, grep !$core{$_}, @INC ];
}

sub source_for {
  my ($self, $module) = @_;
  if (my $find = Object::Remote::FromData->can('find_module')) {
    if (my $source = $find->($module)) {
      return $source;
    }
  }
  my ($found) = first {  -f $_ }
                  map File::Spec->catfile($_, $module),
                    @{$self->dir_list};
  die "Couldn't find ${module} in remote \@INC. dir_list contains:\n"
      .join("\n", @{$self->dir_list})
    unless $found;
  open my $fh, '<', $found or die "Couldn't open ${found} for ${module}: $!";
  return do { local $/; <$fh> };
}

1;
