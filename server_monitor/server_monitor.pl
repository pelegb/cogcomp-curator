#!/usr/bin/perl -w

# script: server_monitor.pl
#  -- web demo version installed on grandpa.cs.uiuc.edu, run from gargamel
#
# description:
#   This script is itself a server used to monitor and manipulate processes on
#   the local machine.  It is designed to communicate with the
#   connect_to_server_monitor.pl cgi script on l2r.  That script sends this
#   script the following commands having the following effects:
#
#   - getProcessInformation
#     This command has no arguments.  When issued, this script will collect
#     information about the processes registered with it and return a
#     multi-line string, each line representing a single process and having
#     the following format:
#         <name>|<PID>|<running time>|<port>
#     <name> is the registered name of the process.  See the comments in the
#     code for how to register your process.  If the process could not be
#     identified, the <PID> field is left blank and the <running time> field
#     will contain the string "Not running".  <port> indicates the <b>main</b>
#     port on which the server listens (the demo may have components listening
#     on other ports also)
#   - pass <password>
#     When this command is received, the argument is checked against the
#     password stored in a password file.  If it doesn't match communication
#     between this script and its client is terminated.  Otherwise, the client
#     is given permission to use the "start" and "kill" commands.
#   - start <name>
#     The argument is the registered name of the process to start.  There must
#     be a single space between "start" and <name>.  If a password has not
#     been successfully transmitted via the "pass" command when this command
#     is received, communication between this script and its client is
#     terminated.  When this command is received, the command associated with
#     the specified process will be issued to the operating system.
#   - kill <name>
#     The argument is the registered name of the process to kill.  There must
#     be a single space between "kill" and <name>.  If a password has not been
#     successfully transmitted via the "pass" command when this command is
#     received, communication between this script and its client is
#     terminated.  This command runs the kill.sh script associated with
#     this process name.
#   - quit
#     This command terminates communication between this script and its
#     client.
#
#   After each command is carried out successfully, the message "Operation
#   successful" is sent back to the client (except for
#   "getProcessInformation", whose successful completion results in a message
#   filled with process information).  If the command could not be carried
#   out, an error message is sent to the client.  If a command other than one
#   listed above is received, the message "Unrecognized command" is returned
#   to the client, and communication is terminated.
#
#   VERY IMPORTANT NOTES:
#   These demo servers' properties are defined in a local resource file 
#   passed as an argument to this script. Check the README for details of
#   how to specify/provide resources for a demo.
#   Processes are partly managed by searching for an identifier string in the 
#   process list returned by the "ps" system call.  Since the implementation 
#   of this call is system dependent, you may need to experiment with its
#   implementation on your system and modify the appropriate code below.
#   
#   This script uses the network communication protocol adopted by cogcomp in
#   which the length of any message is always sent as a binary integer
#   immediately before the message is sent.  Knowing the length of a message
#   to be received is essential, since the recv command is not guaranteed to
#   receive an entire message, especially if that message is long.  ***TO
#   ENSURE THAT MESSAGES ARE SENT AND RECEIVED PROPERLY, BOTH THE SENDER AND
#   RECEIVER MUST IMPLEMENT THIS PROTOCOL, AND BOTH MUST BE USING THE LATEST
#   VERSION OF PERL.***
#
#   To run server_monitor, specify the main resource file and a second file
#   that simply lists the names of demos that should be run by this instance
#   of server_monitor, one entry per line, using the demo name specified in
#   the 'name' attribute of the <demo> node.
#

use strict;

use Socket;
use Carp;
use FileHandle;
use XML::Twig;

my $DEBUG = 1;

my $DEMO_TAG = "demo";
my $HOST_TAG = "host";
my $DEMO_SCRIPT_HOME_TAG = "demo_script_home";
my $KILL_SCRIPT_ATT = "kill_script";
my $COMPONENT_GROUP_TAG = "components";
my $HOME_PATH_ATT = "home_path";
my $COMPONENT_TAG = "component";
my $NAME_ATT = "name";
my $START_SCRIPT_TAG = "start_script";
my $PORT_TAG = "port";
my $PORT_TYPE_ATT = "type";
my $ARGS_TAG = "args";
my $ID_TAG = "process_identifier";
my $NEED_PORT_ATT = "need_port";
my $KEY_ATT = "key";
my $HOST_ATT = "host";
my $KILL_CMD_TAG = "kill_cmd";
my $ENV_LABEL = "env";
my $TRUE_VAL = "TRUE";
my $AUTO_VAL = "AUTO";

my $demoScriptHome;

my $hostName = $ENV{ HOSTNAME };

if ( $hostName =~ /^([^.]+)\./ ) {
  $hostName = $1;
}

print STDERR "## $0: host name is '$hostName'.\n";

