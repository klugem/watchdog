/* Generated by www.jsonschema2pojo.org with example requests sent to the github API v3. */
package de.lmu.ifi.bio.watchdog.validator.github.autogen.compare;

import java.util.List;

public class CompareAPIv3 {

    private String url;
    private String htmlUrl;
    private String permalinkUrl;
    private String diffUrl;
    private String patchUrl;
    private BaseCommit baseCommit;
    private MergeBaseCommit mergeBaseCommit;
    private String status;
    private Integer aheadBy;
    private Integer behindBy;
    private Integer totalCommits;
    private List<Commit__> commits = null;
    private List<File> files = null;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }

    public String getDiffUrl() {
        return diffUrl;
    }

    public void setDiffUrl(String diffUrl) {
        this.diffUrl = diffUrl;
    }

    public String getPatchUrl() {
        return patchUrl;
    }

    public void setPatchUrl(String patchUrl) {
        this.patchUrl = patchUrl;
    }

    public BaseCommit getBaseCommit() {
        return baseCommit;
    }

    public void setBaseCommit(BaseCommit baseCommit) {
        this.baseCommit = baseCommit;
    }

    public MergeBaseCommit getMergeBaseCommit() {
        return mergeBaseCommit;
    }

    public void setMergeBaseCommit(MergeBaseCommit mergeBaseCommit) {
        this.mergeBaseCommit = mergeBaseCommit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAheadBy() {
        return aheadBy;
    }

    public void setAheadBy(Integer aheadBy) {
        this.aheadBy = aheadBy;
    }

    public Integer getBehindBy() {
        return behindBy;
    }

    public void setBehindBy(Integer behindBy) {
        this.behindBy = behindBy;
    }

    public Integer getTotalCommits() {
        return totalCommits;
    }

    public void setTotalCommits(Integer totalCommits) {
        this.totalCommits = totalCommits;
    }

    public List<Commit__> getCommits() {
        return commits;
    }

    public void setCommits(List<Commit__> commits) {
        this.commits = commits;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

}