# load some libs
library(getopt)
library(RColorBrewer)
library(gplots)

validMethods <- c("all","limma", "edgeR", "DESeq2", "DESeq")

# options to parse
spec <- matrix(c('controlCondition', 'a', 1, "character",
		  'testCondition', 'b', 1, "character",
		  'countFile', 'c', 1, "character",
		  'sampleAnnotation', 'd', 1, "character",
		  'featureAnnotation', 'e', 1, "character",
		  'featureAnnotationID', 'f', 1, "character",
		  'featureAnnotationType', 'g', 1, "character",
		  'excludeSamples', 'h', 1, "character",
		  'pValueCutoff', 'i', 1, "numeric",
		  'minKeepReads', 'j', 1, "numeric",
		  'foldchangeCutoff', 'k', 1, "character",
		  'foldchangeCutoffNames', 'l', 1, "character",
		  'method', 'm', '1', "character",
		  'output', 'n', 1, "character",
		  'installDir', 'o', 1, "character",
		  'confirmRun2EndFile', 'p', 1, "character"), ncol=4, byrow=T)

# parse the parameters
opt = getopt(spec)

# we do no more checking for arguments because we expect that all checking is done before!
# but ensure, that method is not manipulated
if(is.null(opt$method) || !opt$method %in% validMethods) 
{
	print(paste("[ERROR] Only methods '", paste(validMethods, collapse = "', '"), "' are valid!", sep=""))
	quit(save = "no", status = 14, runLast = FALSE) # status = 14 <--> invalid arguments
}

# change working directory for sourcing of files
setwd(opt$installDir)

opt$foldchangeCutoff <- as.numeric(strsplit(opt$foldchangeCutoff, ",")[[1]])
opt$foldchangeCutoffNames <- c(strsplit(opt$foldchangeCutoffNames, ",")[[1]])
opt$foldchangeCutoffNames <- opt$foldchangeCutoffNames[order(opt$foldchangeCutoff)]
opt$foldchangeCutoff <- sort(opt$foldchangeCutoff)

vs <- paste(opt$testCondition, opt$controlCondition, sep="_")
# excludeSamples some of the samples.
if(!is.null(opt$excludeSamples)) 
{
	opt$excludeSamples <- sort(unlist(strsplit(opt$excludeSamples, ",")))
}

