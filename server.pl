#!/usr/bin/perl -l
use strict;
use warnings;
use threads;
use threads::shared;
use Thread::Queue;
use IO::Socket;
use CGI;
use Getopt::Long;
use Pod::Usage;
use PC2R::HTML::HashTable;
use PC2R::Tools;
use Time::HiRes qw ( time );

# Debugging stuff #
use Data::Dumper;
use diagnostics;
diagnostics::enable;
use constant DEBUG => 1;    # 1 to enable debugging trace
use constant BOOTSTRAP_CSS =>
    "http://netdna.bootstrapcdn.com/bootstrap/3.0.2/css/bootstrap.min.css";

# Check if this Perl is built to support threads
use Config;
$Config{useithreads} or die('Recompile Perl with threads to run this program.');

# Server stuff #
# This hash is used to store data about the clients connected
my %clients : shared    = ();
my %spectators : shared = ();

# This hash is used to store all commands sent by the clients
my %history : shared = ();

# Store the server settings
# Can be changed by passing options
my %cfg : shared = ();
GetOptions( \%cfg, 'help!', 'max=i', 'timeout=i', 'port=i', 'dico=s', 'host=s' )
    or pod2usage("Try '$0 --help for more information");
pod2usage( -verbose => 1 ) if $cfg{'help'};
$cfg{"max"} = 3 if !defined $cfg{'max'};    # Number of players to start a game
$cfg{"timeout"} = 30   if !defined $cfg{'timeout'}; # Time in seconds of a round
$cfg{"port"}    = 2013 if !defined $cfg{'port'};    # Port of the server
$cfg{"dico"} = "dict/amiens.dict"
    if !defined $cfg{'dico'};    # Dictionnary used to generate word
$cfg{"host"} = "0.0.0.0"
    if !defined $cfg{'host'};    # Host address
$cfg{"pause"}     = 20;        # Time in seconds between 2 games
$cfg{"alert"}     = 3;         # Alerts needed to stop a round
$cfg{"http_port"} = 2092;      # Port of the http server
my $cid = 0;                   # Client ID, for debug purpose
my @threads : shared = ();

# Protocole #
my @commands_all =
    qw(CONNECT CONNECTED EXIT EXITED NEW_ROUND GUESS GUESSED WORD_FOUND
  WORD_FOUND_TIMEOUT END_ROUND SCORE_ROUND SET_COLOR SET_SIZE SET_LINE
  LINE TALK LISTEN CHEAT PASS SPECTATOR);

# Protocole client -> server
my @commands =
    qw(CONNECT EXIT GUESS SET_COLOR SET_SIZE SET_LINE TALK CHEAT PASS SPECTATOR);

# Create a listening socket
my $listen = IO::Socket::INET->new(
    LocalAddr => $cfg{"host"},
    LocalPort => $cfg{"port"},
    ReuseAddr => 1,
    Proto     => 'tcp',
    Listen    => 10,             # Queue size for listen
    );
die "Cant't create a listening socket : $@" unless $listen;
print "S : Ready. Waiting for connections on " . $cfg{"port"} if DEBUG;

# Game stuff #
my %game : shared   = ();
my %rounds : shared = ();
$game{"rounds"} = \%rounds;
$game{"state"}  = 0;             # 0 = not started, 1 = in progress, 2 = ended
$game{"queue"} = Thread::Queue->new();

# User stuff #
my $username = "";
my $exit     = 0;

# Game thread
my $game_thr = threads->create( \&handle_game )->detach();
my $http_thr = threads->create( \&http_server )->detach();

# Accept loop listening for connections
while ( my $socket = $listen->accept )
{
    print "S : New connection (cid=$cid)" if DEBUG;
    $cid++;
    $threads[$cid] = threads->create( \&handle_connection, $socket )->detach();
}

