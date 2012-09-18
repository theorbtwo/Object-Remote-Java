package Object::Remote::FromData;

use strictures 1;
use Object::Remote;

our %Modules;
our %Not_Loaded_Yet;
our %Seen;

sub import {
  my $target = caller;
  return if $Seen{$target};
  $Seen{$target} = $Not_Loaded_Yet{$target} = 1;
}

sub flush_loaded {
  foreach my $key (keys %Not_Loaded_Yet) {
    my $data_fh = do { no strict 'refs'; *{"${key}::DATA"} };
    my $data = do { local $/; <$data_fh> };
    my %modules = reverse(
      $data =~ m/(^package ([^;]+);\n.*?(?:(?=^package)|\Z))/msg
    );
    $_ .= "\n1;\n" for values %modules;
    @Modules{keys %modules} = values %modules;
    delete $Not_Loaded_Yet{$key};
  }
}

sub find_module {
  flush_loaded;
  my ($module) = @_;
  $module =~ s/\//::/g;
  $module =~ s/\.pm$//;
  return $Modules{$module};
}

1;
