#! /usr/bin/perl -l
use strict;
use warnings;
use threads;
my $number_clients = shift || 3;
my $cmd            = "./client_bc.pl -port=2013 -user=";
my $i              = 0;
my @threads        = ();

while ( $i < $number_clients )
{
	print "Launch client $i";
	push @threads, threads->create( \&launch_client, "Client#".$i );
	$i++;
}
foreach (@threads)
{
	$_->join();
}
print "All clients terminated.";

sub launch_client
{
	my $username = shift;
	system( $cmd. "'" . $username . "'" );
}
