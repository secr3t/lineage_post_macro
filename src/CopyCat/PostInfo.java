package CopyCat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostInfo {

	private int number;
	private int wroteId;
	private LocalDateTime crawledTime;
	private String writer;
	private String title;
	private String contents;
	private List<CommentInfo> comments = new ArrayList<>();
	private List<String> imagePaths = new ArrayList<>();
	private boolean moved = false;

	public boolean isMoved() {
		return moved;
	}

	public void setMoved(boolean moved) {
		this.moved = moved;
	}

	public List<String> getImagePaths() {
		return imagePaths;
	}

	public void setImagePaths(List<String> imagePaths) {
		this.imagePaths = imagePaths;
	}

	public void addImagePaths(String path) {
		imagePaths.add(path);
	}

	public int getWroteId() {
		return wroteId;
	}

	public void setWroteId(int wroteId) {
		this.wroteId = wroteId;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public LocalDateTime getCrawledTime() {
		return crawledTime;
	}

	public void setCrawledTime(LocalDateTime crawledTime) {
		this.crawledTime = crawledTime;
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String writer) {
		this.writer = writer;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public void addComment(CommentInfo commentInfo) {
		this.comments.add(commentInfo);
	}

	public List<CommentInfo> getComments() {
		return comments;
	}

	public void setComments(List<CommentInfo> comments) {
		this.comments = comments;
	}

	// crawl 한지 20분이 지났으면 true 반환
	public boolean isTimeOut() {
		return this.crawledTime.plusMinutes(20).compareTo(LocalDateTime.now()) < 0 ? true : false;
	}

	@Override
	public String toString() {
		return "PostInfo [number=" + number + ", wroteId=" + wroteId + ", crawledTime=" + crawledTime + ", writer="
				+ writer + ", title=" + title + ", contents=" + contents + ", comments=" + comments + ", imagePaths="
				+ imagePaths + ", moved=" + moved + "]";
	}


}