# build expressionSet
exprs <- as.matrix(read.table(opt$countFile, header=TRUE, sep="\t", row.names=1, as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE))
pData <- read.table(opt$sampleAnnotation, row.names=1, header=TRUE, sep="\t", stringsAsFactors = FALSE)
features <- NULL
if(!is.null(opt$featureAnnotation)) 
{
	features <- read.table(opt$featureAnnotation, header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
}

# remove samples, if wished
if(!is.null(opt$excludeSamples)) 
{
	opt$excludeSamples <- opt$excludeSamples[pData[opt$excludeSamples, ] == opt$controlCondition | pData[opt$excludeSamples, ] == opt$testCondition]
	exprs <- exprs[, ! colnames(exprs) %in% opt$excludeSamples]
	vs <- paste(vs, "_excludeSamples_", paste(opt$excludeSamples, collapse="_"), sep="")
}

# later for DE gene test testCondition / controlCondition is computed though controlCondition should be control
opt$output <- paste(opt$output, "/", vs, sep="")
dir.create(opt$output, showWarnings = F, mode = "0755")
# removed not used rows
pDataTmp <- pData
pDataTmp$sample <- rownames(pDataTmp)
pDataTmp <- pDataTmp[pDataTmp$condition == opt$controlCondition | pDataTmp$condition == opt$testCondition, ]

pData <- as.data.frame(pDataTmp[, "condition"])
rownames(pData) <- pDataTmp$sample

S1 <- pDataTmp[pDataTmp$condition == opt$controlCondition, "sample"]
S2 <- pDataTmp[pDataTmp$condition == opt$testCondition, "sample"]
exprs <- exprs[, colnames(exprs) %in% c(S1, S2)]
colnames(pData) <- "condition"
pData$condition <- droplevels(pData$condition)

pData <- as.data.frame(pData[colnames(exprs), ])
rownames(pData) <- colnames(exprs)
colnames(pData)[1] <- "condition"

if(ncol(exprs) == 0) 
{
	print(paste("[ERROR] No samples are left after merging with sample annotation. Probably names of the sample annotation do not match the names of the count file.", sep=""))
	quit(save = "no", status = 14, runLast = FALSE) # status = 14 <--> invalid arguments
}

# make a statistic, if we have a annotation file if a column named type
if(!is.null(features)) {
	if(!(opt$featureAnnotationID %in% colnames(features))) {
		print(paste("[ERROR] No colum with name '", opt$featureAnnotationID, "' found in feature annotation file.", sep=""))
		quit(save = "no", status = 14, runLast = FALSE) # status = 14 <--> invalid arguments
	}

	if(opt$featureAnnotationType %in% colnames(features)) {
		pdf(paste(opt$output, "/", "countDistribution.pdf",  sep=""))
		for(i in seq(0, ncol(exprs))) {
			if(i == 0) {
				sample <- "all"
				# get sum of all counts
				sumCounts <- as.data.frame(apply(exprs, 1, sum))
			}
			else {
				sample <- colnames(exprs)[i]
				sumCounts <- as.data.frame(exprs[, i])
			}

			colnames(sumCounts) <- c("counts")
			stats <- merge(features, sumCounts, by.x=opt$featureAnnotationID, by.y=0)
			# try to remove numeric suffixs if there 
			if(nrow(stats) == 0) {
				features[, opt$featureAnnotationID] <- gsub("\\.[0-9]+$", "", features[, opt$featureAnnotationID])
				stats <- merge(features, sumCounts, by.x=opt$featureAnnotationID, by.y=0)

				if(nrow(stats) == 0) {
					print(paste("[ERROR] Feature annotation seems not to match count file.", sep=""))
					quit(save = "no", status = 14, runLast = FALSE) # status = 14 <--> invalid arguments
				}
			}
	
			# make statistics
			data <- aggregate(stats$counts, by=list(stats[, opt$featureAnnotationType]), FUN=sum)
			colnames(data) <- c("type", "counts")
			data$per <- round(data$counts / sum(data$counts) * 100, digits = 2)
			# sort it
			data <- data[order(-data$counts), ]

			# just keep all greater than 0,5
			stripped <- NULL
			stripped <- data[data$per >= 0.5, ]
			other <- list("other", sum(data[data$per < 0.5, 'counts']), round(100-sum(data[data$per >= 0.5, 'per']), digits = 2))
			stripped <- rbind(stripped, other)
			n <- nrow(stripped)
			color <- rainbow(n)
			lengthLabels <- nchar(stripped$type)
			stripped$type <- substr(stripped$type, 0, 20)
			stripped$type[lengthLabels > 20] <- paste(stripped$type[lengthLabels > 20], "...", sep="")
			stripped$typePer <- paste(stripped$type, " (", stripped$per, "%)", sep="")
			labels <- stripped[stripped$per >= 1, 'type']
			labels <- c(labels, rep(NA, n-length(labels)))

			labels <- sub("_", "\\\\_", labels)
			#stripped$typePer <- sub("_", "\\\\_", stripped$typePer)
			#stripped$typePer <- sub("%", "\\\\%", stripped$typePer)
			pie(stripped$counts / sum(stripped$counts), col=color, labels=NA, main=paste("Read count distribution of " , sample, " sample(s)", sep=""))
			legend("bottom", stripped$typePer, cex=1, fill=color,ncol=2, inset=c(0,-0.14), xpd=T)
		}
	dev.off()
	}
}

# remove nearly unexpressed features
keep <- rowSums(exprs) >= opt$minKeepReads*ncol(exprs)
exprs <- exprs[keep, ]

if(nrow(exprs) == 0) {
	print(paste("[ERROR] No gene passed the low expression filtering!", sep=""))
	quit(save = "no", status = 14, runLast = FALSE) # status = 14 <--> invalid arguments
}

perform <- c(opt$method)
if(opt$method == "all") {
	perform <- validMethods[seq(2, length(validMethods))]
}

for(currentMethod in perform) {
	pdf(paste(opt$output, "/", currentMethod, ".pdf",  sep=""))

	# backup the value
	pData.bak <- pData
	exprs.bak <- exprs
	features.bak <- features

	# perform the actual test
	print(paste("Executing ", currentMethod, "...", sep=""))
	source(paste(currentMethod, ".R", sep=""))

	# write results and create some plots
	source("writeResults.R")
	source("plotDE.R")

	# restore the backup
	pData <- pData.bak
	exprs <- exprs.bak
	features <- features.bak
}

# compare the methods
source("compareDEMethods.R")

# write a file that we know, the script run to its end
if(!is.null(opt$confirmRun2EndFile)) {
	file.create(opt$confirmRun2EndFile, showWarnings = FALSE)
}
