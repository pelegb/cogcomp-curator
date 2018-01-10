#!/usr/bin/perl -w

use strict;


###
# PURPOSE:
#   allow a command to be run with an extra parameter, a unique identifier,
#     to be used by server_monitor to identify the process
#

die "Usage: $0 command [arguments] identifier"
  unless @ARGV >= 2;


my $id = pop @ARGV;


my $cmd = shift @ARGV;

my @argList = @ARGV;
unshift @argList, $id;

#while ( my $arg = shift @ARGV ) {
#  $cmdString .= " $arg";
#}

print STDERR "## $0: running command '$cmd' on args '@argList'...\n";

exec { $cmd } @argList;

die "ERROR: $0: failed to exec command '$cmd' with args '"
. join( " ", @argList ) . "': $!\n";
