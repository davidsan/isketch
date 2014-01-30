#! /usr/bin/perl -l
use strict;
use warnings;
use Getopt::Long;
my $JAVA_PROJECT_DIR = "~/workspace/isketch/bin/";
my $JAVA_JAR = "isketch.jar";
my %opts;
GetOptions( \%opts, 'port=i', 'user=s', 'host=s' );
$opts{'port'} = 2013        if !defined $opts{'port'};
$opts{'user'} = "pc2r"      if !defined $opts{'user'};
$opts{'host'} = "127.0.0.1" if !defined $opts{'host'};
my $cmd =
    "java -jar "
  . $JAVA_JAR . " "
  . $opts{'port'} . " "
  . $opts{'user'} . " "
  . $opts{'host'};
print $cmd;
system($cmd);
