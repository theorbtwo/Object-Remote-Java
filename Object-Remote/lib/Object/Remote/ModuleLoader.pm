package Object::Remote::ModuleLoader;

BEGIN {
  package Object::Remote::ModuleLoader::Hook;
  use Moo;
  has sender => (is => 'ro', required => 1);

  # unqualified INC forced into package main
  sub Object::Remote::ModuleLoader::Hook::INC {
    my ($self, $module) = @_;
    if (my $code = $self->sender->source_for($module)) {
      open my $fh, '<', \$code;
      return $fh;
    }
    return;
  }
}

use Moo;

has module_sender => (is => 'ro', required => 1);

has inc_hook => (is => 'lazy');

sub _build_inc_hook {
  my ($self) = @_;
  Object::Remote::ModuleLoader::Hook->new(sender => $self->module_sender);
}

sub BUILD { shift->enable }

sub enable {
  push @INC, shift->inc_hook;
  return;
}

sub disable {
  my ($self) = @_;
  my $hook = $self->inc_hook;
  @INC = grep $_ ne $hook, @INC;
  return;
}

sub DEMOLISH { $_[0]->disable unless $_[1] }

1;
