# load some libs
library(getopt)

# options to parse
spec <- matrix(c('countFile', 'a', 1, "character",
		  'featureAnnotation', 'b', 1, "character",
		  'featureAnnotationID', 'c', 1, "character",
        	  'featureAnnotationType', 'd', 1, "character",
		  'outFolder', 'e', 1, "character",
		  'bamMergedStats', 'f', 1, "character",
		  'confirmRun2EndFile', 'g', 1, "character"), ncol=4, byrow=T)

# parse the parameters
opt = getopt(spec);
# we do no more checking for arguments because we expect that all checking is done before!

countsRaw <- read.csv(opt$countFile, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
bamstats <- read.csv(opt$bamMergedStats, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
samples <- colnames(countsRaw)
features <- read.csv(opt$featureAnnotation, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)

# get mapping values
cmap <- aggregate(cbind(mapped) ~ sample, bamstats, FUN = "sum")

# merge it with sample annotation
counts <- merge(countsRaw, features, by.x="FeatureID", opt$featureAnnotationID)
mappedTotal <- 0

name <- unlist(strsplit(basename(opt$countFile), "\\."))[1]
pdf(file = paste(opt$outFolder, "/", name, ".pdf", sep=""))
for(i in seq(2, (length(samples)+2))) {
	# get data for one file
	if(i <= length(samples)) {
		s <- samples[i]
		mapped <- cmap[cmap$sample == s, "mapped"]
		data <- counts[, c("FeatureID", s, "name", opt$featureAnnotationType)]
		colnames(data)[2] <- "count"
		mappedTotal <- mappedTotal + mapped
	}
	# get data for all files
	else {
		s <- "all samples"
		data <- as.data.frame(cbind(countsRaw$FeatureID, as.numeric(apply(countsRaw[, seq(2, length(countsRaw))], 1, sum))), stringsAsFactors = F)
		colnames(data) <-  c("FeatureID", "count")
		data$count <- as.integer(data$count)
		data <- merge(data, features, by.x="FeatureID", by.y=opt$featureAnnotationID)
		mapped <- mappedTotal
	}

	# rename the type
	renameIDs <- which(colnames(data) == opt$featureAnnotationType)
	colnames(data)[renameIDs] <- "type"

	# get number of features with more than 10 counts
	minRawReads <- length(which(data$count >= 10))
	total <- max(sum(data$count), 1)

	# get features with more than 0.5% of total reads
	if(length(samples)+2 != i) {
		part <- 0.005
	}
	else {
		part <- 0.10
	}
	top <- data[data$count / total >= part, ]
	anteilTop <- round(sum(top$count) / total * 100, 1)

	# classify rest of the stuff using the type argument
	rest <- data[!rownames(data) %in% rownames(top), ]
	types <- aggregate(cbind(count) ~ type, rest, FUN = "sum")
	colnames(types) <- c("name", "count")
	types$type <- types$name
	topTypes <- types[types$count / total >= 0.005, ]

	oT <- droplevels(as.data.frame(t(as.data.frame(c(sum(types[types$count / total < 0.005, "count"]), "other types", "other types")))))
	rownames(oT) <- NULL
	colnames(oT) <- c("count", "name", "type")

	# plot the stuff
	all <- top[order(top$count, decreasing=T), c("count", "name", "type")]
	all <- rbind(all, topTypes[order(topTypes$count, decreasing=F), c("count", "name", "type")])
	all <- rbind(all, oT)
	all$count <- as.integer(all$count)

	par(mar=c(10,6,6,5))
	max <- max(all$count)
	t <- as.character(unique(all$type))
	col <- rainbow(length(t))
	all$col <- col[match(all$type, t)]
	barplot(all$count, las=2, ylim=c(0, max), names.arg=all$name, col=all$col)
	mtext("#assigned read count", side=2, line=3, cex.lab=1,las=3, padj=-1.5)
	# perAssigned <- round(total / mapped * 100, 1) <-- does not work because of paired end mode
	legend("top", t, fill=col, ncol=3, inset=c(0,-0.30), xpd=T, title=paste("File: ", s, "; rawRead >= 10: ", minRawReads, sep="")) #, "; assigned of mapped: ", perAssigned, "%",

	# plot percentage axis
	percentageMax <- max/total
	labs <- seq(0, percentageMax, length.out = 10)
	scale <- 1/labs[10]
	axis(side = 4, at = scale*max*labs, labels = paste0(round(labs * 100, digits = 1), "%"), las=2)
	mtext("percent", side=4, line=3, cex.lab=1,las=3, padj=1)
}

# end PDF plot
dev.off()

# write a file that we know, the script run to its end
if(!is.null(opt$confirmRun2EndFile)) {
	file.create(opt$confirmRun2EndFile, showWarnings = FALSE)
}

