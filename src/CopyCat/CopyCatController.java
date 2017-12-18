package CopyCat;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.web.ElementOutsideViewportException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.Alert.AlertType;

public class CopyCatController implements Initializable {

	/*
	 * javaFX elements declarations
	 */
	@FXML
	private Button start;
	@FXML
	private Button exit;
	@FXML
	private Button chooseDirectoryBtn;
	@FXML
	private TextField directoryTF;
	@FXML
	private TextField commentWriter;
	@FXML
	private TextField comment1;
	@FXML
	private TextField comment2;
	@FXML
	private TextArea loggingArea;
	@FXML
	private Label ipStatus;

	/*
	 * webDriver Declaration
	 */
	private WebDriver postReadingDriver;
	private WebDriver postWritingDriver;
	private WebDriver commentReadingDriver;
	private WebDriver commentWritingDriver;
	/*
	 * wrting page strings
	 */
	private String dc = "http://gall.dcinside.com/board/lists/?id=lineage&page=1";
	// private String destBoardList =
	// "http://linmania.net/bbs/board.php?bo_table=10";
	private String writePostPage = "http://linmania.net/bbs/write.php?bo_table=10";
	private String postedPage = "http://linmania.net/bbs/board.php?bo_table=10&wr_id="; // + idx
	private String boardUrl = "http://gall.dcinside.com/board/view/?id=lineage&no="; // + idx
	private String deletedUrl1 = "http://gall.dcinside.com/board/lists/?id=lineage";
	private String deletedUrl2 = "http://gall.dcinside.com/error/deleted/lineage";
	private String imagePath = "D:\\Pictures\\";
	/*
	 * thread-safety collection declarations
	 */
	private List<PostInfo> postInfoList = new CopyOnWriteArrayList<>();
	private BlockingQueue<PostInfo> postInfosQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<PostInfo> updatedCommentQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<String> logs = new LinkedBlockingQueue<>();
	/*
	 * etc declarations
	 */
	// private JavascriptExecutor js;
	private String allPws = "aabbcc";
	private String ipRegex = "(?:\\d{1,3}\\.){2}\\*\\.\\*";
	/*
	 * thread declarations
	 */
	private CheckPostInfoListThread commentChecker = new CheckPostInfoListThread();
	private PostReadingThread postReader = new PostReadingThread();
	private PostWritingThread postWriter = new PostWritingThread();
	private CommentWritingThread commentWritingThread = new CommentWritingThread();
	// private CommentReadingThread commentReader = new CommentReadingThread();
	private LoggingThread logger = new LoggingThread();
	/*
	 * make a flag to prevent comment reader and writer before any post is posted
	 */
	private boolean isPostWrote = false;
	private ChromeOptions chromeOpt = new ChromeOptions().addArguments("--headless");
	// chromeOpt.addArguments("--headless");

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.setProperty("webdriver.chrome.driver", "src/chromedriver.exe");
		// System.setProperty("phantomjs.binary.path", "src/phantomjs.exe");
		loggingArea.textProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				loggingArea.setScrollTop(Double.MAX_VALUE);
			}
		});
		postReadingDriver = new ChromeDriver(chromeOpt);
		// postReadingDriver = new PhantomJSDriver();
		postWritingDriver = new ChromeDriver(chromeOpt);
		// 미구현 목록은 주석처리

		// int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		// int height = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

		// postReadingDriver.manage().window().setPosition(new Point(0, 0));
		// postReadingDriver.manage().window().setSize(new Dimension(width / 2, height /
		// 2));
		// postReadingDriver.manage().window().maximize();
		// postReadingDriver.manage().window().setSize(new Dimension(width, height *
		// 100));
		// postReadingDriver.manage().window().setPosition(new Point(-width, -height));
		// postWritingDriver.manage().window().setPosition(new Point(width / 2, 0));
		// postWritingDriver.manage().window().setSize(new Dimension(width / 2, height /
		// 2));

		// commentReadingDriver = new PhantomJSDriver(DesiredCapabilities.phantomjs());
		// commentWritingDriver = new PhantomJSDriver(DesiredCapabilities.phantomjs())
		commentReadingDriver = new ChromeDriver(chromeOpt);
		commentWritingDriver = new ChromeDriver(chromeOpt);
		// commentReadingDriver.manage().window().setPosition(new Point(0, height / 2));
		// commentReadingDriver.manage().window().setSize(new Dimension(width / 2,
		// height / 2));
		// commentWritingDriver.manage().window().setPosition(new Point(width / 2,
		// height / 2));
		// commentWritingDriver.manage().window().setSize(new Dimension(width / 2,
		// height / 2));
		// js = (JavascriptExecutor) commentReadingDriver;
	}

	// 시작시 실행.
	public void start() {
		logger.start();
		postReader.start();
		postWriter.start();
		commentChecker.start();
		commentWritingThread.start();
		// replyChecker.start();
	}

	// 종료버튼 클릭시.
	public void exit() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("종료합니다.");
		alert.setHeaderText("종료");
		alert.setContentText("이용해주셔서 감사합니다. 종료합니다.");
		alert.showAndWait().ifPresent(rs -> {
			if (rs == ButtonType.OK) {
				commentChecker.interrupt();
				commentWritingThread.interrupt();
				postReader.interrupt();
				postWriter.interrupt();
				postReadingDriver.quit();
				postWritingDriver.quit();
				commentReadingDriver.quit();
				commentWritingDriver.quit();
				Platform.exit();
				System.exit(0);
			}
		});
	}

	public class CheckPostInfoListThread extends Thread {
		@Override
		public void run() {
			while (true) {
				if (isPostWrote)
					for (PostInfo e : postInfoList) {
						if (e.isTimeOut()) {
							logs.add(e.getNumber() + "글이 작성된지 20분이 경과하였기 때문에 더이상 확인하지 않음");
							postInfoList.remove(e);
						} else if (e.isMoved()) {
							commentReadingDriver.get(boardUrl + e.getNumber());
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
							}
							if (commentReadingDriver.getCurrentUrl().equals(deletedUrl1)
									|| commentReadingDriver.getCurrentUrl().equals(deletedUrl2)) {
								logs.add(e.getNumber() + "글이 DC에서 삭제되어 더이상 확인하지 않음");
								postInfoList.remove(e);
							} else {
								Document doc = Jsoup.parse(commentReadingDriver.getPageSource());
								int saved = saveComment(e, doc);
								postInfoList.set(getIndexOfPostInfoInPostInfoList(e), e);
								if (saved > 0) {
									updatedCommentQueue.add(e);
									logs.add(saved + "개의 댓글이" + "DC번호 : " + e.getNumber() + "린갤 번호 : " + e.getWroteId()
											+ "글의 댓글이 업데이트 되어 댓글을 저장합니다.");
								}
							}
						}
					}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public class LoggingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				String log = logs.poll();
				if (log != null) {
					Platform.runLater(() -> {
						loggingArea.appendText(System.lineSeparator());
						loggingArea.appendText(log);
					});
				}
			}
		}
	}

	// 댓글을 읽어들이는 thread
	/*
	 * public class CommentReadingThread extends Thread {
	 * 
	 * @Override public void run() { while (true) { commentReadingDriver.get(dc);
	 * while (((JavascriptExecutor)
	 * commentReadingDriver).executeScript("return document.readyState")
	 * .equals("complete")) { logs.add(getPostsReplyCount(null).toString()); break;
	 * } try { Thread.sleep(1000 * 10); } catch (InterruptedException e) {
	 * e.printStackTrace(); } } } }
	 */

	// 추가된 댓글을 작성하는 thread
	public class CommentWritingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				if (isPostWrote) {
					PostInfo current = updatedCommentQueue.poll();
					if (current != null) {
						commentWritingDriver.get(postedPage + current.getWroteId());
						System.out.println(postedPage + current.getWroteId() + "로 이동");
						Document doc = Jsoup.parse(commentWritingDriver.getPageSource());
						Elements LScomments = doc.select("section.comment-media .media");
						List<CommentInfo> readyComments = current.getComments();
						for (int i = 0, j = 0; i < readyComments.size(); i++) {
							if (j < LScomments.size()) {
								j++;
								continue;
							} else if (readyComments.get(i).getWiter().equals(commentWriter.getText())) {
								continue;
							} else {
								writeCommentLSInner(commentWritingDriver, readyComments.get(i));
								try {
									Thread.sleep(1000); // 연속해서~~ 방지
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						logs.add("DC번호 : " + current.getNumber() + "린갤 번호 : " + current.getWroteId()
								+ "글의 업데이트된 댓글을 추가하였습니다.");
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 게시글을 읽어들이는 thread
	public class PostReadingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				openDC();
				postRead1();
				Random rand = new Random();
				int randomSleep = rand.nextInt(7) + 5;
				logs.add(randomSleep + "초만큼 쉼.");
				try {
					Thread.sleep(randomSleep * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 게시글을 린갤 저장소에 작성하는 thread
	public class PostWritingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				PostInfo postInfo = postInfosQueue.poll();
				if (postInfo != null) {
					try {
						writePost(postInfo);
						Thread.sleep(1000);
						writeCommentLS(postInfo);
						isPostWrote = true;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(1000); // 너무 빠른 속도로 인하여 cpu 점유율이 급격하게 높아짐.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

	// DC 린갤로 이동.
	public void openDC() {
		postReadingDriver.navigate().to(dc);
	}

	// DC에서 새 글을 찾음.
	public void postRead1() {
		int num = 0;
			num = getNewNumber();
		int lastExist;
		try {
			lastExist = postInfoList.get(postInfoList.size() - 1).getNumber();
		} catch (ArrayIndexOutOfBoundsException e) {
			lastExist = 0;
		}
		if (num > lastExist && num != 0) {
			try {
				postReadingDriver.get(boardUrl + num);
			} catch (Exception e) {
				postReadingDriver.quit();
				postReadingDriver = new ChromeDriver(chromeOpt);
				postReadingDriver.manage().window().maximize();
				postReadingDriver.get(boardUrl + num);
			}
			// 댓글 하나 또는 두 개를 먼저 입력한 후에 collection에 저장함.
			try {
				writeCommentDC();
			} catch (UnhandledAlertException e) {
				logs.add(e.getMessage());
				postReadingDriver.switchTo().alert().accept();
				postReadingDriver.switchTo().defaultContent();
				ipStatus.setOpacity(1.0);
				MySoundUtils util = new MySoundUtils();
				util.playSoundClip("src/beep.wav");
			}
			getCurrentInfo(num);
		}
	}

	// 글을 가져오기 전에 DC에 댓글을 작성함.
	public void writeCommentDC() {
		hideAllIframe(postReadingDriver);
		WebElement commentUserName = postReadingDriver.findElement(By.cssSelector("input#name"));
		WebElement commentPW = postReadingDriver.findElement(By.cssSelector("input#password"));
		WebElement comment = postReadingDriver.findElement(By.cssSelector("textarea#memo"));
		WebElement writeCommentBtn = postReadingDriver.findElement(By.cssSelector("img#re_write"));
		if (!comment1.getText().isEmpty()) {
			String username = commentWriter.getText();
			username = username.isEmpty() ? "ㅋㅋㅋㅋ아우" : username;
			commentUserName.sendKeys(username);
			commentPW.sendKeys(allPws);
			comment.sendKeys(comment1.getText());
			try {
				writeCommentBtn.click();
			} catch (Exception e) {
				try {
					scrollIntoView(postReadingDriver, writeCommentBtn);
					writeCommentBtn.click();
				} catch (UnhandledAlertException alertException) {
					throw new UnhandledAlertException(alertException.getAlertText());
				}
			}
			if (!comment2.getText().isEmpty()) {
				Random rand = new Random();
				int randomSleep = rand.nextInt(7) + 5;
				try {
					Thread.sleep(1000 * randomSleep);
				} catch (InterruptedException e1) {
				}
				comment.sendKeys(comment2.getText());
				try {
					writeCommentBtn.click();
				} catch (Exception e) {
					try {
						scrollIntoView(postReadingDriver, writeCommentBtn);
						writeCommentBtn.click();
					} catch (UnhandledAlertException alertException) {
						throw new UnhandledAlertException(alertException.getAlertText());
					}
				}
			}
		}
		logs.add("댓글 쓰기 완료");
	}

	// 게시글을 읽어서 vo에 담은 뒤에 collection에 저장
	public void getCurrentInfo(int num) {
		logs.add("게시글 저장 시작");
		Document doc = Jsoup.parse(postReadingDriver.getPageSource());
		PostInfo currentPost = new PostInfo();
		currentPost.setNumber(num);
		currentPost.setCrawledTime(LocalDateTime.now());
		currentPost.setTitle(doc.select("dl.wt_subject > dd").text());
		logs.add(currentPost.getTitle());
		currentPost.setWriter(doc.select("span.user_layer").attr("user_name"));
		String contents = doc.select("div.s_write td").html();
		// 이미지가 존재하면 이미지를 선택한 경로에 저장한 뒤 저장한 경로에서
		// posting 페이지에서 추가하기 위함.
		List<WebElement> imgs = postReadingDriver.findElements(By.cssSelector(".s_write img"));
		for (int i = 0; i < imgs.size(); i++) {
			String thisImagePath = imagePath + currentPost.getNumber() + i + ".png";
			// takeScreenshotElement(imgs.get(i), thisImagePath);
			try {
				Shutterbug.shootElement(postReadingDriver, imgs.get(i)).withName("" + currentPost.getNumber() + i)
						.save(imagePath);
			} catch (ElementOutsideViewportException | RasterFormatException exc) {
				logs.add(currentPost.getNumber() + "의 글은 알 수 없는 이유로 스크린샷을 시도할 수 없습니다. 캡쳐를 건너뜁니다.");
			}
			currentPost.addImagePaths(thisImagePath);
		}
		// Elements imgs = doc.select(".s_write img");
		// postReadingDriver.findElements(By.cssSelector(".s_write img")).get(0).

		// 해당 방법은 ~.jpg, ~.png 등 확장자가 붙어있는 경우에만 사용가능하였다.
		/*
		 * for(int i = 0; i < imgs.size(); i++) { try { URL url = new
		 * URL(imgs.get(i).attr("src")); BufferedImage image = ImageIO.read(url); String
		 * thisImagePath = imagePath + currentPost.getNumber() + i + ".png"; if(image !=
		 * null) { ImageIO.write(image, "png", new File(thisImagePath));
		 * currentPost.addImagePaths(thisImagePath); } image.flush(); } catch
		 * (IOException e) { } }
		 */
		// image, #dcappfooter 삭제
		contents = contents.replace(doc.select(".s_write td img").outerHtml(), "");
		contents = contents.replace(doc.select("#dcappfooter").outerHtml(), "");
		currentPost.setContents(contents);
		saveComment(currentPost, doc);
		// logs.add(currentPost.toString());
		postInfoList.add(currentPost);
		// logs.add(postInfoList.toString());
		try {
			if (!currentPost.getContents().isEmpty() || currentPost.getImagePaths().size() > 0)
				postInfosQueue.put(currentPost);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// 댓글 저장하기 (초기에 읽어들인 후에 20분간 확인해서 저장하기 때문에 method로 작성함)
	public int saveComment(PostInfo currentPost, Document doc) {
		Elements commentWriters = doc.select("#gallery_re_contents > tbody > tr.reply_line > td.user.user_layer");
		Elements comments = doc.select("td.reply");
		int start = currentPost.getComments().size();
		System.out.println("저장되어 있는 댓글 수 : " + currentPost.getComments().size() + " 댓글 수 : " + comments.size());
		for (int i = start; i < commentWriters.size(); i++) {
			CommentInfo commentInfo = new CommentInfo();
			commentInfo.setComment(comments.get(i).text().replaceAll(ipRegex, ""));
			commentInfo.setWiter(commentWriters.get(i).attr("user_name"));
			currentPost.addComment(commentInfo);
		}
		return commentWriters.size() - start;
	}

	// 린갤저장소에 댓글을 작성하는 method
	public void writeCommentLSInner(WebDriver driver, CommentInfo e) {
		if (!e.getWiter().equals(commentWriter.getText())) {
			WebElement writerInput = driver.findElement(By.id("wr_name"));
			WebElement passwordInput = driver.findElement(By.id("wr_password"));
			WebElement contentTA = driver.findElement(By.id("wr_content"));
			WebElement captchaInput = driver.findElement(By.id("captcha_key"));
			WebElement addCommentBtn = driver.findElement(By.id("btn_submit"));
			writerInput.sendKeys(e.getWiter());
			passwordInput.sendKeys(allPws);
			contentTA.sendKeys(e.getComment());
			captchaInput.sendKeys(allPws);
			try {
				addCommentBtn.click();
			} catch (Exception exc) {
				scrollIntoView(driver, addCommentBtn);
				addCommentBtn.click();
			}
		}
	}

	// 린갤저장소에 게시물을 작성한 후에 댓글을 작성하는 method
	public void writeCommentLS(PostInfo postInfo) {
		List<CommentInfo> comments = postInfo.getComments();
		for (CommentInfo e : comments) {
			writeCommentLSInner(postWritingDriver, e);
			try {
				Thread.sleep(1000); // 너무 빠른 시간..~~~
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	// 린갤저장소에 게시물을 작성하는 method
	public void writePost(PostInfo postInfo) throws InterruptedException {
		postWritingDriver.get(writePostPage);
		WebElement writerInput = postWritingDriver.findElement(By.cssSelector("input#wr_name"));
		WebElement passwordInput = postWritingDriver.findElement(By.cssSelector("input#wr_password"));
		WebElement subjectInput = postWritingDriver.findElement(By.cssSelector("input#wr_subject"));
		WebElement kcaptcha = postWritingDriver.findElement(By.cssSelector("input#captcha_key"));
		writerInput.sendKeys(postInfo.getWriter());
		passwordInput.sendKeys(allPws);
		subjectInput.sendKeys(postInfo.getTitle());
		String winHandleBefore = postWritingDriver.getWindowHandle();
		for (String path : postInfo.getImagePaths()) {
			uploadImage(path, postWritingDriver, winHandleBefore);
		}
		writeContentInIframe(postInfo.getContents(), postWritingDriver);
		WebElement submitBtn = postWritingDriver.findElement(By.cssSelector("button#btn_submit"));
		kcaptcha.sendKeys(allPws);
		scrollIntoView(postWritingDriver, submitBtn);
		submitBtn.click();
		Thread.sleep(5000);
		String postedUrl = postWritingDriver.getCurrentUrl();
		Integer wroteId = Integer.parseInt(postedUrl.split("wr_id=")[1]);
		postInfo.setWroteId(wroteId);
		postInfo.setMoved(true);
		postInfoList.set(getIndexOfPostInfoInPostInfoList(postInfo), postInfo);
		logs.add(postInfo.toString());
		logs.add(postInfoList.toString());
		Thread.sleep(1000);
	}

	// post 정보를 업데이트 하기 위해서 index를 가져오는 method 작성
	public int getIndexOfPostInfoInPostInfoList(PostInfo postInfo) {
		int returnNum = 0;
		for (int i = 0; i < postInfoList.size(); i++) {
			if (postInfoList.get(i).getNumber() == postInfo.getNumber())
				returnNum = i;
		}
		return returnNum;
	}

	// DC에서 가장 최근 글을 찾기 위해서 최상단 번호를 추출함.
	public int getNewNumber() {
		Document doc = Jsoup.parse(postReadingDriver.getPageSource());
		Elements td_numbers = doc.select("td.t_notice");
		int num = 0;
		for (Element td_number : td_numbers) {
			try {
				num = Integer.parseInt(td_number.text());
				logs.add("게시글 번호 : " + num);
			} catch (NumberFormatException nfe) {
				System.out.println(td_number + " : 숫자를 가지고 있지 않습니다.");
			}
		}
		return num;
	}

	// 해당하는 post의 reply 갯수를 가져옴.
	public Integer getPostsReplyCount(PostInfo postInfo) {

		return 0;
	}

	public void scrollIntoView(WebDriver driver, WebElement element) {
		runScript(driver, "arguments[0].scrollIntoView(false)", element);
	}

	public void setCaretToEndPos(WebDriver driver, WebElement element) {
		runScript(driver, "arguments[0].selectionStart = arguments[0].selectionEnd = arguments[0].value.length;",
				element);
	}

	public void hideAllIframe(WebDriver driver) {
		((JavascriptExecutor) driver).executeScript(
				"var elements = document.getElementsByTagName('iframe');" + "var iframeArr = Array.from(elements);"
						+ "iframeArr.forEach( function(item, index) {" + "	item.style.display = 'none';" + "} );"
						+ "var a = document.getElementById('wif_adx_banner_wrap');\r\n" + "if (a!=null) {a.remove();}");
	}

	public Object runScript(WebDriver driver, String script, WebElement target) {
		/*
		 * to run script shorter
		 */
		return ((JavascriptExecutor) driver).executeScript(script, target);
	}

	public static void uploadImage(String imagePath, WebDriver driver, String winHandleBefore)
			throws InterruptedException {

		// 사진 첨부 버튼
		WebElement photoUploadBtn = driver.switchTo().frame(driver.findElement(By.cssSelector(".form-group iframe")))
				.findElement(By.cssSelector("button.se2_photo"));
		Thread.sleep(1000);
		photoUploadBtn.click();
		// 사진 첨부 창으로 포커스 이동
		Thread.sleep(1000);
		for (String winHandle : driver.getWindowHandles()) {
			driver.switchTo().window(winHandle);
		}
		Thread.sleep(1000);
		WebElement fileInput = driver.findElement(By.cssSelector("input#fileupload"));
		Thread.sleep(1000);
		fileInput.sendKeys(imagePath);
		Thread.sleep(1000);
		Thread.sleep(1000);
		driver.findElement(By.cssSelector("button#img_upload_submit")).click();
		Thread.sleep(1000);
		driver.switchTo().window(winHandleBefore);
	}

	public void writeContentInIframe(String contents, WebDriver driver) throws InterruptedException {
		Thread.sleep(5000);
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.cssSelector(".form-group iframe")));
		// html 로 글 쓰기
		WebElement toHtmlTA = driver.findElement(By.cssSelector("button.se2_to_html"));
		try {
			toHtmlTA.click();
		} catch (Exception e) {
			scrollIntoView(driver, toHtmlTA);
			toHtmlTA.click();
		}
		WebElement htmlTA = driver.findElement(By.cssSelector("textarea.se2_input_syntax.se2_input_htmlsrc"));
		// String ifImage = htmlTA.getText();
		// htmlTA.clear();
		htmlTA.click();
		setCaretToEndPos(postWritingDriver, htmlTA);
		htmlTA.sendKeys(contents);
		// htmlTA.sendKeys(ifImage);
		driver.switchTo().defaultContent();
	}

	public void chooseDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("저장할 이미지의 경로를 선택해주세요.");
		// Show open file dialog

		File file = directoryChooser.showDialog(null);

		if (file != null) {
			imagePath = file.getPath() + "\\";
			directoryTF.setText(imagePath);
		}
	}

	public void takeScreenshotElement(WebElement element, String path) throws IOException {
		System.out.println("이미지 저장 시작" + path);
		WrapsDriver wrapsDriver = (WrapsDriver) element;
		File screenshot = ((TakesScreenshot) wrapsDriver.getWrappedDriver()).getScreenshotAs(OutputType.FILE);
		Rectangle rectangle = new Rectangle(element.getSize().width, element.getSize().height);
		Point location = element.getLocation();
		BufferedImage bufferedImage = ImageIO.read(screenshot);
		BufferedImage destImage = bufferedImage.getSubimage(location.x, location.y, rectangle.width, rectangle.height);
		ImageIO.write(destImage, "png", screenshot);
		File file = new File(path);
		FileUtils.copyFile(screenshot, file);
	}

}