# load some libs
library(getopt)
library(gtools)

# options to parse
spec <- matrix(c('statsFolder', 'a', 1, "character",
		  'outFile', 'b', 1, "character",
		  'confirmRun2EndFile', 'c', 1, "character"), ncol=4, byrow=T)

# parse the parameters
opt = getopt(spec)
# we do no more checking for arguments because we expect that all checking is done before!

# get parameters
fastqcPath <- paste(opt$statsFolder, '/Basic_Statistics.txt', sep="")
bamstatsPath <- paste(opt$statsFolder, '/Idxstats.txt', sep="")
flagstatsPath <- paste(opt$statsFolder, '/Flagstat.txt', sep="")
featurePath <- paste(opt$statsFolder, '/Assignment_statistic.txt', sep="")
cutadaptPath <- paste(opt$statsFolder, '/Cutadapt.txt', sep="")

cutadpat <- NULL
if(file.exists(cutadaptPath))
{
	cutadpat <- read.csv(cutadaptPath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
	cutadpat$sample <- gsub("\\.fastq$", "", cutadpat$sample)
	cutadpat$sample <- gsub("\\.fq$", "", cutadpat$sample)
}

# start PDF and read files
pdf(file = paste(opt$outFile, ".pdf", sep=""))
fastqc <- read.csv(fastqcPath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
bamstats <- read.csv(bamstatsPath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
flagstats <- read.csv(flagstatsPath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
featureCount <- read.csv(featurePath, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)

# get number of reads from fastqc file
fastqc <- fastqc[order(fastqc$Filename), ]
fastqc <- fastqc[fastqc[, "#Measure"] == "Total Sequences", c("Value", "Filename")]
colnames(fastqc)[1] <- "ReadNumber"
fastqc$ReadNumber <- as.numeric(fastqc$ReadNumber)
fastqc$Filename <-  gsub(".fastq", "", fastqc$Filename)

# check, if we have there a fake replicate ending with "PR[0-9]"
psydoReplicate <- grep(".PR[0-9]$", featureCount$Filename, perl=TRUE, value=FALSE)
if(length(psydoReplicate) > 0) {
	featureCount[psydoReplicate, "Filename"] <- gsub(".PR[0-9]$", "", featureCount[psydoReplicate, "Filename"])
	featureCount <- unique(featureCount)
}

# take the maximum in case of paired end reads
firstP <- grep("_R1(_.*)*$", fastqc$Filename, perl=TRUE, value=FALSE)
secondP <- grep("_R2(_.*)*$", fastqc$Filename, perl=TRUE, value=FALSE)

pe <- 0
if(length(firstP) == length(secondP) && length(firstP) > 0) {
	pe <- 1
	fastqc$Filename <- gsub("_R1$", "", fastqc$Filename)
	fastqc$Filename <- gsub("_R2$", "", fastqc$Filename)
	fastqc$Filename <- gsub("_R1_.*$", "", fastqc$Filename)
	fastqc$Filename <- gsub("_R2_.*$", "", fastqc$Filename)

	fastqc <- aggregate(cbind(ReadNumber) ~ Filename, fastqc, FUN = "max")

	if(!is.null(cutadpat)) 
	{
		cutadpat$sample <- gsub("_R1$", "", cutadpat$sample)
		cutadpat$sample <- gsub("_R2$", "", cutadpat$sample)
		cutadpat$sample <- gsub("_R1_.*$", "_R1", cutadpat$sample)
		cutadpat$sample <- gsub("_R2_.*$", "_R2", cutadpat$sample)
	}
}

# remove stuff from fc which is not needed 
featureCount <- featureCount[, c("Filename", "Assigned")]
cutD <- NULL
# get removed sequences from cutadapt if there
if(!is.null(cutadpat)) {
	cutD <- cutadpat[cutadpat$parameter == "Too short reads" | cutadpat$parameter == "Reads that were too short", c("value", "sample")]
	colnames(cutD) <- c("smallLength", "Filename")
	cutD$smallLength <- as.numeric(gsub(",","",cutD$smallLength))
	cutD <- aggregate(cbind(smallLength) ~ Filename, cutD, FUN = "sum") # WARN: might overestimate nuber of cut reads, if both mates are too short after trimming, but might be no problem because normally not the case!
}

# get values for contextMap
if(pe == 1) {
	# get the data
	ppaired <- flagstats[flagstats$event == "with itself and mate mapped", c("number", "sample")]
	r1_mapped <- flagstats[flagstats$event == "read1", c("number", "sample")]
	r2_mapped <- flagstats[flagstats$event == "read2", c("number", "sample")]

	# ensure the same order
	ppaired <- ppaired[order(ppaired$sample), ]
	r1_mapped <- r1_mapped[order(r1_mapped$sample), ]
	r2_mapped <- r2_mapped[order(r2_mapped$sample), ]

	# get r1 and r2 only
	cmap <- NULL
	cmap$R1_only <- r1_mapped$number - (ppaired$number/2)
	cmap$R2_only <- r2_mapped$number - (ppaired$number/2) 
	cmap$mapped <- unlist(lapply(((ppaired$number/2) - (cmap$R1_only + cmap$R2_only)), max, 0))
	cmap$mapped <- cmap$mapped - flagstats[flagstats$event == "secondary", "number"] 
	cmap$sample <- ppaired$sample
	cmap <- as.data.frame(cmap)
} else { # single end mapping mode
	cmap <- flagstats[flagstats$event == "mapped", c("number", "sample")] 
	cmap$number <- cmap$number - flagstats[flagstats$event == "secondary", "number"] 
	colnames(cmap)[1] <- "mapped"
}

# merge all together
data <- merge(fastqc, cmap, by.x="Filename", by.y="sample")
data <- merge(data, featureCount, by="Filename")
if(!is.null(cutD)) {
	data <- merge(data, cutD, by="Filename")
	data$ReadNumber <- data$ReadNumber + data$smallLength
}
data <- data[order(data$Filename), ]

# create a plot showing total lib sizes
par(mar=c(10,4.5,4,2))
barplot(data$ReadNumber, main="#raw input fragments per sample", names.arg=data$Filename, las=2)

# calculate percentage
c <- NULL
c$counted <- data$Assigned / data$ReadNumber
if(pe == 1) {
	mapped <- data$mapped + data$R1_only + data$R2_only
} else {
	mapped <- data$mapped
}
c$uniquely <- (mapped - data$Assigned) / data$ReadNumber
if(!is.null(cutadpat)) {
	c$smallLength <- data$smallLength / data$ReadNumber
	c$unmapped <- (data$ReadNumber - mapped - data$smallLength) / data$ReadNumber
} else {
	col <- NULL
	c$unmapped <- (data$ReadNumber - mapped) / data$ReadNumber
}
c <- t(as.matrix(as.data.frame(c)))
colnames(c) <- data$Filename

if(!is.null(cutadpat)) {
	rownames(c) <- c("assigned to feature", "mapped but not counted", "too short after trimming", "unmapped")
	col <- "pink"
} else {
	rownames(c) <- c("assigned to feature", "mapped but not counted", "unmapped")
}
c <- c * 100

# make the plot
par(mar=c(10,4.5,4,2))
c <- c[,mixedorder(colnames(c)) ]
col <- c("green", "blue", col, "red")
barplot(c, ylab="percent [%] of total reads", col=col, las=2)
legend("top", rownames(c), fill=col, ncol=3, inset=c(0,-0.14), xpd=T)

# satistic about the mapped data
if(pe == 1) {
	d <- NULL
	d$mapped <- data$mapped / data$ReadNumber * 100
	d$R1_only <- data$R1_only / data$ReadNumber * 100
	d$R2_only <- data$R2_only / data$ReadNumber * 100
	d <- t(as.matrix(as.data.frame(d)))
	colnames(d) <- data$Filename
	d <- d[,mixedorder(colnames(d)) ]
	par(mar=c(10,4.5,4,2))
	barplot(d, ylab="percent [%] of total reads", col=c("green", "yellow", "orange"), las=2, ylim=c(0, 100))
	legend("top", c("both mapped", "mate 1 only", "mate 2 only"), fill=c("green", "yellow", "orange"), ncol=3, inset=c(0,-0.14), xpd=T)
}
dev.off()
write.table(c, file=paste(opt$outFile, ".stats", sep=""), quote=FALSE, sep="\t", row.names=TRUE, col.names=TRUE)
write.table(data, file=paste(opt$outFile, ".stats2", sep=""), quote=FALSE, sep="\t", row.names=TRUE, col.names=TRUE)

# write a file that we know, the script run to its end
if(!is.null(opt$confirmRun2EndFile)) {
	file.create(opt$confirmRun2EndFile, showWarnings = FALSE)
}

