package Some::Class;

sub new {
    my ($class) = @_;

    bless {foo => 0xCAFEbabe}, $class;
}

'this sentance is false';
