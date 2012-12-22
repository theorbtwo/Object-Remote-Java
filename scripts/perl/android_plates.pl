#!/usr/bin/perl
use strictures 1;
use Object::Remote;
use Time::HiRes 'sleep';
use 5.10.0;

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.shift.'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

my $overall_layout = android::widget::LinearLayout->new::on($conn, $activity);
$overall_layout->setOrientation($overall_layout->__get_property('VERTICAL'));

my $edit_text = android::widget::EditText->new::on($conn, $activity);
$overall_layout->addView($edit_text, -1, -2);

#$edit_text->setHint() -- wants a resid.
# how many lines are displayed at once
$edit_text->setLines(1);
# how many lines a user is allowed to type in
$edit_text->setMaxLines(1);

my $go_button = android::widget::Button->new::on($conn, $activity);
$overall_layout->addView($go_button, -1, -2);
$go_button->setText("Go");

my $show_text;

my $go_button_cb = sub {
  my ($invocation_handler, $this, $method_info, $args) = @_;
  my ($method, $decl_class, $short_name, $long_name) = @$method_info;

  if ($long_name eq 'public abstract void android.view.View$OnClickListener.onClick(android.view.View)') {
    my ($view) = @$args;
    my $text = $edit_text->getText->toString;

    my $results = parse_plate($text);
    
    $activity->runOnUiThread(uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $show_text, 'setText', [$results]));
  } else {
    die "Unknown method on go button: $long_name";
  }
};

my $button_event_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.view.View$OnClickListener', $go_button_cb);
$go_button->setOnClickListener($button_event_listener);

$show_text = android::widget::TextView->new::on($conn, $activity);
$overall_layout->addView($show_text, -1, -1);
$show_text->setText("Type in a number plate above, and I'll tell you about it here");



my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$overall_layout]);
$activity->runOnUiThread($runnable);

# Object::Remote::MiniLoop
my $or_loop = Object::Remote->current_loop;
$or_loop->want_run;
$or_loop->run_while_wanted;

sub parse_plate {
  my ($text) = @_;
  my $output = '';

  $text =~ s/ //g;
  $text = uc $text;

  $output .= "Normalized: $text\n";

  # http://en.wikipedia.org/wiki/Vehicle_registration_plates_of_the_United_Kingdom,_Crown_dependencies_and_overseas_territories#Current_system
  # local tag: two letters A-Z except IQZ.
  # age identifier: two digits
  # random letters: three letters A-Z except IQ
  if ($text =~ m/^([A-HJ-PR-Y]{2})(\d{2})([A-HJ-PR-Y]{3})$/) {
    my ($local_tag, $age_ident, $random) = ($1, $2, $3);

    # Top level: first letter of local tag.
    # arrayref is name for first level...
    # followed by pairs of regexes for second letter (which can ignore the fact that IQZ aren't used), and name that matches.
    state $local_tags = {
                         A=>['Anglia',
                             qr/[A-N]/, 'Peterborough',
                             qr/[O-U]/, 'Norwich',
                             qr/[V-Y]/, 'Ipswich',
                            ],
                         B=>['Birmingham', qr/[A-Y]/, 'Birmingham'],
                         C=>['Cymru (Wales)', 
                             qr/[A-O]/, 'Cardiff',
                             qr/[P-V]/, 'Swansea',
                             qr/[WXY]/, 'Bangor'],
                         D=>['Deeside to Shrewsbury',
                             qr/[A-K]/, 'Chester',
                             qr/[L-Y]/, 'Shrewsbury'],
                         E=>['Essex',
                             qr/[A-Y]/, 'Chelmsford'],
                         F=>['Forest and Fens (East Midlands)',
                             qr/[A-P]/, 'Nottingham',
                             qr/[R-Y]/, 'Lincoln'],
                         G=>['Garden of England (Kent, Surrey and Sussex)',
                             qr/[A-O]/, 'Maidstone',
                             qr/[P-Y]/, 'Brighton'],
                         H=>['Hampshire and Dorset (including Isle of Wight)',
                             qr/[A-J]/, 'Bournemouth',
                             qr/W/,   , '(Isle of Wight)', #Note: W for Isle of Wight residents only
                             qr/[K-Y]/, 'Portsmouth'],
                         K=>['(Milton Keynes)', 
                             qr/[A-L]/, 'Borehamwood',
                             qr/[M-Y]/, 'Northampton'],
                         L=>['London',
                             qr/[A-J]/, 'Wimbledon',
                             qr/[K-T]/, 'Borehamwood',
                             qr/[U-Y]/, 'Sidcup'],
                         M=>['Manchester and Merseyside',
                             qr/[A-MO-Y]/, 'Manchester',
                             qr/N/, '(Isle of Man)', # Note: N reserved for Isle of Man
                            ],
                         N=>['North',
                             qr/[A-O]/, 'Newcastle',
                             qr/[P-Y]/, 'Stockton'],
                         O=>['Oxford', qr/[A-Y]/, 'Oxford'],
                         P=>['Preston',
                             qr/[A-T]/, 'Preston',
                             qr/[U-Y]/, 'Carlisle'
                            ],
                         R=>['Reading', 
                             qr/[A-Y]/, 'Reading'],
                         S=>['Scotland',
                             qr/[A-J]/, 'Glasgow',
                             qr/[L-O]/, 'Edinburgh',
                             qr/[P-T]/, 'Dundee',
                             qr/[U-W]/, 'Aberdeen',
                             qr/[X-Y]/, 'Inverness'],
                         V=>['Severn Valley',
                             qr/[A-Y]/, 'Worcester'],
                         W=>['West of England',
                             qr/[A-J]/, 'Exeter',
                             qr/[KL]/, 'Truro',
                             qr/[M-Y]/, 'Bristol'],
                         X=>['Temporary Export Plates',
                             qr/[A-F]/, 'Temporary Export Plates'],
                         Y=>['Yorkshire',
                             qr/[A-K]/, 'Leeds',
                             qr/[L-U]/, 'Sheffield',
                             qr/[V-Y]/, 'Beverley'],
                        };

    $output .= "Current system\n";
    $output .= "Local tag: $local_tag\n";
    my ($local_region_tag, $local_city_tag) = split //, $local_tag;
    if (exists $local_tags->{$local_region_tag}) {
      my $region_info = $local_tags->{$local_region_tag};
      my $region_name = $region_info->[0];
      my $i=1;
      while (1) {
        if ($i > @$region_info-1) {
          last;
        }
        
        print "Checking i=$i for region $region_name, tag $local_city_tag\n";

        if ($local_city_tag =~ $region_info->[$i]) {
          my $city_name = $region_info->[$i+1];
          
          $output .= " Region: $region_name\n";
          $output .= " DVLA office: $city_name\n";
          
          last;
        }
        
        $i += 2;
      }
    }
    $output .= "Age identifier: $2\n";
    $output .= "Random letters: $3\n";

  } else {
    $output .= "Cannot parse -- invalid, or unsupported format\n";
  }

  return $output;
}
