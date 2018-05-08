# load libraries
library(DESeq2)

###################################### DESeq2 #############################################
colData <- data.frame(condition=factor(pData$condition))

exprs <- round(exprs)
dds <- DESeqDataSetFromMatrix(exprs, colData, formula(~ condition))
dds <- DESeq(dds)
res <- results(dds, contrast=c("condition", opt$testCondition, opt$controlCondition))

# filtering / renaming
all <- as.data.frame(res)
colnames(all) <- c("aveLog2CPM", "log2FC", "lfcSE" , "stat", "PValue", "adj.PValue")
all$ID <- rownames(all)
all <- all[,c(ncol(all), seq(1, ncol(all)-1))]

# normalize without prior knowlege
vsd <- varianceStabilizingTransformation(dds, blind = T) # normalize without prior knowlege
normCounts <- assay(vsd)
colnames(normCounts) <- colnames(exprs)
########################################################################################### 
