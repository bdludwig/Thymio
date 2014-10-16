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
	private Socket conn;
	private PrintWriter printWriter;
	private BufferedReader bufferedReader;
	
	public ThymioClient() {
		try {
			connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    
	private void connect() throws IOException {
		conn = new Socket("192.168.43.149", 6789);

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
		synchronized (this) {
			try {
				String msg = "set " + variable;

				for (int i = 0; i < data.size(); i++) {
					msg += " ";
					msg += data.get(i).toString();
				}

				write(msg);
				msg = read();
				if (!msg.startsWith("ok:")) System.out.println(msg);
			}
			catch (IOException e) {
				System.out.println("error while setVariable: " + e);
				e.printStackTrace();
			}

			notify();
		}
	}

	public synchronized void setSpeed(List<Short> data) {
		synchronized (this) {
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

			notify();
		}
	}
	
	public synchronized List<Short> getVariable(String variable) {
		synchronized (this) {
			ArrayList<Short> res = new ArrayList<Short>();

			try {
				String msg;

				write("get " + variable);
				msg = read();
				if (msg.startsWith("ok: ")) {
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

			notify();
			return res;
		}
	}

	public static void main(String [] args) {
		try {
			ThymioClient t = new ThymioClient();
			List<Short> res;
			int count;
			
			ArrayList<Short> val = new ArrayList<>();
			val.clear();
			val.add(new Short((short) 200));
			t.setVariable("motor.left.target", val);
			t.setVariable("motor.right.target", val);
			
			count = 0;
			while (count < 10) {
				Thread.sleep(250);
				res = t.getVariable("motor.left.speed");
				if (res != null) {
					for (int i = 0; i < res.size(); i++) System.out.print(res.get(i) + " ");
					System.out.print("\n");
				}
				
				count ++;
			}
			
			val.clear();
			val.add(new Short((short) 0));
			t.setVariable("motor.left.target", val);
			t.setVariable("motor.right.target", val);			
		}
		catch(Exception e) {
			System.out.println("connection failure: " + e);
		}
	}
}