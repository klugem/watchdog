# assumes that a DE test was run before!
if(exists("normCounts")) {

	# principal component analysis from DESeq 
	library(genefilter)
	library(lattice)
	library(RColorBrewer)
	rv = rowVars(normCounts)
	pca = prcomp(t(normCounts))
	S1 <- which(colnames(normCounts) %in% rownames(pData)[pData$condition == opt$controlCondition])
	S2 <- which(colnames(normCounts) %in% rownames(pData)[pData$condition == opt$testCondition])
	types <- rep("", ncol(normCounts))
	types[S1] <- opt$controlCondition
	types[S2] <- opt$testCondition
	fac = factor(types)
	colours = brewer.pal(length(fac), "Paired")[seq(1,min(3, length(levels(fac))))]
	if(length(levels(fac)) == 2) {
		colours <- colours[c(1,2)]
	}
	p <- xyplot(PC2 ~ PC1, groups = fac, data = as.data.frame(pca$x), pch = 16, cex = 2, aspect = "iso", col = colours, main = draw.key(key = list(rect = list(col = colours), text = list(levels(fac)), rep = FALSE)),
 		panel=function(x, y, ...) {
			panel.xyplot(x, y, ...);
			ltext(x=x, y=y, labels=colnames(normCounts), pos=1, offset=1, cex=0.8)
            })
	print(p)

	# heatmap, top 1000 or all what is there
	minElements <- min(nrow(normCounts), 1000)
	select = order(rowMeans(normCounts), decreasing=TRUE)[1:minElements]
	hmcol = colorRampPalette(brewer.pal(9, "GnBu"))(100)
	d <- t(normCounts[select,])
	colnames(d) <- rep("", ncol(d))
	heatmap.2(d, col = hmcol, trace="none", dendrogram="row", labels=F, main = paste("Clustering of top ", minElements, " genes", sep=""), density.info = "none", lhei=c(0.2, 1), margins=c(3,10), cexRow=0.9) 

	# clustering based on euclidian distances
	dists = dist(t(normCounts))
	mat = as.matrix(dists)
	rownames(mat) = colnames(mat) = colnames(normCounts)
	heatmap.2(mat, trace="none", col = rev(hmcol), dendrogram="row", main="Euclidean distance", cex.lab = 4, density.info = "none", lhei=c(0.2, 1), margins=c(10,10), cexRow=0.9, cexCol=0.9) 

	# vulcano plot
	a <- notSignificant[!is.na(notSignificant$adj.PValue) & !is.na(notSignificant$log2FC), ]
	allNotNA <- all[!is.na(all$adj.PValue) & !is.na(all$log2FC), ]
	xl <- c(max(-25, min(allNotNA$log2FC[allNotNA$log2FC<0 & !is.infinite(allNotNA$log2FC)])), min(25, max(allNotNA$log2FC[allNotNA$log2FC>0 & !is.infinite(allNotNA$log2FC)])))
	yl <- c(0, max(-log10(allNotNA$adj.PValue[allNotNA$adj.PValue > 0])) -0.5) 
	plot(a$log2FC, -log10(a$adj.PValue), ylab="-log10(adj. PValue)", xlab="log2 foldchange", xlim=xl, ylim=yl, pch=20, cex=0.75, main=paste("Volcano plot for", nrow(allNotNA), "genes", sep=" "))
	
	legendNames <- opt$foldchangeCutoffNames[order(opt$foldchangeCutoff, decreasing = F)]
	cutoffs <- c(opt$foldchangeCutoff, 10^100)
	cutoffs <- sort(cutoffs, decreasing = F)
	hmcol = brewer.pal(length(cutoffs)+1, "Dark2")
	points(notSelected$log2FC, -log10(notSelected$adj.PValue), col=hmcol[1], pch=20, cex=0.75)
	text <- c(paste("too small FC (n=", nrow(notSelected),")",sep=""))
	for(i in seq(1, length(cutoffs)-1)) {
		c <- allNotNA[!is.na(allNotNA$log2FC) & !is.na(allNotNA$adj.PValue) & allNotNA$adj.PValue <= opt$pValueCutoff & abs(allNotNA$log2FC) >= cutoffs[i] & abs(allNotNA$log2FC) < cutoffs[i+1], ]
		points(c$log2FC, -log10(c$adj.PValue), col=hmcol[i+1], pch=20, cex=0.75)
		text <- c(text, paste(legendNames[i], " (n=", nrow(c), ")", sep=""))
	}	
	legend("top", text, cex=1, fill=hmcol[seq(1, length(text))] ,ncol=3, inset=c(0,0), xpd=T)

	# end plotting
	dev.off()
}
