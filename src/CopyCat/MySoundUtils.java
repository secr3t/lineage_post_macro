package CopyCat;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MySoundUtils implements LineListener{

	private boolean playCompleted = false;
	
	public void playSoundClip(String filePath) {
		File audioFile = new File(filePath);
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
			AudioFormat format = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			Clip audioClip = (Clip) AudioSystem.getLine(info);
			audioClip.addLineListener(this);
			audioClip.open(audioInputStream);
			audioClip.start();
			while(!playCompleted) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			audioClip.close();
			audioInputStream.close();
			playCompleted = false;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void update(LineEvent event) {
			LineEvent.Type type = event.getType();
			if(type == LineEvent.Type.START) {
				playCompleted = false;
			} else if (type == LineEvent.Type.STOP) {
				playCompleted = true;
			}
	}
/*	public static void main(String[] args) {
		String audioFilePath = "D:\\java_output\\lineage_copycat\\src\\beep.wav";
		MySoundUtils util = new MySoundUtils();
				util.playSoundClip(audioFilePath);
	}*/
}