sub handle_game
{
    _broadcast_all( "BROADCAST", "GAME_WILL_START_WHEN_ENOUGH_PLAYERS" );
    {
        lock %game;
        cond_wait(%game) until $game{"state"} == 1;
    }
    print "S : Game started." if DEBUG;
    _broadcast_all( "BROADCAST", "GAME_STARTED" );

    # loop MAX time
    while ( scalar( keys %{ $game{"rounds"} } ) < $cfg{"max"} )
    {
        # Make new round
        my $round_id = keys %{ $game{"rounds"} };
        my $drawer   = pick_drawer();
        $game{"rounds"}{$round_id} = &share( {} );
        add_round( $round_id, $drawer );
        my $current_round = \%{ $game{"rounds"}{$round_id} };
        print( "S : New round, round ", $round_id ) if DEBUG;
        print( "S : Artist is ",        $drawer )   if DEBUG;
        broadcast_new_round( $drawer, $current_round->{"word"} );
        broadcast_server("ARTIST_IS/$drawer");

        while ( defined( my $data_ref = $game{"queue"}->dequeue() ) )
        {
            if ( $current_round->{"ended"} != 0 || $game{"state"} != 1 )
            {
                print "S : End of round" if DEBUG;
                last;
            }
            my %data = %{$data_ref};
            my $cmd  = $data{"command"};
            my $user = $data{"username"};
            if ( $cmd =~ /^(g)|(guess)$/i )
            {
                my $word = shift @{ $data{"argv"} };
                if ( is_artist( $current_round, $user ) )
                {
                    # if artist try to guess...
                    broadcast_error_user( $clients{$user}{"socket"},
                                          "ARTIST_CANNOT_GUESS" );
                    print "Client "
                        . $user
                        . " (artist on this round) tried to guess the word"
                        if DEBUG;
                    next;
                }
                if ( defined $current_round->{"guesser"}{$user} )
                {
                    # guess twice
                    broadcast_error_user( $clients{$user}{"socket"},
                                          "ALREADY_GUESSED" );
                    print "Client " . $user . " tried to guess the word twice"
                        if DEBUG;
                    next;
                }
                if ( $current_round->{"word"} =~ m/^$word$/i )
                {    # correct !
                    $current_round->{"guesser"}{$user}++; # remember who guessed
                    if (    $current_round->{"found"} == 0
                            && $cfg{"max"} > 2 )
                    {    # if first correct guesser
                        threads->create( \&arm_timeout, $round_id )->detach();

                        # arm_timeout();
                        $current_round->{"winner"} = $user;
                        broadcast_word_found_timeout($user);
                    }
                    broadcast_word_found($user);
                    $current_round->{"found"}++;

                    # update the score of the user
                    $clients{$user}{"score"} +=
                        compute_score_guesser( $current_round->{"found"} );
                    if ( $current_round->{"found"} ==
                         ( ( scalar( keys %clients ) ) - 1 ) )
                    {
                        $current_round->{"ended"} = 1;
                        broadcast_server("EVERYBODY_HAS_GUESSED_THE_WORD");
                        print "Everybody has guessed the word!" if DEBUG;
                        last;
                    }
                } else
                {    # wrong word !
                    broadcast_guessed( $user, $word );
                }
            } elsif ( $cmd =~ m/talk/i )
            {
                my $text = shift @{ $data{"argv"} };
                talk_command( $user, $text );
            } elsif ( $cmd =~ m/tick/i )
            {
                my $argv = shift @{ $data{"argv"} };
                my $time = $cfg{"timeout"} - $argv;
                if ( $time ne 0 && ( $time % 5 ) eq 0 )
                {
                    _broadcast_all( "BROADCAST", "ROUND_END_IN", $time );
                }
                print "tick" if DEBUG;
            } elsif ( $cmd =~ /^(sc)|(set_color)$/i )
            {
                set_color_command( $user, $data{"argv"} );
            } elsif ( $cmd =~ /^(ss)|(set_size)$/i )
            {
                set_size_command( $user, $data{"argv"} );
            } elsif ( $cmd =~ /^(sl)|(set_line)$/i )
            {
                set_line_command( $current_round, $user, $data{"argv"} );
            } elsif ( $cmd =~ /^(sb)|(set_courbe)$/i )
            {
                set_courbe_command( $current_round, $user, $data{"argv"} );
            } elsif ( $cmd =~ /^(a!)|(alert)|(cheat)$/i )
            {
                alert_command( $current_round, $user, $data{"argv"} );
            } elsif ( $cmd =~ /^(sk)|(skip)|(pass)$/i )
            {
                skip_command( $current_round, $user );
            } elsif ( $cmd =~ /^(e)|(exit)$/i )
            {
                my $username = shift @{ $data{"argv"} };
                exit_command_round( $current_round, $user, $username );
            } else
            {
                print "S : Unknown command $cmd"
                    if DEBUG;    # this should never happen
            }
        }
        $current_round->{"ended"} = 1;
        if ( not defined $current_round->{"winner"} )
        {
            $current_round->{"winner"} = "nobody";
        }

        # hyp : we always have a winner
        broadcast_end_round( $current_round->{"winner"},
                             $current_round->{"word"} );
        if (    $current_round->{"found"} > 0
                && $current_round->{"alert"} < $cfg{"alert"} )
        {
            # HACK to maintain victory / defeat count
            foreach ( keys %clients )
            {
                $clients{$_}{"defeat"}++;
            }
            foreach ( keys %{ $current_round->{"guesser"} } )
            {
                if ( defined $clients{$_} )
                {
                    # in case the guesser has exited
                    $clients{$_}{"victory"}++;
                    $clients{$_}{"defeat"}--;
                }
            }
            if ( defined $clients{$drawer} )
            {    # in case the drawer has exited
                $clients{$drawer}{"defeat"}--;
            }

            # update artist score
            my $score_artist = 0;
            $score_artist = compute_score_artist( $current_round->{"found"} );
            if ( defined $clients{$drawer} )
            {
                $clients{$drawer}{"score"} += $score_artist;
            }
        }
        broadcast_score_round($round_id);
        print "S : End of the round !" if DEBUG;
    }
    $game{"state"} = 2;
    print "S : End of the game"    if DEBUG;
    print "S : Preparing new game" if DEBUG;
    _broadcast_all( "BROADCAST",
                    "ALL_SCORES_WILL_BE_LOST_WHEN_NEW_GAME_START" );

    # pause between game to view the score
    for ( 0 .. $cfg{"pause"} )
    {
        sleep 1;
        if ( ( $cfg{"pause"} - $_ ) % 5 eq 0 )
        {
            _broadcast_all( "BROADCAST", "GAME_START_IN", $cfg{"pause"} - $_ );
        }
    }
    print "S : Restarting the game" if DEBUG;
    reset_game();
    reset_clients();
    print "S : Game ready" if DEBUG;
    $game_thr = threads->create( \&handle_game )->detach();
    check_and_start();
}

