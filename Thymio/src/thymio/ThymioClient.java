package thymio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ThymioClient {
	public static final int PORT = 6789;
	private Socket conn;
	private PrintWriter printWriter;
	private BufferedReader bufferedReader;
	
	public ThymioClient(String host) {
		try {
			connect(host);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (conn != null) System.out.println("connected to Thymio ...");
	}

	private void write(String message) throws IOException {
		printWriter.println(message);
		printWriter.flush();
	}

	private String read() throws IOException {
		String message;
		message = bufferedReader.readLine();
		
		return message;
    }
    

	private void connect(String host) throws IOException {
		conn = new Socket(host, ThymioClient.PORT);

		printWriter =
				new PrintWriter(
						new OutputStreamWriter(
								conn.getOutputStream()));
		
		bufferedReader =
				new BufferedReader(
						new InputStreamReader(
								conn.getInputStream()));
	}

	public synchronized void setVariable(String variable, List<Short> data) {		
		try {
			String msg = "set " + variable;

			for (int i = 0; i < data.size(); i++) {
				msg += " ";
				msg += data.get(i).toString();
			}

			write(msg);
			msg = read();
			if ((msg == null) || !msg.startsWith("ok:")) System.out.println(msg);
		}
		catch (IOException e) {
			System.out.println("error while setVariable: " + e);
			e.printStackTrace();
		}
	}

	public synchronized void setSpeed(List<Short> data) {
		try {
			String msg = "setspeed";

			for (int i = 0; i < data.size(); i++) {
				msg += " ";
				msg += data.get(i).toString();
			}

			write(msg);
			msg = read();
			if (!msg.startsWith("ok: ")) System.out.println(msg);
		}
		catch (IOException e) {
			System.out.println("error while setSpeed: " + e);
			e.printStackTrace();
		}
	}
	
	public synchronized List<Short> getVariable(String variable) {
		ArrayList<Short> res = new ArrayList<Short>();

		try {
			String msg;

			write("get " + variable);
			msg = read();
			if ((msg != null) && msg.startsWith("ok: ")) {
				String [] data = msg.substring(4).split(" ");

				for (int i = 0; i < data.length; i++) res.add(new Short(Short.parseShort(data[i])));
			}
			else {
				System.out.println(msg);
			}
		}
		catch (IOException e) {
			System.out.println("error while getVariable: " + e);
			e.printStackTrace();
		}

		return res;
	}
}
