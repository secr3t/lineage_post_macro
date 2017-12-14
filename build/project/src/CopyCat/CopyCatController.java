package CopyCat;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
	private String destBoardList = "http://linmania.net/bbs/board.php?bo_table=10";
	private String writePostPage = "http://linmania.net/bbs/write.php?bo_table=10";
	private String postedPage = "http://linmania.net/bbs/board.php?bo_table=10&wr_id=";		// + idx
	private String boardUrl = "http://gall.dcinside.com/board/view/?id=lineage&no=";			// + idx
	private String deletedUrl1 = "http://gall.dcinside.com/board/lists/?id=lineage";
	private String deletedUrl2 = "http://gall.dcinside.com/error/deleted/lineage";
	private String imagePath = "D:\\Pictures";
	/*
	 * thread-safety collection declarations
	 */
	private List<PostInfo> postInfoList = Collections.synchronizedList(new ArrayList<>());
	private BlockingQueue<PostInfo> postInfosQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<String> logs = new LinkedBlockingQueue<>();
	/*
	 * etc declarations
	 */
	private JavascriptExecutor js;
	private String allPws = "aabbcc";
	private String ipRegex = "\\b(?:\\d{1,3}\\.){2}\\*\\.\\*";
	/*
	 * thread declarations
	 */
	private PostReadingThread postReader = new PostReadingThread();
	private PostWritingThread postWriter = new PostWritingThread();
	private CommentReadingThread commentReader = new CommentReadingThread();
	private LoggingThread logger = new LoggingThread();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		 System.setProperty("phantomjs.binary.path",
		 "src/phantomjs.exe");
		System.setProperty("webdriver.chrome.driver", "src/chromedriver.exe");
		loggingArea.textProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				loggingArea.setScrollTop(Double.MAX_VALUE);
			}
		});
		 postReadingDriver = new PhantomJSDriver();
//		postReadingDriver = new ChromeDriver();
//		 postWritingDriver = new PhantomJSDriver();
		postWritingDriver = new ChromeDriver();
