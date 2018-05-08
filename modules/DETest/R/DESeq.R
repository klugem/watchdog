# load libraries
library(DESeq)

###################################### DESeq #############################################
pasillaDesign <- data.frame(row.names = colnames(exprs), condition = pData$condition)
condition <- pasillaDesign$condition

exprs <- round(exprs)
cds <- newCountDataSet(exprs, condition)
cds <- estimateSizeFactors(cds)
cds <- tryCatch(estimateDispersions(cds, method = "per-condition"), error = function(e) { print("[ERROR] Estimation of dispersions in DESeq failed!"); quit(save = "no", status = 1, runLast = FALSE) })
DE <- nbinomTest(cds, opt$controlCondition, opt$testCondition) 

# filtering / renaming
all <- DE
colnames(all) <- c("ID", "aveLog2CPM", paste("log2CPM_", opt$controlCondition, sep=""),  paste("log2CPM_", opt$testCondition, sep=""), "FC", "log2FC", "PValue", "adj.PValue")

# normalize without prior knowlege
cdsBlind <- tryCatch(estimateDispersions(cds, method = "blind"),
error = function(e) { return(estimateDispersions(cds, method = "blind", fitType="local")) }
)
vsd <- varianceStabilizingTransformation(cdsBlind)
normCounts <- exprs(vsd)
########################################################################################### 
