package CopyCat;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.assertthat.selenium_shutterbug.core.Shutterbug;

public class TestShutterBug {

	public static void main(String[] args) {
		System.setProperty("webdriver.chrome.driver", "src/chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		
		driver.get("https://www.naver.com");
		
		Shutterbug.shootPage(driver).withName("test").save("D:\\Documents\\test\\");
		
		driver.quit();
	}
}
