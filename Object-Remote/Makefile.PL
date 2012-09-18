use strict;
use warnings FATAL => 'all';
use ExtUtils::MakeMaker;

#(do 'maint/Makefile.PL.include' or die $@) unless -f 'META.yml';

WriteMakefile(
  NAME => 'Object::Remote',
  VERSION_FROM => 'lib/Object/Remote.pm',
  PREREQ_PM => {
    Moo => 0,
    'Module::Runtime' => 0,
    'JSON::PP' => 0,
    'CPS::Future' => 0,
    'Class::C3' => 0, # required to fatpack Moo
    'IO::Prompter' => 0,
  },
  EXE_FILES => [
    'bin/object-remote-node',
    'bin/object-remote-slave',
    'bin/remoterepl',
  ],
);
