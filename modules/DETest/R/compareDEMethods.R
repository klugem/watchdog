# we can only compare the results if all methods were applied
if(opt$method == "all") {
	# get infos about the annotation cols
	geneInfoCols <- c()
	if(!is.null(opt$featureAnnotation)) {
		geneInfoCols <- colnames(features)
		geneInfoCols <- geneInfoCols[!geneInfoCols %in% c(opt$featureAnnotationID)]
	}

	# change working dir
	setwd(opt$output)

	opt$foldchangeCutoffNames <- c("all", opt$foldchangeCutoffNames)
	opt$foldchangeCutoff <- c(0, opt$foldchangeCutoff)

	pdf("compareDE.pdf")
	for(i in 1:length(opt$foldchangeCutoffNames)) {
		type <- opt$foldchangeCutoffNames[i]
	 
		# load the data
		limma <- read.csv(paste('limma', type, "csv", sep="."), header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
		edgeR <- read.csv(paste('edgeR', type, "csv", sep="."), header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
		DESeq <- read.csv(paste('DESeq', type, "csv", sep="."), header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)
		DESeq2 <- read.csv(paste('DESeq2', type, "csv", sep="."), header=TRUE, sep="\t", as.is=TRUE, check.names = FALSE, stringsAsFactors = FALSE)

		# remove gene info 
		gl <- as.data.frame(limma[, colnames(limma) %in% c("ID", geneInfoCols)])
		gr <- as.data.frame(edgeR[, colnames(edgeR) %in% c("ID", geneInfoCols)])
		gd <- as.data.frame(DESeq[, colnames(DESeq) %in% c("ID", geneInfoCols)])
		gd2 <- as.data.frame(DESeq2[, colnames(DESeq2) %in% c("ID", geneInfoCols)])
		colnames(gl)[1] <- "ID"
		colnames(gr)[1] <- "ID"
		colnames(gd)[1] <- "ID"
		colnames(gd2)[1] <- "ID"
		gm <- rbind(gl, gr, gd, gd2)
		genes <- as.data.frame(gm[!duplicated(gm), ])
		colnames(genes)[1] <- "ID"

		limma <- limma[, !colnames(limma) %in% geneInfoCols]
		edgeR <- edgeR[, !colnames(edgeR) %in% geneInfoCols]
		DESeq <- DESeq[, !colnames(DESeq) %in% geneInfoCols]
		DESeq2 <- DESeq2[, !colnames(DESeq2) %in% geneInfoCols]

		# rename the columns
		colnames(limma)[seq(2, ncol(limma))] <- paste("limma", colnames(limma)[seq(2, ncol(limma))], sep="_")
		colnames(edgeR)[seq(2, ncol(edgeR))] <- paste("edgeR", colnames(edgeR)[seq(2, ncol(edgeR))], sep="_")
		colnames(DESeq)[seq(2, ncol(DESeq))] <- paste("DESeq", colnames(DESeq)[seq(2, ncol(DESeq))], sep="_")
		colnames(DESeq2)[seq(2, ncol(DESeq2))] <- paste("DESeq2", colnames(DESeq2)[seq(2, ncol(DESeq2))], sep="_")
		merged <- merge(merge(merge(limma, edgeR, by="ID", all=TRUE), DESeq, by="ID", all=TRUE), DESeq2, by="ID", all=TRUE)
		merged <- merge(merged, genes, by="ID", all.x = T)

		# make venn diagram
		m <- NULL
		m$limma <- rep(0, nrow(merged))
		m$edgeR <- rep(0, nrow(merged))
		m$DESeq <- rep(0, nrow(merged))
		m$DESeq2 <- rep(0, nrow(merged))
		if(i > 1) {
			m$DESeq[which(!is.na(merged$DESeq_PValue) & merged$DESeq_adj.PValue <= opt$pValueCutoff)] <- 1
			m$DESeq2[which(!is.na(merged$DESeq2_PValue) & merged$DESeq2_adj.PValue <= opt$pValueCutoff)] <- 1
			m$limma[which(!is.na(merged$limma_PValue) & merged$limma_adj.PValue <= opt$pValueCutoff)] <- 1
			m$edgeR[which(!is.na(merged$edgeR_PValue) & merged$edgeR_adj.PValue <= opt$pValueCutoff)] <- 1

			m <- as.data.frame(m)
			vc <- vennCounts(m)
			vennDiagram(vc, main=paste("Overlap for ", type, " genes.", sep=""))
		}
		else {
			m$DESeq[which(!is.na(merged$DESeq_PValue))] <- 1
			m$DESeq2[which(!is.na(merged$DESeq2_PValue))] <- 1
			m$limma[which(!is.na(merged$limma_PValue))] <- 1
			m$edgeR[which(!is.na(merged$edgeR_PValue))] <- 1
			m <- as.data.frame(m)
		}

		# genes detected by all four methods
		write.table(merged[which(rowSums(m) == 4), ], file = paste("detectedBy4of4_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		write.table(merged[which(rowSums(m) >= 3), ], file = paste("detectedBy3of4_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		write.table(merged[which(rowSums(m) >= 2), ], file = paste("detectedBy2of4_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		write.table(merged[which(rowSums(m) >= 1), ], file = paste("detectedBy1of4_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)


		merged$median_log2FC <- apply(merged[, c("limma_log2FC", "edgeR_log2FC", "DESeq2_log2FC")], 1, median, na.rm=TRUE)
		merged$median_adj.PValue <- apply(merged[, c("limma_adj.PValue", "edgeR_adj.PValue", "DESeq2_adj.PValue")], 1, median, na.rm=TRUE)

		# write the stuff only for limma, edgeR and DEseq2
		m <- m[, c("limma", "edgeR", "DESeq2")]
		write.table(merged[which(rowSums(m) >= 3), ], file = paste("detectedBy3of3_withoutDESeq_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		write.table(merged[which(rowSums(m) >= 2), ], file = paste("detectedBy2of3_withoutDESeq_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		write.table(merged[which(rowSums(m) >= 1), ], file = paste("detectedBy1of3_withoutDESeq_", type, ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
	}
	dev.off()
}

