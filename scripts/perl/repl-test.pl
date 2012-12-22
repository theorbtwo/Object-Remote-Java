#!/usr/bin/perl
use strictures 1;
use Eval::WithLexicals;
use Try::Tiny;
use Object::Remote;
use 5.10.1;

my $eval = Eval::WithLexicals->with_plugins('HintPersistence')->new;

use Object::Remote::Connector::TCP;
my $conn = Object::Remote->connect('tcp://'.(shift||'localhost:9849').'/');
my $activity = uk::me::desert_island::theorbtwo::bridge::AndroidServiceStash->can::on($conn, 'get_activity')->();

my $id = 10;

my $output_text = android::widget::TextView->new::on($conn, $activity);
$output_text->setId($id++);
$output_text->setText(make_spannable($conn, "# welcome to the Android Perl Read-Eval-Print-Loop\n"));

my $edit_text = android::widget::EditText->new::on($conn, $activity);
$edit_text->setId($id++);

my $go_button = android::widget::Button->new::on($conn, $activity);
$go_button->setId($id++);
$go_button->setText("Go");
my $button_event_listener = uk::me::desert_island::theorbtwo::bridge::InterfaceImplementor->can::on($conn, 'make_instance')->('android.view.View$OnClickListener', \&go_button_cb);
$go_button->setOnClickListener($button_event_listener);


my $overall_layout = android::widget::RelativeLayout->new::on($conn, $activity);
$overall_layout->addView($output_text,
                         make_rel_layout_params($conn, $overall_layout, 
                                                WIDTH => 'FILL_PARENT',
#                                                HEIGHT => $overall_layout->__get_property('height') - $edit_text->__get_property('height'),
#                                                HEIGHT => 0,
                                                ALIGN_PARENT_TOP => 'TRUE',
                                                ALIGN_PARENT_LEFT => 'TRUE',
                                                ALIGN_PARENT_RIGHT => 'TRUE',
                                                ABOVE => $output_text,
                                               ));
$overall_layout->addView($edit_text,
                         make_rel_layout_params($conn, $overall_layout, 
#                                                WIDTH => 'FILL_PARENT',                                                
                                                HEIGHT => 'WRAP_CONTENT',
                                                LEFT_OF => $go_button,
                                                ALIGN_PARENT_LEFT => 'TRUE',
                                                ALIGN_PARENT_BOTTOM => 'TRUE',
                                               ));
$overall_layout->addView($go_button,
                         make_rel_layout_params($conn, $overall_layout,
                                                WIDTH => 'WRAP_CONTENT',
                                                HEIGHT => 'WRAP_CONTENT',
#                                                BELOW => $output_text,
#                                                RIGHT_OF => $edit_text,
                                                ALIGN_PARENT_RIGHT => 'TRUE',
                                                ALIGN_PARENT_BOTTOM => 'TRUE',
                                               ));




my $runnable = uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $activity, 'setContentView', [$overall_layout]);
$activity->runOnUiThread($runnable);
my $or_loop = Object::Remote->current_loop;
$or_loop->want_run;
$or_loop->run_while_wanted;

sub make_spannable {
  my ($conn, $text, %decorations) = @_;

  # http://developer.android.com/reference/android/text/Spanned.html#SPAN_EXCLUSIVE_EXCLUSIVE
  state $flags = 0x21;

  my $spannable = android::text::SpannableStringBuilder->new::on($conn, $text);
  for (keys %decorations) {
    my $v = $decorations{$_};
    when ('foreground') {
      # Android wants this to be a *signed* integer, but in perl, 0x... constants are *unsigned*.
      $v = unpack('l', pack('L', $v));
      my $style = android::text::style::ForegroundColorSpan->new::on($conn, $v);
      $spannable->setSpan($style, 0, length($text), $flags);
    }
    default {
      die "Don't know what to do with decoration $_ (value $v)";
    }
  }

  return $spannable;
}

sub make_rel_layout_params {
  my ($conn, $rel_layout, @in_params) = @_;

  my $lp = 'android.widget.RelativeLayout$LayoutParams'->new::on($conn, -1, -1);
  while (@in_params) {
    my ($verb, $anchor) = splice(@in_params, 0, 2, ());

    if (ref $anchor) {
      $anchor = $anchor->getId();
    } elsif ($anchor eq 'FILL_PARENT') {
      $anchor = -1;
    } elsif ($anchor eq 'WRAP_CONTENT') {
      $anchor = -2;
    } elsif ($anchor =~ m/^[-0-9]/) {
      # plain number, or number followed by unit, but we can be lax --
      # anything that isn't a java identifier will do.
    } else {
      $anchor = $rel_layout->__get_property($anchor);
    }

    if ($verb eq 'WIDTH') {
      $lp->getClass->getField('width')->set($lp, $anchor);
    } elsif ($verb eq 'HEIGHT') {
      $lp->getClass->getField('height')->set($lp, $anchor);
    } else {
      $verb = $rel_layout->__get_property($verb);
      $lp->addRule($verb, $anchor);
    }
  }

  print "\n", $lp->debug(""), "\n";

  return $lp;
}

sub go_button_cb {
  my ($invocation_handler, $this, $method_info, $args) = @_;
  my ($method, $decl_class, $short_name, $long_name) = @$method_info;

  if ($long_name eq 'public abstract void android.view.View$OnClickListener.onClick(android.view.View)') {
    my ($view) = @$args;
    my $to_eval = $edit_text->getText->toString;
    $to_eval .= "\n" unless $to_eval =~ m/\n$/;

    my $red   = 0xFFFF3333;
    my $green = 0xFF33FF33;
    my $blue  = 0xFF3333FF;

    my $spannable = make_spannable($conn, $to_eval, foreground => $blue);

    $activity->runOnUiThread(uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $output_text, 'append', [$spannable]));

    my @ret;
    
    my $ok = eval {
      local $SIG{INT} = sub {die "Caught SIGINT"};
      @ret = $eval->eval($to_eval); 1;
    };

    my $new_text;

    if ($ok) {
      if (@ret == 1) {
        $new_text .= "Success: '$ret[0]'\n";
      } else {
        $new_text .= "Success: \n";
        $new_text .= "  $_: $ret[$_]\n";
      }
      $new_text = make_spannable($conn, $new_text, foreground => $green);
    } else {
      $new_text .= "Died: $@\n";
      $new_text = make_spannable($conn, $new_text, foreground => $red);
    }
    
    $activity->runOnUiThread(uk::me::desert_island::theorbtwo::bridge::MethodCallRunnable->new::on($conn, $output_text, 'append', [$new_text]));
  } else {
    die "Unknown method on go button: $long_name";
  }
}