sub talk_command
{
    my $user = shift;
    my $text = shift;
    broadcast_listen( $user, $text );
}

sub set_color_command
{
    my $user     = shift;
    my $argv_ref = shift;
    my @argv     = @{$argv_ref};
    if ( not is_array_of_integer $argv_ref )
    {
        return;
    }

    # a guesser can set the color of his brush
    my $red   = shift @argv;
    my $green = shift @argv;
    my $blue  = shift @argv;
    $clients{$user}{"drawing_context"}{"color_red"}   = $red;
    $clients{$user}{"drawing_context"}{"color_green"} = $green;
    $clients{$user}{"drawing_context"}{"color_blue"}  = $blue;
}

sub set_size_command
{
    # a player (guesser or artist) can set the size of his brush
    my $user     = shift;
    my $argv_ref = shift;
    my @argv     = @{$argv_ref};
    if ( not is_array_of_integer $argv_ref )
    {
        return;
    }
    my $size = shift @argv;
    $clients{$user}{"drawing_context"}{"brush_size"} = $size;
}

sub set_line_command
{
    my $current_round = shift;
    my $user          = shift;
    my $argv_ref      = shift;
    my @argv          = @{$argv_ref};
    if ( not is_artist( $current_round, $user ) )
    {
        print "Client " . $user . " (guesser on this round) tried to draw"
            if DEBUG;
        return;
    }
    my $x1              = shift @argv;
    my $y1              = shift @argv;
    my $x2              = shift @argv;
    my $y2              = shift @argv;
    my $drawing_context = $clients{$user}{"drawing_context"};
    broadcast_line(
        $x1,
        $y1,
        $x2,
        $y2,
        $drawing_context->{color_red},
        $drawing_context->{color_green},
        $drawing_context->{color_blue},
        $drawing_context->{brush_size}
	);
    $current_round->{"drawing"}{ time() } =
        "LINE/"
        . "$x1/$y1/$x2/$y2/"
        . "$drawing_context->{color_red}/"
        . "$drawing_context->{color_green}/"
        . "$drawing_context->{color_blue}/"
        . "$drawing_context->{brush_size}";
}

sub set_courbe_command
{
    my $current_round = shift;
    my $user          = shift;
    my $argv_ref      = shift;
    my @argv          = @{$argv_ref};

    # check that the user is the artist for the current round
    if ( not is_artist( $current_round, $user ) )
    {
        print "Client " . $user . " (guesser on this round) tried to draw"
            if DEBUG;
        return;
    }
    my $x1              = shift @argv;
    my $y1              = shift @argv;
    my $x2              = shift @argv;
    my $y2              = shift @argv;
    my $x3              = shift @argv;
    my $y3              = shift @argv;
    my $x4              = shift @argv;
    my $y4              = shift @argv;
    my $drawing_context = $clients{$user}{"drawing_context"};
    broadcast_bezier_curve(
        $x1,
        $y1,
        $x2,
        $y2,
        $x3,
        $y3,
        $x4,
        $y4,
        $drawing_context->{color_red},
        $drawing_context->{color_green},
        $drawing_context->{color_blue},
        $drawing_context->{brush_size}
	);
    $current_round->{"drawing"}{ time() } =
        "COURBE/"
        . "$x1/$y1/$x2/$y2/$x3//$y3/$x4/$y4/"
        . "$drawing_context->{color_red}/"
        . "$drawing_context->{color_green}/"
        . "$drawing_context->{color_blue}/"
        . "$drawing_context->{brush_size}";
}

sub alert_command
{
    my $current_round = shift;
    my $user          = shift;
    my $argv_ref      = shift;
    my @argv          = @{$argv_ref};

    # check if the alert is for the current round
    my $cheater = shift @argv;
    if ( $cheater ne $current_round->{"drawer"} )
    {
        print "Client "
            . $user
            . "tried to alert someone who is not the artist of the current round"
            if DEBUG;
        _broadcast_user( $user, "ERROR/CANT_ALERT_NOT_THE_ARTIST" );
        return;
    }

    # check that the user is not the artist for the current round
    if ( is_artist( $current_round, $user ) )
    {
        print "Client "
            . $user
            . " (artist on this round) tried to alert himself"
            if DEBUG;
        _broadcast_user( $user, "ERROR/CANT_ALERT_WHEN_ARTIST" );
        return;
    }
    if ( defined( $current_round->{"alerters"}{$user} )
         && $current_round->{"alerters"}{$user} > 0 )
    {
        print "Client " . $user . " tried to alert more than once"
            if DEBUG;
        _broadcast_user( $user, "ERROR/CANT_ALERT_TWICE" );
        return;
    }
    $current_round->{"alerters"}{$user}++;
    $current_round->{"alert"}++;
    if ( $current_round->{"alert"} >= $cfg{"alert"} )

        #	 && $current_round->{"found"} == 0 )
    {
        $current_round->{"winner"} = "nobody";
        $current_round->{"ended"}  = 1;
        my @empty = ();
        my %fake = (
            "command"     => "tick",
            "argc"        => 0,
            "argv"        => \@empty,
            "username"    => "",
            "raw_request" => ""
            );

        # HACK enqueue a fake tick to stop the round
        $game{"queue"}->enqueue( \%fake );
    }
    broadcast_alert();
}

