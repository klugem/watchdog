#!/usr/bin/env perl
use strict;
use warnings;

use File::Basename qw(dirname);
use File::Path qw(mkpath);
use List::Util qw(min max);
use lib dirname (__FILE__).'/../../core_lib/perl';

use IO::Handle;
use Watchdog::ExitCode qw(exitCode);
use Getopt::Long::Descriptive;
 
# the script reads a gtf file and extracts some relevant information and stores it in a table based format
# it also calculates how many bp per gene encode for exons (overlaping stuff is not counted twice)

my ($opt, $usage) = describe_options(
'gtf2info.pl %o',
[ 'gtf|g=s',		"path to the GTF file", { required => 1 }],
[ 'output|o=s',		"path to the output file", { required => 1 }],
[ 'returnfilepath|r=s', "path to the return variables file", { required => 1 }], 
[ 'help',		"print usage message and exit" ]
);
print($usage->text), exitCode("EXIT_OK") if $opt->help;

(my $filenameGTF = $opt->gtf) =~ s/['"]//g;
(my $filenameOUT = $opt->output) =~ s/['"]//g;
(my $returnFilePath = $opt->returnfilepath) =~ s/['"]//g;

	
open(my $fh_gtf, '<:encoding(UTF-8)', $filenameGTF) or die "Could not open file '$filenameGTF'!";

if(!-d $filenameOUT) {
	mkpath(dirname($filenameOUT));
}
open(my $fh_out, '>:encoding(UTF-8)', $filenameOUT) or die "Could not open file '$filenameOUT'!";
open(my $fh_out_exons, '>:encoding(UTF-8)', "$filenameOUT.exons") or die "Could not open file '$filenameOUT.exons'!";

print "GTF file: $filenameGTF\n";
print "Output file: $filenameOUT\n";
print "Output exon file: $filenameOUT.exons\n";
 
############################################
print "started parsing of gtf file...\n";

my %genes;
my %exons;
my %transcripts;
my $line;
my @tmp;
my $feature;
my $chr;
my $source;
my $start;
my $end;
my $strand;
my $attribute;
my $gene_id;
my $transcript_id;
my $gene_name;
my $gene_biotype;
my $exon_id;

while ($line = <$fh_gtf>) {
	chomp $line;
	
	# skip comment lines
	if($line =~ m/^#/) {
		next;
	}
	# parse the format
	@tmp = split("\t", $line);
	if(scalar(@tmp) == 9) {
		$feature = $tmp[2];

		# only these two entries are interesting for us
		if($feature eq "gene" || $feature eq "exon") {
			$chr = $tmp[0];
			$source = $tmp[1];
			$start = $tmp[3];
			$end = $tmp[4];
			$strand = $tmp[6];
			$attribute = $tmp[8];

			# find the additional information
			if($attribute =~ m/gene_id "([^"]+)";/) {
				$gene_id = $1;	
			}
			else {
				print "Can not find required attribute 'gene_id' in line '$line'!\n";
				exit 1;
			}
			if($attribute =~ m/gene_name "([^"]+)";/) {
				$gene_name = $1;	
			}
			else {
				print "Can not find required attribute 'gene_name' in line '$line'!\n";
				exit 1;
			}
			if($attribute =~ m/gene_(type|biotype) "([^"]+)";/) {
				$gene_biotype = $2;	
			}
			else {
				print "Can not find required attribute 'gene_biotype' in line '$line'!\n";
				exit 1;
			}

			# new gene entry
			if($feature eq "gene") {
				# test if a gene with that ID is already there
				if(exists $genes{$gene_id}) {
					print "Gene with ID '$gene_id' is already there!\n";
					exit 1;
				}

				my @info = ($gene_id, $gene_name, $gene_biotype, $source, $chr, $start, $end, $strand);
				my @exons = ();
				$genes{$gene_id} = \@info;
				$exons{$gene_id} = \@exons;
				$transcripts{$gene_id} = {};

			}
			# another exon for a gene
			elsif($feature eq "exon") {
				# test if gene for that exon was already there
				if(!exists $genes{$gene_id}) {
					print "Gene with ID '$gene_id' was not read before!\n";
					exit 1;
				}

				# find additional transcript id
				if($attribute =~ m/transcript_id "([^"]+)";/) {
					$transcript_id = $1;	
				}
				else {
					print "Can not find required attribute 'transcript_id' in line '$line'!\n";
					exit 1;
				}
				# find additional exon id
				if($attribute =~ m/exon_id "([^"]+)";/) {
					$exon_id = $1;	
				}
				else {
					print "Can not find required attribute 'exon_id' in line '$line'!\n";
					exit 1;
				}
				# add the exon
				push(@{$exons{$gene_id}}, [$exon_id, $chr, $start, $end, $strand, $transcript_id]);

				# add transcript id
				if(!exists $transcripts{$gene_id}{$transcript_id}) {
					$transcripts{$gene_id}{$transcript_id} = 0;
				}
				$transcripts{$gene_id}{$transcript_id}++;
			}
		}
	}
	else {
		print "Invalid formated line: '$line'!\n";
		exit 1;
	}
}
print "finished parsing of gtf file!\n";
############################################