//		미구현 목록은 주석처리
/*		commentReadingDriver = new ChromeDriver();
		commentWritingDriver = new ChromeDriver();
		int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		int height = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		postReadingDriver.manage().window().setPosition(new Point(0, 0));
		postReadingDriver.manage().window().setSize(new Dimension(width / 2, height / 2));
		postWritingDriver.manage().window().setPosition(new Point(width / 2, 0));
		postWritingDriver.manage().window().setSize(new Dimension(width / 2, height / 2));
		commentReadingDriver.manage().window().setPosition(new Point(0, height / 2));
		commentReadingDriver.manage().window().setSize(new Dimension(width / 2, height / 2));
		commentWritingDriver.manage().window().setPosition(new Point(width / 2, height / 2));
		commentWritingDriver.manage().window().setSize(new Dimension(width / 2, height / 2));*/
		js = (JavascriptExecutor) commentReadingDriver;
	}

	// 시작시 실행.
	public void start() {
		postReader.start();
		postWriter.start();
		logger.start();
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
				postReadingDriver.quit();
				postWritingDriver.quit();
//				commentReadingDriver.quit();
//				commentWritingDriver.quit();
				Platform.exit();
				System.exit(0);
			}
		});
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
	public class CommentReadingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				commentReadingDriver.get(dc);
				while (((JavascriptExecutor) commentReadingDriver).executeScript("return document.readyState")
						.equals("complete")) {
					logs.add(getPostsReplyCount(null).toString());
					break;
				}
				try {
					Thread.sleep(1000 * 10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
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
				try {
					postRead1();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Random rand = new Random();
				int randomSleep = rand.nextInt(5) + 5;
				logs.add(randomSleep + "초만큼 쉼.");
				try {
					Thread.sleep(randomSleep*1000);
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
			while(true) {
				PostInfo postInfo = postInfosQueue.poll();
				if(postInfo != null) {
					try {
						writePost(postInfo);
						// 글을 쓰고 난 후 40초 동안은 글을 쓰면 안됨.
						Thread.sleep(1000*40);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	// DC 린갤로 이동.
	public void openDC() {
		postReadingDriver.get(dc);
	}

	// DC에서 새 글을 찾음.
	public void postRead1() throws InterruptedException {
		int num = getNewNumber();
		int lastExist;
		try {
			lastExist = postInfoList.get(postInfoList.size() - 1).getNumber();
		} catch (ArrayIndexOutOfBoundsException e) {
			lastExist = 0;
		}
		if (num > lastExist) {
			postReadingDriver.get(boardUrl + num);
			// 댓글 하나 또는 두 개를 먼저 입력한 후에 collection에 저장함.
			writeCommentDC();
			getCurrentInfo(num);
		}
	}

	// 글을 가져오기 전에 DC에 댓글을 작성함.
	public void writeCommentDC() {
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
				scrollIntoView(postReadingDriver, writeCommentBtn);
				writeCommentBtn.click();
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
				scrollIntoView(postReadingDriver, writeCommentBtn);
				writeCommentBtn.click();
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
		currentPost.setTitle(doc.select("dl.wt_subject > dd").get(0).text());
		currentPost.setWriter(doc.select("span.user_layer").get(0).attr("user_name"));
		String contents = doc.select("div.s_write td").get(0).text();
		currentPost.setContents(contents);
		Elements commentWriters = doc.select("#gallery_re_contents > tbody > tr.reply_line > td.user.user_layer");
		Elements comments = doc.select("td.reply");
		logs.add("댓글 수 : " + comments.size());
		for (int i = 0; i < commentWriters.size(); i++) {
			CommentInfo commentInfo = new CommentInfo();
			commentInfo.setComment(comments.get(i).text().replaceAll(ipRegex, ""));
			commentInfo.setWiter(commentWriters.get(i).attr("user_name"));
			currentPost.addComment(commentInfo);
		}
//		이미지가 존재하면 이미지를 선택한 경로에 저장한 뒤 저장한 경로에서
//		posting 페이지에서 추가하기 위함.
		Elements imgs = doc.select("img.txc-image");
		for(int i = 0; i < imgs.size(); i++) {
			try {
				URL url = new URL(imgs.get(i).attr("src"));
				BufferedImage image = ImageIO.read(url);
				String thisImagePath = imagePath + currentPost.getNumber() + i + ".gif";
				if(image != null) {
					ImageIO.write(image, "gif", new File(thisImagePath));
					currentPost.addImagePaths(thisImagePath);
				}
			} catch (IOException e) {
			}
		}
		logs.add(currentPost.toString());
		postInfoList.add(currentPost);
		logs.add(postInfoList.toString());
		try {
			if(!currentPost.getContents().isEmpty() || currentPost.getImagePaths().size() > 0)
			postInfosQueue.put(currentPost);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

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
		for(String path : postInfo.getImagePaths()) {
			uploadImage(path, postWritingDriver, winHandleBefore);
		}
		writeContentInIframe(postInfo.getContents(), postWritingDriver);
		WebElement submitBtn = postWritingDriver.findElement(By.cssSelector("button#btn_submit"));
		kcaptcha.sendKeys(allPws);
		scrollIntoView(postWritingDriver, submitBtn);
		submitBtn.click();
		Thread.sleep(3000);
		String postedUrl = postWritingDriver.getCurrentUrl();
		Integer wroteId = Integer.parseInt(postedUrl.split("wr_id=")[1]);
		postInfoList.remove(postInfo);
		postInfo.setWroteId(wroteId);
		postInfoList.add(postInfo);
		logs.add(postInfo.toString());
		logs.add(postInfoList.toString());
		Thread.sleep(1000);
	}
	
	// DC에서 가장 최근 글을 찾기 위해서 최상단 번호를 추출함.
	public Integer getNewNumber() {
		Document doc = Jsoup.parse(postReadingDriver.getPageSource());
		Elements td_numbers = doc.select("td.t_notice");
		for (Element td_number : td_numbers) {
			Integer number = 0;
			try {
				number = Integer.parseInt(td_number.text());
				logs.add("게시글 번호 : " + number.toString());
				return number;
			} catch (NumberFormatException nfe) {
				logs.add(td_number + " : 숫자를 가지고 있지 않습니다.");
			}
		}
		return null;
	}

	// 해당하는 post의 reply 갯수를 가져옴.
	public Integer getPostsReplyCount(PostInfo postInfo) {

		return 0;
	}

	public void scrollIntoView(WebDriver driver, WebElement element) {
		runScript(driver, "arguments[0].scrollIntoView(false)", element);
	}

	public Object runScript(WebDriver driver, String script, WebElement target) {
		/*
		 * to run script shorter
		 */
		return ((JavascriptExecutor) driver).executeScript(script, target);
	}

	public static void uploadImage(String imagePath, WebDriver driver, String winHandleBefore) throws InterruptedException {
		
//		사진 첨부 버튼
		WebElement photoUploadBtn = driver.switchTo().frame(driver.findElement(By.cssSelector(".form-group iframe")))
				.findElement(By.cssSelector("button.se2_photo"));
		Thread.sleep(1000);
		photoUploadBtn.click();
//		사진 첨부 창으로 포커스 이동
		Thread.sleep(1000);
		for(String winHandle : driver.getWindowHandles()) {
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
	
	public static void writeContentInIframe(String contents, WebDriver driver) throws InterruptedException {
		Thread.sleep(3000);
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.cssSelector(".form-group iframe")));
		driver.switchTo().frame(driver.findElement(By.cssSelector("iframe#se2_iframe")));
		WebElement smartEditorTextArea =	driver.findElement(By.cssSelector("body.se2_inputarea"));
		smartEditorTextArea.sendKeys(contents);
		driver.switchTo().defaultContent();
	}
	public void chooseDirectory () {
		  DirectoryChooser directoryChooser = new DirectoryChooser(); 

          directoryChooser.setTitle("저장할 이미지의 경로를 선택해주세요.");

          //Show open file dialog

          File file = directoryChooser.showDialog(null);

         if(file!=null){
             imagePath = file.getPath(); 
        	 directoryTF.setText(imagePath);
         }
	}
}