sub skip_command
{
    my $current_round = shift;
    my $user          = shift;

    # check that the user is the artist for the current round
    if ( !is_artist( $current_round, $user ) )
    {
        print "Client "
            . $user
            . " (guesser on this round) tried to skip, but is not the drawer"
            if DEBUG;
        return;
    }
    if ( $current_round->{"found"} > 0 || $current_round->{"ended"} != 0 )
    {
        _broadcast_user( $user, "ERROR/CANT_SKIP" );
        return;
    }
    $current_round->{"winner"} = "nobody";
    $current_round->{"ended"}  = 1;
    my @empty = ();
    my %fake = (
        "command"     => "tick",
        "argc"        => 0,
        "argv"        => \@empty,
        "username"    => "",
        "raw_request" => ""
	);

    # HACK enqueue a fake tick to stop the round
    $game{"queue"}->enqueue( \%fake );
    broadcast_skip($user);
}

sub is_artist
{
    my $current_round = shift;
    my $user          = shift;
    return $current_round->{"drawer"} eq $user;
}

sub arm_timeout
{
    my $round_id = shift;
    my @empty    = ();
    my %hash = (
        "command"     => "tick",
        "argc"        => 0,
        "argv"        => \@empty,
        "username"    => "",
        "raw_request" => ""
	);
    for my $i ( 0 .. ( $cfg{"timeout"} / 2 ) )
    {
        sleep 2;
        @empty = ( $i * 2 );
        $hash{"argv"} = \@empty;
        $game{"queue"}->enqueue( \%hash );
        last if ( $game{"rounds"}{$round_id}{"ended"} eq 1 );
    }
    $game{"rounds"}{$round_id}{"ended"} = 1;
    $game{"queue"}->enqueue( \%hash );
}

sub compute_score_artist
{
    my $found = shift;    # number of people who guessed the word
    if ( $found > 5 )
    {
        return 15;
    }
    if ( $found > 0 )
    {
        return 10 + ( $found - 1 );
    }
    return 0;
}

sub compute_score_guesser
{
    my $found = shift;    # number of people who guessed, guesser included
    if ( $found > 5 )
    {
        return 5;
    } else
    {
        return 10 - ( $found - 1 );
    }
}

sub reset_game
{
    $game{"rounds"} = &share( {} );
    $game{"state"}  = 0;
    $game{"queue"}  = Thread::Queue->new();
}

sub add_round
{
    my $round_id = shift;
    my $drawer   = shift;
    $game{"rounds"}{$round_id}{"drawer"} = $drawer;
    $game{"rounds"}{$round_id}{"word"} =
        PC2R::Tools::pick_random_word( $cfg{"dico"} );
    $game{"rounds"}{$round_id}{"found"}    = 0;
    $game{"rounds"}{$round_id}{"guesser"}  = &share( {} );
    $game{"rounds"}{$round_id}{"alert"}    = 0;
    $game{"rounds"}{$round_id}{"alerters"} = &share( {} );
    $game{"rounds"}{$round_id}{"ended"}    = 0;
    $game{"rounds"}{$round_id}{"drawing"}  = &share( {} );
}

sub pick_drawer
{
    foreach $username ( keys %clients )
    {
        if ( $clients{$username}{has_draw} == 0 )
        {
            $clients{$username}{has_draw}++;
            return $username;
        }
    }
    return "nobody";
}

sub handle_connection
{
    my $socket = shift;
    while ( $socket->connected() && ( my $inputLine = <$socket> ) )
    {
        print( "C" . $cid . " : ", $inputLine ) if DEBUG;

        # last if ($game{"state"} eq 2);
        if ( not $inputLine =~ /\/$/ )
        {
            broadcast_error_user( $socket, "COMMAND_MUST_END_WITH_SLASH" );
            next;
        }
        handle_request( $socket, $inputLine );
        if ( $exit eq 1 )
        {
            last;
        }
    }
    print "C" . $cid . " : DISCONNECTED" if DEBUG;
    if ( _is_connected($socket) )
    {
        my $username = get_username_from_socket($socket);
        my @args     = ();
        push @args, $username;
        if ( $game{"state"} == 1 )
        {
            my %hash = (
                "command"     => "EXIT",
                "argc"        => 1,
                "argv"        => \@args,
                "username"    => $username,
                "raw_request" => "EXIT/$username"    # debug purpose
                );
            $game{"queue"}->enqueue( \%hash );
            return;
        }
        return exit_command( $socket, $username );
    }
    shutdown( $socket, 2 );
    close($socket);
}

