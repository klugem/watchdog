package Watchdog::ExitCode;
use strict;
use warnings;

use Exporter qw(import);
our @ISA =   qw(Exporter);
our @EXPORT = qw(exitCode);
use File::Basename;
use Cwd 'abs_path';
our %EXIT_CODES = ();

# name of the exit code how it is defined in the sh file
sub exitCode {
	my $name = $_[0];
	
	# check, if a exit code with that name is defined
	if(exists $EXIT_CODES{$name}) {
		exit $EXIT_CODES{$name};
	}
	else {
		exit 1;
	}
}

# read in the exitCodes.sh file
sub INIT() {
	my $BASE=abs_path(dirname(__FILE__));
	my $EXIT_CODE_FILE="$BASE/../../exitCodes.sh";
	open(my $fh, '<:encoding(UTF-8)', $EXIT_CODE_FILE) or die "Could not open file '$EXIT_CODE_FILE'!";
	while (my $row = <$fh>) {
		if($row =~ /([A-Z_]+)=([0-9]+)/) {
			$EXIT_CODES{$1}=$2;
		}
	}	
	close($fh);
}

1;
