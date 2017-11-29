# load some libs
library(getopt)

# options to parse
spec <- matrix(c('bamMergedStats', 'a', 1, "character",
		  'outputFile', 'b', 1, "character",
		  'confirmRun2EndFile', 'c', 1, "character"), ncol=4, byrow=T)

# parse the parameters
opt = getopt(spec);
# we do no more checking for arguments because we expect that all checking is done before!

bamstatsPath <- opt$bamMergedStats
outputPath <- opt$outputFile

stats <- read.csv(bamstatsPath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)

# order the stuff correctly...
n <- suppressWarnings(as.numeric(sub("chr", "", stats$contigt)))
n[is.na(n)] <- 999999
stats <- stats[order(n), ]
start <- suppressWarnings(which(is.na(as.numeric(sub("chr", "", stats$contigt))))[1])
first <- stats[seq(1, start-1), ]
second <- stats[seq(start, nrow(stats)), ]
# rename some specific stuff
second[second$contigt == "chrM", "contigt"] <- "chrZM"
second[second$contigt == "chrRDNA", "contigt"] <- "chrZRDNA"
second[second$contigt == "*", "contigt"] <- "chrZZ"
second[second$contigt == "mcmv_genome_lars", "contigt"] <- "MCMV"
# order it	
second <- second[order(second$contigt), ]
# rename it back
second[second$contigt == "chrZM", "contigt"] <- "chrM"
second[second$contigt == "chrZRDNA", "contigt"] <- "R-DNA"
second[second$contigt == "chrZZ", "contigt"] <- "*"
# merge it again!
stats <- rbind(first, second)
correctOrder <- unique(stats$contigt)

# get sample names
samples <- sort(unique(stats$sample))

pdf(file = outputPath)
for(i in seq(1, (length(samples)+1))) {
	# get data for one file
	if(i <= length(samples)) {
		s <- samples[i]
		data <- stats[stats$sample == s, ]
	}
	# get data for all files
	else {
		s <- "all together"
		stripped <- as.data.frame(stats[, c("mapped", "unmapped", "contigt")])
		data <- aggregate(cbind(mapped, unmapped) ~ contigt, stripped, FUN = "sum")
		data <- data[order(correctOrder), ]

		x <- data.frame()
		for(ii in seq(1, length(correctOrder))) {
			x <- rbind(x, data[data$contigt == correctOrder[ii], ])
		}
		data <- x
	}

	# reformat the stuff
	d <- as.data.frame(data[, c("mapped", "unmapped", "contigt")])
	rownames(d) <- d$contigt
	d <- d[, c("mapped", "unmapped")]
	sum <- sum(d)
	max <- max(d$mapped + d$unmapped)
	percentageMax <- max((d$mapped + d$unmapped) / sum)

	# plot raw reads
	par(mar=c(5,6,5,5))
	barplot(t(as.matrix(d)), col=c("green", "red"), las=2)
	mtext("#alignment count", side=2, line=3, cex.lab=1,las=3, padj=-2.5)
	legend("top", colnames(d), fill=c("green", "red"), ncol=2, inset=c(0,-0.14), xpd=T, title=paste("Sample:", s))

	# plot percentage axis
	labs <- seq(0, percentageMax, length.out = 10)
	scale <- 1/labs[10]
	axis(side = 4, at = scale*max*labs, labels = paste0(round(labs * 100, digits = 2), "%"), las=2)
	mtext("percent", side=4, line=3, cex.lab=1,las=3, padj=1.5)

}

# end PDF plot
dev.off()

# write a file that we know, the script run to its end
if(!is.null(opt$confirmRun2EndFile)) {
	file.create(opt$confirmRun2EndFile, showWarnings = FALSE)
}


