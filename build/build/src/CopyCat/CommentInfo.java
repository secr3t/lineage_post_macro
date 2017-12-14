package CopyCat;

public class CommentInfo {
	private String witer;
	private String comment;
	public String getWiter() {
		return witer;
	}
	public void setWiter(String witer) {
		this.witer = witer;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	@Override
	public String toString() {
		return "ReplyInfo [witer=" + witer + ", comment=" + comment + "]";
	}
}