my (%start, %processIdentifier, %port, %kill);


croak "Usage: $0 configFile port" unless @ARGV == 2;

my $configFile = $ARGV[0];
my $smPort = $ARGV[1];

sub logmsg { print "$0 $$: @_ at ", scalar localtime, "\n"; 1; }

my $proto = getprotobyname('tcp');


socket(Server, AF_INET, SOCK_STREAM, $proto) or die "socket: $!";
setsockopt(Server, SOL_SOCKET, SO_REUSEADDR, 1) or die "setsockopt: $!";
bind(Server, sockaddr_in($smPort, INADDR_ANY)) or die "bind port $smPort: $!";
listen(Server, SOMAXCONN) or die "listen: $!";

logmsg "Server monitor started on port $smPort";

my $waitedpid = 0;
my $paddr;

sub REAPER
{
  $waitedpid = wait;
  $SIG{CHLD} = \&REAPER;
  logmsg "reaped $waitedpid" . ($? ? " with exit $?" : "");
}
$SIG{CHLD} = \&REAPER;


&loadConfig( $configFile );



my $passwordFile = "$demoScriptHome/password.txt";

my $int_msg = "Interrupted system call";

logmsg "Waiting for clients...";

INFINITE: for (;;)
{
  my $client;

  while (!($paddr = accept($client, Server)))
  { last INFINITE unless $! eq $int_msg; }

  my($port, $iaddr) = sockaddr_in($paddr);
  my $name = gethostbyaddr($iaddr, AF_INET);

  logmsg "connection from $name [", inet_ntoa($iaddr), "] at port $port";

  spawn($client);

  logmsg "closing connection to $name";
  close $client;
}

logmsg "Terminating: $!";


sub spawn
{
  my $clientSocket = shift;

  die "spawn: no client socket: $!\n" unless $clientSocket;

  my $pid;
  if (!defined($pid = fork))
  {
    logmsg "cannot fork: $!";
    exit;
  }
  elsif ($pid)
  {
    logmsg "begat $pid";
    return $pid;  # This is the parent.
  }
# else this is the child

  my (%PID, %running_time);

  foreach (keys %processIdentifier) {

    if ( $DEBUG ) {
      logmsg "## processIdentifier key '$_' (value '$processIdentifier{ $_ }') present...\n";
    }

    $PID{$_} = "";
    $running_time{$_} = "Not running";
  }

  my @processInformation = `ps -ef`;

  for my $i (1 .. $#processInformation) {

    if ( $DEBUG ) {
      logmsg "## processInformation: $processInformation[$i]\n";
    }

    my ($blank, $pid, $time, $command);
    $processInformation[$i] =~ s/^\s+//;
    ($blank, $pid, $blank, $blank, $blank, $blank, $time, $command) =
      split /\s+/, $processInformation[$i], 8;

    if ( $DEBUG ) {
      logmsg "\n\n## command: '$command'\n\n";
    }

    foreach (keys %PID) {
      $PID{$_} = $pid if ($command =~ /$processIdentifier{$_}/);
      $running_time{$_} = $time if ($pid eq $PID{$_});

      if ( $command =~ /$processIdentifier{ $_ }/ )
      {
	logmsg "## found matching processid key '$_'\n";
      }
    }
  }


  my $command = ReceiveFrom($clientSocket);
  logmsg "\n\n## received initial command = '$command'";
  my $message = "";
  my $password_accepted = 0;

  while (substr($command, 0, 4) ne "quit")
  {
    $message = "Operation successful";
    my $exit = 0;

    if ($command eq "getProcessInformation")
    {
      logmsg "getProcessInformation";

      $message = "";
      foreach (sort keys(%PID))
      { $message .= "$_|$port{$_}|$PID{$_}|$running_time{$_}\n"; }

      logmsg "getProcessInformation finished";
    }
    elsif (substr($command, 0, 4) eq "kill")
    {
      if ( $DEBUG )
	{
	  logmsg "## received kill command '$command'";
	}
      if ($password_accepted == 0)
      {
      if ( $DEBUG )
	{
	  logmsg "## password NOT ACCEPTED!!!\n";
	}

        $message = "No password supplied";
        $exit = 1;
      }
      elsif (length($command) < 6)
      {
        $message = "Server to kill not specified."
      }
      else
      {
        my $name = substr($command, 5);
        if (exists $PID{$name} && $running_time{$name} ne "Not running")
        {
#          logmsg "kill -9 $PID{$name}: $name";
#          system "kill -9 $PID{$name}";
	  logmsg "killing $name: command is '$kill{ $name }'...";
	  system "$kill{ $name }";
        }
        else
        {
          $message = "Could not kill '$name'";
          logmsg $message;
        }
      }
    }
    elsif (substr($command, 0, 5) eq "start")
    {

      if ($DEBUG) {
	logmsg "## start command: $command\n";
      }

      if ($password_accepted == 0)
      {
        $message = "No password supplied";
        $exit = 1;
      }
      elsif (length($command) < 7)
      {
        $message = "Server to start not specified."
      }
      else
      {
        my $name = substr($command, 6);
        if (exists $PID{$name} && $running_time{$name} eq "Not running")
        {
          logmsg "start $name";

	  if ( $DEBUG ) {
	    logmsg "## starting with cmd '$start{$name}'...\n";
	  }
          system "$start{$name}";
        }
      }
    }
    elsif (substr($command, 0, 4) eq "pass")
    {
      if (length($command) < 6)
      {
        $message = "Password not specified.";
        $exit = 1;
      }
      else
      {
	if ( $DEBUG ) {
	  print STDERR "## accessing password file '$passwordFile'...\n";
	}

        open IN, $passwordFile or die "Can't open $passwordFile for input: $!";
        my $password = <IN>;
        chomp $password;

	if ( $DEBUG ) {
	  print STDERR "## read password '$password'...\n";
	}
        close IN;

        if ($password eq substr($command, 5)) 
        {
          $password_accepted = 1;
          logmsg "Password accepted";
        }
        else
        {
          $message = "Password not accepted";
          $exit = 1;
          logmsg $message;
        }

	if ( $DEBUG ) {
	  print "## done checking password.\n";
	}
      }
    }
    else
    {
      $message = "Unrecognized command: '$command'";
      $exit = 1;
    }

    SendTo($clientSocket, $message);
    last if $exit;

    $command = ReceiveFrom($clientSocket);
    logmsg "next command = '$command'";
  }

  exit;
}


