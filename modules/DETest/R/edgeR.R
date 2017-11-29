# load libraries
library(edgeR)

###################################### edgeR #############################################
group <- factor(pData$condition)
y <- DGEList(counts=exprs,group=group)

# get normalization factors
y <- calcNormFactors(y, method="TMM")
y <- estimateCommonDisp(y, verbose=TRUE)
y <- estimateTagwiseDisp(y)
et <- exactTest(y, pair=c(opt$controlCondition, opt$testCondition))

# prepare for output
DE <- et$table
DE$adj.PValue <- p.adjust(DE$PValue, method = "BH")
# ensure that it is sorted by adjusted p.value
DE <- DE[order(DE$adj.PValue), ]

# filtering / renaming
all <- DE
all$ID <- rownames(all)
all <- all[, c("ID", "logFC", "logCPM", "PValue", "adj.PValue")]
colnames(all) <- c("ID", "log2FC", "aveLog2CPM", "PValue", "adj.PValue")

# normalize without prior knowlege
normCounts <- log2(y$pseudo.counts+1)
##########################################################################################
