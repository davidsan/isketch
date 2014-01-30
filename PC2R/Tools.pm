# This module contains useful subroutines used in the iSketch server
# Last edit on 25/11/2013 by David San <david.san@etu.upmc.fr>
$VERSION = $VERSION = 0.01;

package PC2R::Tools;
require Exporter;
@ISA    = qw( Exporter );
@EXPORT = qw(
  is_number
  is_array_of_integer
  pick_random_word
  hash_to_scoreboard_html
  );
use strict;

sub is_integer
{
	defined $_[0] && $_[0] =~ /^[+-]?\d+$/;
}

sub is_array_of_integer
{
	my $aref = shift;
	my @argv = @{$aref};
	foreach (@argv)
	{
		next if is_integer $_;
		return 0;
	}
	return 1;
}

# TODO Read once
sub pick_random_word
{
	my $fn = shift;
	open FILE, $fn or die;
	my @dict = ();
	while (<FILE>)
	{
		chomp;
		push @dict, $_;
	}
	close FILE;
	my $rand = rand( scalar @dict );
	my $word = $dict[$rand];
	return $word;
}

sub hash_to_scoreboard_html
{
	my $hashref = shift;
	my %hash    = %{$hashref};
	my $current_time = time();
	my $html    = '
	 <table class="table table-striped table-bordered">
        <thead>
          <tr>
            <th>Player</th>
            <th>Score</th>
            <th>Victory</th>
            <th>Defeat</th>
            <th>Uptime (seconds)</th>
          </tr>
        </thead>
        <tbody>
        ';
	foreach my $k ( keys %hash )
	{
		my $uptime = $current_time-$hash{$k}{'time'};
		my $tr = "<tr>";
		$tr   .= "<td>$k</td>";
		$tr   .= "<td>$hash{$k}{'score'}</td>";
		$tr   .= "<td>$hash{$k}{'victory'}</td>";
		$tr   .= "<td>$hash{$k}{'defeat'}</td>";
		$tr   .= "<td>".$uptime."</td>";
		$tr   .= "</tr>";
		$html .= $tr;
	}
	$html.="</tbody></table>";
	return $html;
}
1;
