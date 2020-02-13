/* Generated by www.jsonschema2pojo.org with example requests sent to the github API v3. */
package de.lmu.ifi.bio.watchdog.validator.github.autogen.compare;


public class Commit_ {

    private Author__ author;
    private Committer__ committer;
    private String message;
    private Tree_ tree;
    private String url;
    private Integer commentCount;
    private Verification_ verification;

    public Author__ getAuthor() {
        return author;
    }

    public void setAuthor(Author__ author) {
        this.author = author;
    }

    public Committer__ getCommitter() {
        return committer;
    }

    public void setCommitter(Committer__ committer) {
        this.committer = committer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Tree_ getTree() {
        return tree;
    }

    public void setTree(Tree_ tree) {
        this.tree = tree;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Verification_ getVerification() {
        return verification;
    }

    public void setVerification(Verification_ verification) {
        this.verification = verification;
    }

}
