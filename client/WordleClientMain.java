
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

public class WordleClientMain {
	public static void main(String[] args) {
		File configFile = new File(System.getProperty("user.dir"), "config/ClientConfigurationFile.properties");
		FileInputStream fileStream = null;
		Properties prop = new Properties();
		try {
			fileStream = new FileInputStream(configFile);
			prop.load(fileStream);
			WordleClient client = new WordleClient(prop.getProperty("multicast_address"),
					Integer.parseInt(prop.getProperty("multicast_port")),
					Integer.parseInt(prop.getProperty("registry_port")),
					Integer.parseInt(prop.getProperty("socket_tcp_port")),
					Integer.parseInt(prop.getProperty("callback_obj")),
					Integer.parseInt(prop.getProperty("socket_tcp_timeout")));

			client.start();
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (fileStream != null) {
				try {
					fileStream.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}