sub SendTo#($sock, $message)
{
  my $sock = shift;
  my $message = shift;

  logmsg "Sending '$message' (" . length($message) . ")";

  send $sock, pack("N", length $message), 0;
  send $sock, $message, 0;
}


sub ReceiveFrom
{
  my($sock) = $_[0];
  my($length, $msg, $message, $received);

  $received = 0;
  $message = "";
  while ($received < 4)
  {
    recv $sock, $msg, 4 - $received, 0;
    $received += length $msg;
    $message .= $msg;
  }

  $length = unpack("N", $message);

  $received = 0;
  $message = "";

  while ($received < $length)
  {
    recv $sock, $msg, $length - $received, 0;
    $received += length $msg;
    $message .= $msg;
  }

  logmsg "ReceiveFrom: recvd msg ($length) '$message'";
  return $message;
}



# Register your process by filling in the %start and %processIdentifier
# hashes.
# - The %start hash's keys are process names, and its values are command lines
#   that start the associated process executing.  It is assumed that each
#   command starts the desired process in the background.


sub loadConfig
{
  my $configFile = shift;

  my $twig = XML::Twig->new();
  $twig->parsefile( $configFile );

  if ( !$twig ) {
    croak "ERROR: $0: couldn't read config file '$configFile'.\n";
  }

  my $root = $twig->root;  # 'demos'

#  $hostName = $root->{ 'att' }->{ "$HOST_TAG" };
  $demoScriptHome = $root->{ 'att' }->{ "$DEMO_SCRIPT_HOME_TAG" };
  my @demos = $root->children( "$DEMO_TAG" );
  
  my $numHosts = 0;

  foreach my $demo ( @demos ) {

    my $demoKey = $demo->{ 'att' }->{ "$KEY_ATT" };
    my $demoHost = $demo->{ 'att' }->{ "$HOST_ATT" };

    print STDERR "## hostName: '$hostName'; demo host: '$demoHost'...\n";

    if ( $demoHost =~ /$hostName/ ) {
      $numHosts++;

      my $demoName = $demo->{ 'att' }->{ "$NAME_ATT" };
	
      my $componentGroup = $demo->first_child( "$COMPONENT_GROUP_TAG" );
      my $demoHomePath = $componentGroup->{ 'att'}->{ "$HOME_PATH_ATT" };
      
      if ( !$demoHomePath ) {
	$demoHomePath = $demoScriptHome;
      }

      my $kill = $componentGroup->{ 'att' }->{ "$KILL_SCRIPT_ATT" };
      
      if ( $kill ) {
	$kill = "$demoHomePath/$kill";
      }
      
      my @components = $componentGroup->children( "$COMPONENT_TAG" );
      
      foreach my $component ( @components ) {
       
	my $componentName = $component->{ 'att' }->{ "$NAME_ATT" };
	my $startScript = $component->first_child( "$START_SCRIPT_TAG" )->text;
	
	if ( !$startScript ) {
	  croak "ERROR: $0: no startscript value specified for demo $demoName, "
	    . "component $componentName\n";
	}
	
	my $portNode = $component->first_child( "$PORT_TAG" );
	my $portVal;
	my $port;
	my $portType;

	if ( defined( $portNode ) ) {
	  $portType = $portNode->{ 'att'}->{ "$PORT_TYPE_ATT" };
	  $port = $portNode->text;
	  $portVal = &getPort( $portType, $port );
	}

	if ( $DEBUG ) {
	  print STDERR "## demoName '$demoName'; port type '$portType';"
	    ." port: '$port'; portVal: '$portVal'\n";
	}

	my $args;
	my $argNode = $component->first_child( "$ARGS_TAG" );
	
	if ( $argNode ) {
	  $args = $argNode->text;
	}
	
	if ( !defined( $demoHomePath ) ) {
	  $demoHomePath = $demoScriptHome;
	}
	
	my $startCmd = "$demoHomePath/$startScript $portVal $args";
	
	if ( $port !~ /^\d+$/ ) { # env var as port means start script does
	  #not take port argument
	  $startCmd = "$demoHomePath/$startScript $args";
	}
	
	my $identifierNode = $component->first_child( "$ID_TAG" );
	my $id = $identifierNode->text;
	my $needsPort = $identifierNode->{ 'att' }->{ "$NEED_PORT_ATT" };
	my $idString = &getIdentifierString( $id, $needsPort, $portVal );
	
	my $killNode = $component->first_child( "$KILL_CMD_TAG" );
	
	$kill = &buildKillCmd( $killNode, $kill, $needsPort, $portVal, $idString );
	
	if ( !$kill ) {
	  croak "ERROR: no kill command provided for demo '$demoName'.\n";
	}
	
	$componentName = "$demoName: $componentName";
	
	$start{ $componentName } = $startCmd;
	$port{ $componentName } = $portVal;
	$kill{ $componentName } = $kill;
	$processIdentifier{ $componentName } = $idString;
	
	print STDERR "## $componentName: start '$startCmd'; port '$port';"
	  ." kill '$kill'; identifier '$idString'.\n";
      }
    }
  }

  if ( 1 > $numHosts ) {
    croak "$0 @ARGV: ERROR: no services use $hostName as their host server.\n";
  }
}


