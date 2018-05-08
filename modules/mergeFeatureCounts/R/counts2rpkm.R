# load some libs
library(getopt)
library(edgeR)

# options to parse
spec <- matrix(c('countFile', 'a', 1, "character",
		  'featureAnnotation', 'b', 1, "character",
		  'featureAnnotationID', 'c', 1, "character",
		  'featureAnnotationExonLength', 'd', 1, "character",
		  'outFolder', 'e', 1, "character",
		  'confirmRun2EndFile', 'g', 1, "character"), ncol=4, byrow=T)

# parse the parameters
opt = getopt(spec);
# we do no more checking for arguments because we expect that all checking is done before!

# create a folder if not already existing
outputFolderPrefix <- paste(dirname(opt$countFile), "/normalized/", sep="")
dir.create(outputFolderPrefix, showWarnings = F, recursive = T)
name <- unlist(strsplit(basename(opt$countFile), "\\."))[1]

# read in the data
countsRaw <- read.table(opt$countFile, head=T, as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
features <- read.table(opt$featureAnnotation, head=T, as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)

m <- merge(countsRaw, features, by.x="FeatureID", by.y=opt$featureAnnotationID)
length <- m[, opt$featureAnnotationExonLength]

# normalize it
rownames(countsRaw) <- countsRaw[, "FeatureID"]
r <- as.data.frame(rpkm(countsRaw[, seq(2, ncol(countsRaw))], length, prior.count=0.25, log=T))
c <- as.data.frame(cpm(countsRaw[, seq(2, ncol(countsRaw))], prior.count=0.25, log=T))
nr <- as.data.frame(rpkm(countsRaw[, seq(2, ncol(countsRaw))], length, log=F))
nc <- as.data.frame(cpm(countsRaw[, seq(2, ncol(countsRaw))], log=F))

n <- colnames(r)
nr[opt$featureAnnotationID] <- rownames(nr)
nc[opt$featureAnnotationID] <- rownames(nc)

# sort the stuff
ord <- order(apply(countsRaw[, seq(2, ncol(countsRaw))], 1, sum), decreasing = T)
countsRaw <- countsRaw[ord, ]
nr <- nr[ord, ]
nc <- nc[ord, ]

# add names and type
features <- features[, c("Geneid", "name", "type")]
nc <- merge(nc, features, by.x="row.names", by.y=opt$featureAnnotationID, sort = F)
nr <- merge(nr, features, by.x="row.names", by.y=opt$featureAnnotationID, sort = F)
countsRaw <- merge(countsRaw, features, by.x="row.names", by.y=opt$featureAnnotationID, sort = F)

# write it
write.table(nr[, c(opt$featureAnnotationID, "name", "type", n)], file=paste(outputFolderPrefix, name, ".rpkm", sep=""), row.names=F, col.names=T, quote=F, sep="\t")
write.table(nc[, c(opt$featureAnnotationID, "name", "type", n)], file=paste(outputFolderPrefix, name, ".cpm", sep=""), row.names=F, col.names=T, quote=F, sep="\t")
write.table(countsRaw[, c("FeatureID", "name", "type", n)], file=paste(outputFolderPrefix, name, ".anno.counts", sep=""), row.names=F, col.names=T, quote=F, sep="\t")

# make some statistics

pdf(paste(outputFolderPrefix, "/", name , "_rpkm_cpm.pdf", sep=""))
for(i in seq(1, length(n))) {
	if(sum(r[, i], na.rm=T) != 0)
	{
		hist(r[, i], breaks=100, xlab="log2(rpkm)", main=paste("Sample: ", n[i], sep=""), xlim=c(min(r[, i], na.rm=T), max(r[, i], na.rm=T)))
	}
}
hist(unlist(r), breaks=100, xlab="log2(rpkm)", main="all samples", xlim=c(min(r, na.rm=T), max(r, na.rm=T)))

for(i in seq(1, length(n))) {
	if(sum(c[, i], na.rm=T) != 0) {
		hist(c[, i], breaks=100, xlab="log2(cpm)", main=paste("Sample: ", n[i], sep=""), xlim=c(min(c[, i], na.rm=T), max(c[, i], na.rm=T)))
	}
}
hist(unlist(c), breaks=100, xlab="log2(cpm)", main="all samples", xlim=c(min(c, na.rm=T), max(c, na.rm=T)))
dev.off()
# write a file that we know, the script run to its end
if(!is.null(opt$confirmRun2EndFile)) {
	file.create(opt$confirmRun2EndFile, showWarnings = FALSE)
}