sub get_username_from_socket
{
    my $client = shift;
    foreach my $k ( keys %clients )
    {
        if ( ( fileno $client ) eq $clients{$k}{"socket"} )
        {
            return $k;
        }
    }
    return $client;
}

sub handle_request
{
    my $client  = shift || die "missing arg";
    my $request = shift || die "missing arg";
    my $username = get_username_from_socket($client);
    print "$username sent a request." if DEBUG;
    my @hist_element : shared = ();
    push @hist_element, $request;
    push @hist_element, fileno $client;
    $history{ time() } = \@hist_element;

    # Isoler la commande des arguments dans la requete
    my @args    = _split_request($request);
    my $command = shift @args || "";
    my $argc    = @args;
    if ( $command =~ /^(c)|(connect)$/i && ( $argc == 1 ) )
    {
        my $name = shift @args;
        return connect_command( $client, $name );
    } elsif ( $command =~ /^(spec)|(spectator)$/i && ( $argc == 0 ) )
    {
        return spectator_command($client);
    }
    if ( not _is_connected($client) )
    {
        broadcast_error_user( $client, "NOT_CONNECTED" );
        print "S : Client $client is not connected : $command.\n"
            if DEBUG;
        return;
    }
    if ( $command =~ /^(e)|(exit)$/i && ( $argc == 1 ) )
    {
        if ( $game{"state"} == 1 )
        {
            my %hash = (
                "command"     => $command,
                "argc"        => $argc,
                "argv"        => \@args,
                "username"    => $username,
                "raw_request" => $request     # debug purpose
                );
            $game{"queue"}->enqueue( \%hash );
        } else
        {
            my $name = shift @args;
            return exit_command( $client, $name );
        }
    } elsif (    ( $command =~ /^(g)|(guess)$/i && ( $argc == 1 ) )
                 || ( $command =~ /^(t)|(talk)$/i           && ( $argc == 1 ) )
                 || ( $command =~ /^(ss)|(set_size)$/i      && ( $argc == 1 ) )
                 || ( $command =~ /^(sc)|(set_color)$/i     && ( $argc == 3 ) )
                 || ( $command =~ /^(sl)|(set_line)$/i      && ( $argc == 4 ) )
                 || ( $command =~ /^(sb)|(set_courbe)$/i    && ( $argc == 8 ) )
                 || ( $command =~ /^(a!)|(alert)|(cheat)$/i && ( $argc == 1 ) )
                 || ( $command =~ /^(sk)|(skip)|(pass)$/i   && ( $argc == 0 ) ) )
    {
        my %hash = (
            "command"     => $command,
            "argc"        => $argc,
            "argv"        => \@args,
            "username"    => $username,
            "raw_request" => $request     # debug purpose
            );
        $game{"queue"}->enqueue( \%hash );
        return;
    } elsif (DEBUG)
    {
        handle_debug_command( $username, $argc, $command, $_, $client,
                              \@commands );
    }
}

