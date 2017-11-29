# load libraries
library(Biobase)
library(limma)
library(edgeR)

###################################### limma (voom) #############################################

# modify names of the condition to ensure that opt$testCondition/opt$controlCondition is calculated
pDataBak <- pData
pData$condition <- as.character(pData$condition)
pData$condition[pData$condition == opt$controlCondition] <- paste("A", opt$controlCondition, sep="_")
pData$condition[pData$condition == opt$testCondition] <- paste("Z", opt$testCondition, sep="_")

phenoData <- new("AnnotatedDataFrame", data=pData) 
eset <- ExpressionSet(assayData=exprs, phenoData=phenoData)

# normalize counts
nf <- calcNormFactors(eset, method="TMM")
groups <- phenoData(eset)$condition

design <- model.matrix(~ groups)
y <- voom(exprs(eset), design, lib.size=colSums(exprs(eset))*nf, normalize.method="quantile")

# build linear model
fit <- lmFit(y,design)
fit <- eBayes(fit)

# ensure that it is sorted by adjusted p.value
DE <- topTable(fit, coef=2, number=Inf, adjust.method="BH")

all <- DE
all$ID <- rownames(all)
all <- all[, c("ID", "logFC", "AveExpr", "B", "t", "P.Value", "adj.P.Val")]
colnames(all) <- c("ID", "log2FC", "aveLog2CPM", "B", "t", "PValue", "adj.PValue")

# normalize without prior knowlege
y <- voom(exprs(eset), lib.size=colSums(exprs(eset))*nf, normalize.method="quantile") 
normCounts <- y$E
pData <- pDataBak
################################################################################################# 