sub getPort
{
  my ($portType, $portVal) = @_;

  my $port = $portVal;

  if ( defined( $portType ) ) {
    if ( "$ENV_LABEL" eq $portType ) {
      $port = $ENV{ $portVal };
      if ( !defined( $port ) ) {
	croak "ERROR: port env var '$portVal' is not defined.\n";
      }
    }
  }

  return $port;
}


sub getIdentifierString
{
  my $idString = shift;
  my $needsPort = shift;
  my $port = shift;

  if ( "$TRUE_VAL" eq $needsPort ) {

    $idString .= ( $idString =~ /\*$/ ? "$port" : " $port" );
  }

#  $idString =~ s/\./\\\./g;

  print STDERR "## idString: '$idString'\n";

  return $idString;
}


sub buildDefaultKillCmd
{
  my $idString = shift;

  my $killCmd = "pkill -f '$idString'";

  return $killCmd;
}

sub buildKillCmd
{
  my $killNode = shift; # a twig
  my $kill = shift; #if non-null, is group kill cmd
  my $needsPort = shift;
  my $portVal = shift;
  my $idString = shift;

  my $killNodeText = "";
  if ( $killNode ) {
    print STDERR "## found kill node...\n";
    $killNodeText = $killNode->text;
  }

  print STDERR "## killNodeText: '$killNodeText'\n";

  if ( $killNodeText eq "$AUTO_VAL" ) {
    $kill = &buildDefaultKillCmd( $idString );
  }
  elsif ( $killNodeText ) {
    $kill = "$killNodeText";
    if ( $needsPort ) {
      $kill .= " $portVal";
    }
  }

  return $kill;
}


sub loadList
{
  my $listFile = shift;
  my $listRef = shift;

  open ( LIST, "<$listFile" ) or die "$0: can't open list file '$listFile': $!\n";

  while ( <LIST> ) {
    chomp;
    
    if ( /(\S+)\s+(\S+)/ ) {
      my $demoKey = $1;
      my $demoHost = $2;


      if ( $DEBUG ) {
	print STDERR "## $0: demoKey is $demoKey; demoHost is $demoHost; hostName is $hostName\n";
      } 
      if ( $demoHost eq $hostName ) {
	
	$listRef->{ $demoKey } = 1;
      }
    }
    elsif ( $_ !~ /^\s*$/ ) {
      croak "ERROR: $0::loadList(): demo list file has wrong format "
	."(each line should be DEMO_KEY DEMO_HOST)\n";
    }
  }

  return;
}
