# assumes that a DE test was run before!
if(exists("all") && exists("normCounts")) {
	# join the feature stuff
	normCountsWrite <- as.data.frame(normCounts)
	normCountsWrite$ID <- rownames(normCounts)
	normCountsWrite <- normCountsWrite[, c(ncol(normCountsWrite), seq(1, ncol(normCountsWrite)-1))]

	if(!is.null(features)) {
		all <- merge(all, features, by.x="ID", by.y=opt$featureAnnotationID, all.x = T)
		normCountsWrite <- merge(normCountsWrite, features, by.x="ID", by.y=opt$featureAnnotationID, all.x = T)
		
	}
	all <- all[order(all$adj.PValue), ]

	# write normalized table to a file
	write.table(normCountsWrite, file = paste(opt$output, "/", currentMethod, ".normalized.csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)

	# write the results of the DE test to files
	i <- 1
	for(cutoff in opt$foldchangeCutoff) {
		# ensure that these columns are really there

		cut <- all[!is.na(all$log2FC) & !is.na(all$adj.PValue) & all$adj.PValue <= opt$pValueCutoff & abs(all$log2FC) >= cutoff, ]
		write.table(cut, file = paste(opt$output, "/", currentMethod, ".", opt$foldchangeCutoffNames[i], ".csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)
		
		# write the bed file	
		if(!is.null(features)) {
			# test if all columns are there
			if(all(c("chr", "start", "end", "ID", "log2FC", "strand") %in% colnames(cut))) {
				write.table(cut[, c("chr", "start", "end", "ID", "log2FC", "strand")], file = paste(opt$output, "/", currentMethod, ".", opt$foldchangeCutoffNames[i], ".bed", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = FALSE, row.names = FALSE)
			}
		}
		i <- i+1
	}
	write.table(all, file = paste(opt$output, "/", currentMethod, ".all.csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = TRUE, row.names = FALSE)

	# not selected or significant
	all$adj.PValue[all$adj.PValue < 10^-50] <- 10^-50
	notSignificant <- all[!is.na(all$log2FC) & !is.na(all$adj.PValue) & all$adj.PValue > opt$pValueCutoff, ]
	notSelected <- all[!is.na(all$log2FC) & !is.na(all$adj.PValue) & all$adj.PValue <= opt$pValueCutoff & abs(all$log2FC) < opt$foldchangeCutoff[1], ]

	# write a table with expressed genes for GO analysis as background
	namesExpr <- gsub("\\.[0-9]+$", "", rownames(exprs))
	write.table(namesExpr,  paste(opt$output, "/", "expressed_min_", opt$minKeepReads,"_reads.csv", sep=""), append = FALSE, quote = FALSE, sep = "\t", col.names = FALSE, row.names = FALSE)
}
