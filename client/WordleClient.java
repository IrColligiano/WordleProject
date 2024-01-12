
import java.util.Scanner;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.net.*;
import java.net.UnknownHostException;
import java.io.*;

public class WordleClient {
	private String multicastAddress;
	private InetAddress multicastGroup;
	private int timeout_socket_tcp;
	private int port_multicast;
	private int port_callbackobj;
	private int port_registry;
	private int port_socket_tcp;
	private MulticastSocket multicastSocket;
	private List<String> multicastMessage;

	public class MulticastManager implements Runnable {

		@SuppressWarnings("deprecation")
		public MulticastManager(){

			// struttura vector sincronizzata per contenere i messaggi del gruppo multicast
			multicastMessage = new Vector<String>();
			try {
				multicastGroup = InetAddress.getByName(multicastAddress);
			}
			catch (UnknownHostException uhe) {
				uhe.printStackTrace();
			}
			if (!multicastGroup.isMulticastAddress()) {
				throw new IllegalArgumentException("ERROR: UNVALID MULTICAST ADDRESS");
			}

			// unione gruppo di multicast
			try {
				multicastSocket = new MulticastSocket(port_multicast);
				multicastSocket.joinGroup(multicastGroup);
				System.out.println("JOINED TO MULTICAST GROUP");
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(1000); // 1 s
					byte[] buf = new byte[2048];
					//pacchetto UDP
					DatagramPacket dp = new DatagramPacket(buf, buf.length);
					multicastSocket.receive(dp);

					// converto i byte ricevuti in una stringa
					String str = new String(buf, "ASCII");
					// System.out.println(str.trim());
					// aggiungo quello che ho letto alla lista
					// che memorizza i msg provenienti dal gruppo multicast
					multicastMessage.add(str);
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				catch (InterruptedException ie) { //sleep è bloccante
					ie.printStackTrace();
				}
			}
		}
	}

	public WordleClient(String multicastAddress, int port_multicast, int port_registry, int port_socket_tcp, int port_callbackobj, int timeout_socket_tcp) {
		this.multicastAddress = multicastAddress;
		this.port_multicast = port_multicast;
		this.port_registry = port_registry;
		this.port_socket_tcp = port_socket_tcp;
		this.port_callbackobj = port_callbackobj;
		this.timeout_socket_tcp = timeout_socket_tcp;
	}

	private void showSharing() {
		if (multicastMessage.isEmpty()) { // la struttura non contiene elem
			System.out.println("THERE ARE NO SHARE YET");
			return;
		}
		while (!multicastMessage.isEmpty()) { // la struttura contiene almeno un elem
			System.out.println(multicastMessage.get(0).trim());
			//rimuovo
			multicastMessage.remove(0);
		}
	}

	// metodo per chiedere di condividere i suggerimenti sul gruppo di multicast
	private boolean shareSuggestions(String username, String password, DataOutputStream dos, BufferedReader reader) {
		String tosend = username + "|" + password + "|" + "share";
		try {
			dos.writeInt(tosend.length());

			dos.writeBytes(tosend);

			dos.flush();

			String serverResponse = reader.readLine();

			if (serverResponse == null) {
				System.out.println("ERROR: EMPTY RESPONSE FROM SERVER");
				return false;
			}
			StringTokenizer tokenizedLine = new StringTokenizer(serverResponse, "|");
			if (!tokenizedLine.nextToken().equals(username)) {
				return false; // il server mi rimanda indietro il mio username

			}
			if (!tokenizedLine.nextToken().equals("share")) {
				return false; // il sever mi risponde con lo stesso comando

			}
			if (tokenizedLine.nextToken().equals("ok")) {
				return true;
			} else
				return false; // no se l'invio sul gruppo non è andato a buon fine
		}

		catch (IOException ioe) {
			return false;
		}

	}