sub _split_request
{
    my $request = shift || die "missing arg";
    my @parse = ();

    # this regex split at each forward slash
    # it doesn't split at the special character '\/'
    while ( $request =~ m/(([^\\\/]*|(\\\/)|(\\\\))*)\//g )
    {
        push @parse, $1;
    }
    return @parse;
}

sub connect_command
{
    my $client = shift;
    $username = shift;
    my $suffix = '_';

    # check if username is taken
    # if it is, append one or more underscores at the
    # end of the username
    {
        lock(%clients);
        if ( _is_connected($client) )
        {
            broadcast_error_user( $client, "ALREADY_CONNECTED" );
            print "S : Client $client tried to connect twice.\n" if DEBUG;
            return;
        }
        if ( scalar( keys %clients ) >= $cfg{"max"} )
        {
            broadcast_error_user( $client, "NO_AVAILABLE_SLOT" );
            $exit = 1;    # soft shutdown of the client
            return;
        }

        # if username is empty, rename to "anon"
        if ($username eq ""){
            $username = "anon";
        }

        # append underscore until a valid username is found
        while ( defined $clients{$username} )
        {
            $username = $username . $suffix;
        }
        print "S : Client username is " . $username if DEBUG;

        # register the user to the clients hash
        add_user( $client, $username );
    }
    broadcast_connected($username);
    broadcast_user_list_players($username);
    check_and_start( $client, $username );
}

sub spectator_command
{
    my $client = shift;
    add_spectator($client);
    broadcast_user_list_players_spectator_socket($client);
    if ( $game{"state"} == 1 )    # if the game is started
    {
        # send all drawing command to the late user
        my $round_id = ( keys %{ $game{"rounds"} } ) - 1;    # index start at 0
        return if $round_id < 0;
        return unless defined $game{"rounds"}{$round_id};
        my $drawing = \%{ $game{"rounds"}{$round_id}{"drawing"} };
        broadcast_user_drawing_history_socket( $client, $drawing );
    }
}

sub check_and_start
{
    my $client   = shift;
    my $username = shift;
    lock(%clients);

    # if game is not started
    if ( $game{"state"} == 0 )
    {
        # start the game if enough player
        if ( scalar( keys %clients ) == ( $cfg{"max"} ) )
        {
            {
                lock %game;
                $game{"state"} = 1;
                cond_signal(%game);
            }
        }
    } elsif ( $game{"state"} == 1 )    # if the game is started
    {
        # send all drawing command to the late user
        my $round_id = ( keys %{ $game{"rounds"} } ) - 1;    # index start at 0
        my $drawing = \%{ $game{"rounds"}{$round_id}{"drawing"} };
        broadcast_user_drawing_history( $username, $drawing );
    }
}

sub _is_connected
{
    my $socket = shift;
    foreach my $k ( keys %clients )
    {
        if ( $clients{$k}{"socket"} == ( fileno $socket ) )
        {
            return 1;
        }
    }
    return 0;
}

sub exit_command
{
    my $client = shift;
    my $name   = shift;
    if ( $name ne $username )
    {
        broadcast_error_user( $client, "UNAUTHORIZED" );
        print "S : Client $client tried to disconnect someone else.\n"
            if DEBUG;
        return;
    }
    {
        lock(%clients);
        delete $clients{$name};
        $exit = 1;
    }
    broadcast_exited($name);    # or $user, same.
}

sub exit_command_round
{
    my $current_round = shift;
    my $client        = shift;
    my $name          = shift;
    my @empty         = ();
    my %fake = (
        "command"     => "tick",
        "argc"        => 0,
        "argv"        => \@empty,
        "username"    => "",
        "raw_request" => ""
	);
    if ( is_artist( $current_round, $name ) )
    {
        # if the artist exit the current round
        # if nobody found the word, and the round is not ended
        if ( $current_round->{"found"} == 0 && $current_round->{"ended"} == 0 )
        {
            $current_round->{"winner"} = "nobody";
            $current_round->{"ended"}  = 1;

            # HACK enqueue a fake tick to stop the round
            $game{"queue"}->enqueue( \%fake );
        }
    }
    {
        lock(%clients);
        delete $clients{$name};
        $exit = 1;
    }
    broadcast_exited($name);

    # stop the game if less than two players
    if ( scalar( keys %clients ) < 2 )
    {
        $game{"state"} = 2;

        # HACK enqueue a fake tick to stop the round
        $game{"queue"}->enqueue( \%fake );
    }
}

sub add_user
{
    my $client   = shift;
    my $username = shift;
    $clients{$username} = &share( {} );
    $clients{$username}{"username"}                       = $username;
    $clients{$username}{"score"}                          = 0;
    $clients{$username}{"defeat"}                         = 0;
    $clients{$username}{"victory"}                        = 0;
    $clients{$username}{"time"}                           = time();
    $clients{$username}{"has_draw"}                       = 0;
    $clients{$username}{"drawing_context"}                = &share( {} );
    $clients{$username}{"drawing_context"}{"color_red"}   = 0;
    $clients{$username}{"drawing_context"}{"color_green"} = 0;
    $clients{$username}{"drawing_context"}{"color_blue"}  = 0;
    $clients{$username}{"drawing_context"}{"brush_size"}  = 10;
    $clients{$username}{"socket"}                         = fileno $client;
}

sub add_spectator
{
    my $client = shift;
    my $fileno = fileno $client;
    $spectators{$fileno}++;
}

sub reset_clients
{
    foreach my $c ( keys %clients )
    {
        $clients{$c}{"score"}    = 0;
        $clients{$c}{"victory"}  = 0;
        $clients{$c}{"defeat"}   = 0;
        $clients{$c}{"has_draw"} = 0;
    }
}

sub _broadcast_all
{
    my @args = @_;
    return unless @args;
    my $msg = join( "/", @args ) . "/";
    lock(%clients);
    foreach my $username ( keys %clients )
    {
        my $fileno = $clients{$username}{"socket"};
        open my $fh, ">&=", $fileno or warn $! and die;
        print $fh $msg;
        close $fh;
    }
    
    lock(%spectators);
    foreach my $spectator ( keys %spectators )
    {
        my $fileno = $spectator;
        open my $fh, ">&=", $fileno or warn $! and die;
        print $fh $msg;
        close $fh;
    }
    print "S : " . $msg if DEBUG;
}

sub broadcast_connected
{
    my $username = shift;
    _broadcast_user( $username, "WELCOME", $username );
    _broadcast_all_but_user( $username, "CONNECTED", $username );
}

sub broadcast_user_list_players
{
    my $username = shift;
    foreach my $k ( keys %clients )
    {
        if ( $clients{$k} && $clients{$k}{"socket"} )
        {
            _broadcast_user( $username, "CONNECTED", $k );
        }
    }
}

sub broadcast_user_list_players_spectator_socket
{
    my $sock = shift;
    foreach my $k ( keys %clients )
    {
        _broadcast_user_by_socket( $sock, "CONNECTED", $k );
    }
}

sub broadcast_exited
{
    my $username = shift;
    _broadcast_all( "EXITED", $username );
}

sub broadcast_new_round
{
    my $drawer = shift;
    my $word   = shift;
    foreach my $username ( keys %clients )
    {
        if ( $drawer eq $username )
        {
            _broadcast_user( $username, "NEW_ROUND", "dessinateur", "$drawer",
                             $word );
        } else
        {
            _broadcast_user( $username, "NEW_ROUND", "devineur", "$drawer" );
        }
    }
}

sub _broadcast_user
{
    my $username = shift;
    my @args     = @_;
    my $msg      = join( "/", @args ) . "/";
    my $fileno   = $clients{$username}{"socket"};
    open my $fh, ">&=", $fileno or warn $!;
    print $fh $msg;
    print "S to $username : " . $msg if DEBUG;
    close $fh;
}

sub broadcast_user_drawing_history_socket
{
    my $socket      = shift;
    my $drawing_ref = shift;
    my %drawing     = %{$drawing_ref};
    foreach my $k ( sort keys %drawing )
    {
        open my $fh, ">&=", $socket or warn $!;
        print $fh $drawing{$k};
        print "S to $username : " . $drawing{$k} if DEBUG;

        # HACK Give some time to the client for drawing
        Time::HiRes::sleep(0.002);
        close $fh;
    }
}

sub broadcast_user_drawing_history
{
    my $username    = shift;
    my $drawing_ref = shift;
    my $fileno      = $clients{$username}{"socket"};
    return broadcast_user_drawing_history_socket( $fileno, $drawing_ref );
}

sub _broadcast_user_by_socket
{
    my $client = shift;
    my @args   = @_;
    my $msg    = join( "/", @args ) . "/";
    open my $fh, ">&=", $client or warn $! and die;
    print $fh $msg;
    print "S to $client : " . $msg if DEBUG;
    close $fh;
}

sub _broadcast_all_but_user
{
    my $spy  = shift;
    my @args = @_;
    my $msg  = join( "/", @args ) . "/";
    foreach my $username ( keys %clients )
    {
        if ( $username ne $spy )
        {
            my $fileno = $clients{$username}{"socket"};
            open my $fh, ">&=", $fileno or warn $! and die;
            print $fh $msg;
            close $fh;
        }
    }
    print "S : " . $msg if DEBUG;
}

sub broadcast_word_found_timeout
{
    my $user_who_found = shift;
    _broadcast_all_but_user( $user_who_found, "WORD_FOUND_TIMEOUT",
                             $cfg{"timeout"} );
}

sub broadcast_word_found
{
    my $user_who_found = shift;
    _broadcast_all( "WORD_FOUND", $user_who_found );
}

sub broadcast_guessed
{
    my $user_who_guess = shift;
    my $guess          = shift;
    _broadcast_all( "GUESSED", $guess, $user_who_guess );
}

sub broadcast_end_round
{
    my $winner = shift;
    my $word   = shift;
    _broadcast_all( "END_ROUND", $winner, $word );
}

sub broadcast_score_round
{
    my $round_id = shift;
    my @output;
    foreach my $user ( reverse( keys %clients ) )
    {
        push @output, $user;
        push @output, $clients{$user}{"score"};
    }
    my $str = join "/", @output;
    _broadcast_all( "SCORE_ROUND", $str );
}

sub broadcast_score_round_total
{
    my @output;
    foreach my $user ( reverse( keys %clients ) )
    {
        push @output, $user;
        push @output, $clients{$user}{"score"};
    }
    my $str = join "/", @output;
    _broadcast_all( "SCORE_GAME", $str );
}

sub broadcast_line
{
    my $x1 = shift;
    my $y1 = shift;
    my $x2 = shift;
    my $y2 = shift;
    my $r  = shift;
    my $g  = shift;
    my $b  = shift;
    my $s  = shift;
    _broadcast_all( "LINE", $x1, $y1, $x2, $y2, $r, $g, $b, $s );
}

sub broadcast_bezier_curve
{
    my $x1 = shift;
    my $y1 = shift;
    my $x2 = shift;
    my $y2 = shift;
    my $x3 = shift;
    my $y3 = shift;
    my $x4 = shift;
    my $y4 = shift;
    my $r  = shift;
    my $g  = shift;
    my $b  = shift;
    my $s  = shift;
    _broadcast_all( "COURBE",
                    $x1, $y1, $x2, $y2, $x3, $y3, $x4, $y4, $r, $g, $b, $s );
}

sub broadcast_listen
{
    my $user_who_talk = shift;
    my $text          = shift;
    _broadcast_all( "LISTEN", $user_who_talk, $text );
}

sub broadcast_error_user
{
    my $client    = shift;
    my $error_msg = shift;
    _broadcast_user_by_socket( $client, "ERROR", $error_msg );
}

sub broadcast_alert
{
    _broadcast_all( "BROADCAST",
                    "A_GUESSER_REMINDS_THE_ARTIST_NOT_TO_VIOLATE_DRAWING_RULES" );
}

sub broadcast_skip
{
    my $user = shift;
    _broadcast_all( "BROADCAST", "THE_ARTIST_HAS_SKIPPED", $user );
}

sub broadcast_server
{
    my $msg = shift;
    _broadcast_all( "BROADCAST", $msg );
}

sub handle_debug_command
{
    my $username = shift;
    my $argc     = shift;
    my $command  = shift;
    my $_        = shift;
    my $client   = shift;
    my $commands = shift;
    if ( $command =~ /^WHOAMI$/i && ( $argc == 0 ) )
    {
        print $client "YOU_ARE/" . $username . "/";
    } elsif ( $command =~ /^DUMP/i )
    {
        print Dumper %clients;
    } elsif ( $command =~ /^LIST$/i && ( $argc == 0 ) )
    {
        my $res = "PLAYERS/";
        if (DEBUG)
        {
            foreach my $key ( reverse( keys %clients ) )
            {
                $res .= $clients{$key}{"username"} . "/";
            }
        }
        print $client $res;
    } elsif (
        scalar(
            grep
            {
                $command =~ /^$_$/i
            } @$commands
        ) == 1
        )
    {
        broadcast_error_user( $client, "NOT_IMPLEMENTED" );
    } else
    {
        broadcast_error_user( $client, "BAD_REQUEST" );
        print Dumper(%clients);
    }
    return ();
}

# HTTP server for delivering scoreboard page
sub http_server
{
    # Create a listening socket
    my $listen = IO::Socket::INET->new(
        LocalPort => $cfg{"http_port"},
        ReuseAddr => 1,
        Proto     => 'tcp',
        Listen    => 5,                   # Queue size for listen
	);
    die "Cant't create a listening socket : $@" unless $listen;
    print "S : Ready. Waiting for connections on " . $cfg{"http_port"}
    if DEBUG;
    while ( my $client = $listen->accept )
    {
        handle_http_request($client);
    }
}

sub _print_hash_html
{
    my $client  = shift;
    my $hashref = shift;
    print $client (
        tablify(
            {
                BORDER => 1,
                DATA   => $hashref,
                SORTBY => 'key',
                ORDER  => 'asc'
            }
        )
	);
}

sub handle_http_request
{
    my $client = shift;
    my $q      = CGI->new;
    print "S (HTTP) : Serving HTTP page to ", fileno $client if DEBUG;
    print $client "HTTP/1.1 200 OK\r\n"
        . $q->header()
        . $q->start_html(
        -title => 'iSketch Server',
        -style => {
            -src  => BOOTSTRAP_CSS,
            -code => "body{background:#fff; color:#333;}"
        }
        );
    print $client $q->div( { -class => "container" } );
    print $client $q->div( { -class => "row" } );
    print $client $q->h1('iSketch Server');
    print $client $q->end_div;
    print $client $q->div( { -class => "row" } );
    print $client $q->div( { -class => "col-md-12" } );
    print $client $q->h2('Scoreboard');
    print $client hash_to_scoreboard_html( \%clients );
    print $client $q->end_div;
    print $client $q->end_div;
    print $client $q->div( { -class => "row" } );
    print $client $q->div( { -class => "col-md-4" } );
    print $client $q->h2('Config');
    _print_hash_html( $client, \%cfg );
    print $client $q->end_div;
    print $client $q->end_div;

    if (DEBUG)
    {
        print $client $q->div( { -class => "row" } );
        print $client $q->h1('Debug');
        print $client $q->end_div;
        print $client $q->div( { -class => "row" } );
        print $client $q->div( { -class => "col-md-12" } );
        print $client $q->h3('Clients');
        _print_hash_html( $client, \%clients );
        print $client $q->end_div;
        print $client $q->end_div;
        print $client $q->div( { -class => "row" } );
        print $client $q->div( { -class => "col-md-6" } );
        print $client $q->h3('Game');
        _print_hash_html( $client, \%game );
        print $client $q->end_div;
        print $client $q->div( { -class => "col-md-6" } );
        print $client $q->h3('History');
        _print_hash_html( $client, \%history );
        print $client $q->end_div;
        print $client $q->end_div;
    }
    print $client $q->end_div;
    print $client $q->end_html;
    shutdown( $client, 2 );
    close $client;
}

=head1 NAME

    server.pl
    
    =head1 SYNOPSIS

    server.pl --max=3 --timeout=30 --port=2013 --dico=dict/english.dict
    
    =head1 DESCRIPTION

    Multithreaded server for playing a Pictionary-like game. 

    =head1 ARGUMENTS

    --help      print Options and Arguments
    
    =head1 OPTIONS

    --max   	  number of players to start a game
    --timeout	  time in seconds of a round
    --port		  port of the server
    --dico       dictionnary used to generate word

    =head1 AUTHOR

    David San

    =head1 TESTED

    Perl    	 	5.14
    Linux			Mint 15 Olivia, Mint 16 Petra, Debian 6.0 Squeeze
    OSX			10.9 Mavericks

    =cut