############################################
print "started writing information in output file...\n";
# write header
print $fh_out "Geneid\tname\ttype\tsource\tchr\tstart\tend\tstrand\texon_length\texon_number\ttranscript_ids\n";
print $fh_out_exons "Exonid\tchr\tstart\tend\tstrand\ttranscript_id\tgene_id\n";
my $iw = 0;
my @data;
my @cor;
my $i = 0;
my $tid = "";
my $cstart = 0;
my $cend = 0;
my $lstart = 0;
my $lend = 0;
my $lastUnaddedStart = 0;
my $maxUnaddedEnd = 0;
my $add = 0;
my $length = 0;
my $nExons = -1; # because one element more is added

foreach $gene_id (sort keys %genes) {
	# print the basic data
	@data = @{$genes{$gene_id}};
	print $fh_out join("\t", @data)."\t";

	# sort the exons in ascending order
	@data = sort {$a->[2] <=> $b->[2] || $a->[3] <=> $b->[3]} @{$exons{$gene_id}};
	$add = 0;
	$length = 0;
	$nExons = -1;
	$lastUnaddedStart = 0;
	$maxUnaddedEnd = 0;
	for($i = 0; $i <= scalar(@data); $i++) {
		if($i < scalar(@data)) {
			# get start of current element
			$cstart = $data[$i][2];
			$cend = $data[$i][3];

			# write exon data to file
			print $fh_out_exons join("\t", @{$data[$i]})."\t".$gene_id."\n";
		}
		else {
			# enforce add of the last bin
			$cstart = $maxUnaddedEnd+100;
		}

		# skip the same elements
		if($i > 0 && ($lstart == $cstart && $lend == $cend)) {
			next;
		}

		# increase counter
		$add++;

		# test, if it overlaps with the last one
		if($i > 0) {
			# both exons do not overlap --> add length
			if(!($cstart <= $maxUnaddedEnd)) {
				$add = 0;	
			}
		}
		else {
			$lastUnaddedStart = $cstart;
			$maxUnaddedEnd = $cend;
		}

		# add the length
		if($add == 0) {
			$length = $length + ($maxUnaddedEnd - $lastUnaddedStart + 1);
			$lastUnaddedStart = $cstart;
			$maxUnaddedEnd = $cend;
		}
		else {
			$maxUnaddedEnd = max($maxUnaddedEnd, $cend);
		}

		# save the information of the last one
		$lstart = $cstart;
		$lend = $cend;
		$nExons++;
	}
	print $fh_out $length."\t".$nExons."\t";

	# print transcript info
	@data = ();
	foreach $tid (keys %{$transcripts{$gene_id}}) {
		push(@data, $tid.":".$transcripts{$gene_id}{$tid});
	}
	print $fh_out join(";", @data);
	print $fh_out "\n";
	$iw++;
}
print "finished  writing information in output file ($iw entries)!\n";
############################################

# close file handles
close($fh_out);
close($fh_out_exons);
close($fh_gtf);

writeReturnValues($filenameOUT);
exitCode("EXIT_OK");

#############################################################
sub writeReturnValues {
	my $out = $_[0];
	open(my $fh_outVars, '>:encoding(UTF-8)', $returnFilePath) or do { print "Could not open return param file '$returnFilePath'!\n"; exitCode("EXIT_RETURN_PARAMETER_MISSING"); };
	print $fh_outVars "geneInfoFile\t$out\n";
	print $fh_outVars "?EOF!\n";
	$fh_outVars->autoflush;
	close($fh_outVars); 
}