	// metodo che implementa la richiesta di logout dell' utente con le credenziali
	// passate in input
	// restituisce true se e solo se il logout è andato a buon fine, false
	// altrimenti
	private boolean logout(String username, String password, BufferedReader reader, DataOutputStream dos) {

		String tosend = username + "|" + password + "|" + "logout";
		try {

			dos.writeInt(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();
			// recupera la prima linea della risposta del server
			String serverResponse = reader.readLine();
			if (serverResponse == null) {
				System.out.println("ERROR: EMPTY RESPONSE FROM SERVER");
				return false;
			}
			// System.out.println(serverResponse);
			StringTokenizer tokenizedLine = new StringTokenizer(serverResponse, "|");
			if (!tokenizedLine.nextToken().equals(username)) {
				return false;
			}
			if (!tokenizedLine.nextToken().equals("logout")) {
				return false;
			}
			if (tokenizedLine.nextToken().equals("ok")) {
				return true;
			}
			else
				return false;
		}

		catch (IOException exc) {
			return false;
		}

	}

	private boolean login(String username, String password, RegistrationService stub) {
		try {
			if (!stub.isRegistered(username)) { // controllo che sia registrato un utente con quell' username
				System.out.println("YOU ARE NOT REGISTERED YET");
				System.out.println("TO REGISTER, DIGIT 'registration'");
				return false;
			}
			// controllo che la password inserita sia uguale a quella nel database
			if (password.equals(stub.getPassword(username))) {
				// eseguo il login invocando il metodo remoto opportuno
				stub.setLogged(username, password);
				return true;
			} else {
				System.out.println("WRONG PASSWORD");
				return false;
			}

		} catch (Exception e) {
			System.out.println("ERROR: IN INVOKING OBJECT METHOD");
			e.printStackTrace();
			return false;
		}
	}

	// metodo che va a registrare un utente con le credenziali username e password
	// passate in input nel db delle registrazioni attraverso l'utilizzo di metodi
	// remoti
	// return true se e solo se la registrazione è andata a buon fine, false
	// altrimenti
	private boolean register(String username, String password, RegistrationService stub) {
		try {
			if (stub.addRegistration(username, password))
				return true;
			else
				return false;
		} catch (Exception e) {
			System.out.println("ERROR: IN INVOKING OBJECT METHOD");
			return false;
		}
	}

	// attraverso questo metodo l' utente che vuole iniziare una partita a wordle
	// lo comunica al server
	// restituisce true se e solo se la partita è stata avviata correttamente
	private boolean playWordle(String username, String password, DataOutputStream dos, BufferedReader reader) {
		// preparo la stringa da mandare al server e la invio con una write
		String tosend = username + "|" + password + "|" + "play\n";
		// la mando
		try {
			dos.writeInt(tosend.length());
			// System.out.print(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();
			//System.out.print(tosend);

			// aspetto la risposta del server

			// recupera la linea di risposta del server
			String serverResponse = reader.readLine();
			if (serverResponse == null) {
				System.out.println("ERROR: EMPTY RESPONSE FROM SERVER");
				return false;
			}
			// System.out.println(serverResponse);

			StringTokenizer tokenizedLine = new StringTokenizer(serverResponse, "|");
			if (!tokenizedLine.nextToken().equals(username))
				return false;
			if (!tokenizedLine.nextToken().equals("play"))
				return false;
			if (tokenizedLine.nextToken().equals("ok")) {
				return true;
			} else
				return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void submitWord(String username, String password, String guessedWord, BufferedReader reader, DataOutputStream dos) {
		String tips = null;
		String tosend = username + "|" + password + "|" + "submit" + "|" + guessedWord + "\n";
		try {
			dos.writeInt(tosend.length());
			// System.out.print(tosend.length());

			dos.writeBytes(tosend);
			dos.flush();

			// recupera la prima linea della risposta del server
			String serverResponse = reader.readLine();
			// System.out.println(serverResponse);
			if (serverResponse == null)
				System.out.println("ERROR: EMPTY RESPONSE FROM SERVER");
			StringTokenizer tokenizedLine = new StringTokenizer(serverResponse, "|");
			if (tokenizedLine.nextToken().equals(username)) {
				if (tokenizedLine.nextToken().equals("submit")){
					String status = tokenizedLine.nextToken();
					if (status.equals("ok")) {
						// parola indovinata
						tips = tokenizedLine.nextToken();
						// prendo la traduzione della parola segreta
						String translation = tokenizedLine.nextToken();
						System.out.println("SUGGESTIONS: " + tips);
						System.out.println("GUESSED WORD !!!!!!!!! TRANSLATION: " + translation);
					} else if (status.equals("tryagain")) {
						tips = tokenizedLine.nextToken();
						System.out.println("SUGGESTIONS: " + tips);
						System.out.println("BEEEEEEEP! WRONG WORD");
						System.out.println("TO SUBMIT A WORD, WRITE: submit + 'word' ");
					} else if (status.equals("end")) {
						tips = tokenizedLine.nextToken();
						if(!tips.equals("supercalifragilistichespiralidoso"))
							System.out.println(tips);
						String translation = tokenizedLine.nextToken();
						System.out.println("BEEEEEEEP! WRONG WORD ,YOU HAVE FINISHED YOUR ATTEMPTS\n" +
								"THE GUESSED WORD WAS: " + translation);
					} else if (status.equals("resend")) {
						System.out.println("BEEEEEEEP! WORD DOES NOT EXIST IN WORDLE'S VOCABOLARY\n"
								+ "RESEND ONE\n");
					} else if (status.equals("play")) {
						System.out.println("YOU ARE NOT IN A MATCH YET, DIGIT 'play'");
					} else if (status.equals("finished")) {
						System.out.println("YOU HAVE ALREADY PLAYED WORLE TODAY\n"
								+ "WAYT A NEW WORD\n");
					}
				}
			}
		} catch (IOException exc) {
			exc.printStackTrace();
		}

	}

	private void showClassification(String username, String password, DataInputStream dis, BufferedReader bs, DataOutputStream dos) {
		String tosend = username + "|" + password + "|" + "classification";
		// System.out.println(tosend);
		try {
			dos.writeInt(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();
			// lenght msg
			int bytes_to_read = dis.readInt();
			// System.out.println(bytes_to_read);
			// alloco un array di caratteri con tante posizioni quanti sono i byte da leggere
			char[] array = new char[bytes_to_read];
			int bytes_read = 0;
			while ((bytes_read += bs.read(array, 0, array.length)) != bytes_to_read) {
				;
			}
			// array di caratteri in una stringa
			String str = String.valueOf(array).trim(); // stringa senza spazi
			System.out.println(str);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	// metodo che stampa a schermo le statistiche
	private void sendMeStatistics(String username, String password, BufferedReader br, DataOutputStream dos) {
		String tosend = username + "|" + password + "|" + "statistics";
		try {
			dos.writeInt(tosend.length());
			dos.writeBytes(tosend);
			dos.flush();
			// alloco abbastanza spazio a contenere il to_string delle statistiche 
			char[] array = new char[2048];
			br.read(array, 0, array.length);
			String str = String.valueOf(array).trim();// da array di char a string senza spazzi
			System.out.println(str);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void start() {

		System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
		System.out.println("WELLCOME IN WORDLE");
		System.out.println("IF YOU OWN AN ACCOUNT, WRITE 'login'");
		System.out.println("ELSE, WRITE 'registration'");
		System.out.println("TO EXIT: 'exit'");
		System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
		boolean logged = false;
		//boolean registered = false;

		// apro uno scanner sullo standard input
		Scanner sc = new Scanner(System.in);
		// comando inserito da tastiera
		String cmd = null;
		// username utente
		String username = null;
		// password utente
		String password = null;
		// parola guessed_word, inviata da client a server per indovinare parola segreta
		String guessed_word = null;
		// registry dove reperire stub oggetti remoti
		Registry registry = null;
		// stub servizio di registrazione
		RegistrationService stub = null;
		// stringhe coi nomi dei servizi da reperire sul registry
		String regService = null;
		String notifService = null;
		// prendo il registro attivo su localhost alla porta
		try {
			registry = LocateRegistry.getRegistry(port_registry);
			regService = "REGISTRATION-SERVICE"; // nome servizio di registrazione
			notifService = "NOTIFICATION-SERVICE"; // nome servizio di notifica
			// prendo lo stub sul registro
			stub = (RegistrationService) registry.lookup(regService);
		}
		catch (RemoteException re) {
			re.printStackTrace();
		}
		catch (NotBoundException nbe) {
			nbe.printStackTrace();
		}
		boolean stop = false;
		// finchè non sono loggato non posso giocare
		while (!logged && !stop) {
			// leggo il comando inserito da tastiera
			cmd = sc.next();
			switch (cmd) {
			case ("exit"):
				stop = true;
				sc.close();
				System.exit(0);
				break;
			case "login":
				System.out.println("DIGIT YOUR OWN USERNAME");
				username = sc.next();
				System.out.println("DIGIT YOUR OWN PASSWORD");
				password = sc.next();
				if (this.login(username, password, stub)) {
					try { // se la login è andata a buon fine
						logged = true; // loggato
						//registered = true; // loggato --> registrato
						System.out.println("LOGIN SUCCESSFULLY");
						// reperisco lo stub del servizio di notifica dal registry
						NotificationService notificationService = (NotificationService) registry.lookup(notifService);
						/* si registra per la callback */
						System.out.println("REGISTERING FOR CALLBACK");
						// istanzio l'oggetto che serve per notificare al client
						// di una variazione della classifica
						NotifyEventInterface callbackObj = new NotifyEventImpl();
						// esporto questo oggetto
						NotifyEventInterface stub2 = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, port_callbackobj);
						// registro il client al servizio di notifica
						notificationService.registerForCallback(stub2);
						// mi iscrivo e mi metto in ascolto sul gruppo di multicast
						// attraverso l'avvio di un thread
						Thread multicastHandler = new Thread(new MulticastManager());
						multicastHandler.start();

					}
					catch (Exception e) {
						System.out.println("ERROR: IN INVOKING OBJECT METHOD");
					}
				}
				else
					System.out.println("ERROR: LOGIN FAILED");
				break;
			case "registration":
				System.out.println("DIGIT YOUR USERNAME");
				username = sc.next();
				System.out.println("DEGIT YOUR PASSWORD");
				password = sc.next();
				// la password non può essere la stringa vuota
				while (password.equals("")) {
					System.out.println("UNVALID PASSWORD, RETRY");
					password = sc.next();
				}
				// invoco il metodo opportuno per registrarsi
				if (this.register(username, password, stub)) { // se la registrazione va a buon fine
					//registered = true;
					System.out.println("YOUR ACCOUNT HAS BEEN CREATED, DIGIT: 'login'");
				} else
					System.out.println("ERROR: REGISTRATION FAILED");
				break;
			default:
				System.out.println("UNVALID COMMAND");
				System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
				System.out.println("WELLCOME IN WORDLE");
				System.out.println("IF YOU OWN AN ACCOUNT, WRITE 'login'");
				System.out.println("ELSE, WRITE 'registration'");
				System.out.println("TO EXIT: 'exit'");
				System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
			}
		}
		// CREO UNA CONNESSIONE TCP CON IL SERVER
		String hostname = "localhost"; //ip server
		Socket socket = null;

		try {

			socket = new Socket(hostname, port_socket_tcp);
			System.out.println("CONNECTED TO THE SERVER");
			socket.setSoTimeout(timeout_socket_tcp);

			// CREO UN INPUTSREAM
			InputStream in = socket.getInputStream();

			// CREO UN OUTPUTSTREAM
			OutputStream out = socket.getOutputStream();

			// CREO UN BUFFEREDREADER WRAPPER PER INPUTSTREAM
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			// CREO UN DATAOUTPUTSTREAM WRAPPER PER OUTPUTSTREAM
			DataOutputStream dos = new DataOutputStream(out);

			// CREO UN DATAINPUTSTRAM WRAPPER PER INPUTSTREAM
			DataInputStream dis = new DataInputStream(in);

			// Arrivato a questo punto l'utente è loggato
			System.out.println("HELLO " + username);
			System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
			System.out.println("THESE ARE THE COMMANDS TO INTERACT WITH WORDLE\n" +
					"TO PLAY: 'play'\n" +
					"TO LOGOUT: 'logout'\n" +
					"TO VIEW THE STATISTICS: 'statistics'\n" +
					"TO VIEW THE CLASSIFICATION: 'classification'\n" +
					"TO SHARE THE SUGGESTIONS: 'share'\n" +
					"TO VIEW THE SUGGESTIONS YOU HAVE RECEIVED: 'sharing'\n" +
					"TO EXIT: 'exit'\n" +
					"DO YOU NEED HELP? 'help'");
			System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");

			while (!stop) {
				// leggo la richiesta dell' utente da tastiera
				cmd = sc.next();
				// gestisco la richiesta che mi arriva
				switch (cmd) {
					case ("play"): // voglio iniziare una partita di wordle
						if (this.playWordle(username, password, dos, reader)) {
							// partita avviata
							System.out.println("MATCH STARTED");
							System.out.println("TO SUBMIT A WORD, DIGIT: 'submit' + 'word'");
						} else // non è stato possibile avviare la partita
							System.out.println("ERROR: UNABLE TO START A GAME");
						break;
					case ("logout"):
						if (this.logout(username, password, reader, dos)) {
							System.out.println("LOGOUT SUCCESSFUL");
						} else
							System.out.println("ERROR: LOGOUT FAILED");
						break;
					case ("exit"):
						stop = true;
						break;
					case ("submit"): // l'utente vuole inviare una parola
						guessed_word = sc.next(); // scanner
						this.submitWord(username, password, guessed_word, reader, dos);
						break;
					case ("statistics"):
						this.sendMeStatistics(username, password, reader, dos);
						break;
					case ("classification"):
						this.showClassification(username, password, dis, reader, dos);
						break;
					case ("share"):
						this.shareSuggestions(username, password, dos, reader);
						break;
					case ("sharing"):
						this.showSharing();
						break;
					default:
						System.out.println("ERROR: INVALID COMMAND");
					case ("help"):
						System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
						System.out.println("THESE ARE THE COMMANDS TO INTERACT WITH WORDLE\n" +
								"TO PLAY: 'play'\n" +
								"TO LOGOUT: 'logout'\n" +
								"TO VIEW THE STATISTICS: 'statistics'\n" +
								"TO VIEW THE CLASSIFICATION: 'classification'\n" +
								"TO SHARE THE SUGGESTIONS: 'share'\n" +
								"TO VIEW THE SUGGESTIONS YOU HAVE RECEIVED: 'sharing'\n" +
								"TO EXIT: 'exit'\n" +
								"DO YOU NEED HELP? 'help'");
						System.out.println("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
						break;
				}
			}
			System.exit(0);

		}
		catch (IOException ioe) {
			System.out.println("ERROR: COULD NOT CONNECT TO THE SERVER");
		}
		finally {
			if (socket != null) {
				try {
					socket.close();
					sc.close();
					multicastSocket.close();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}
}
