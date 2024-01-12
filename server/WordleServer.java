
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.io.IOException;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class WordleServer {
	//DB Registrazioni
	private RegistrationServiceImpl registrationService;
	// dichiaro un oggetto della classe ExecutorService che sarà il mio threadpool
	// che eseguirà i task in arrivo al server
	//threadpool per i task in arrivo
	private ExecutorService executorService;

	private Game game;

	private Classification classification;

	private NotificationServiceImpl notificationService;

	// indirizzo gruppo multicast
	private String multicastAddress;

	// InetAddress del gruppo di multicast
	private InetAddress multicastGroup;

	// porta associata all'indirizzo multicast
	private int multicastPort;

	// porta associata al registry
	private int registry_port;

	// porta associata al socket tcp
	private int socket_tcp_port;

	// tempo da aspettare per serializzare in ms
	private int time_to_awayt;

	// porta associata all' oggetto remoto che gestisce le registrazioni
	private int registr_port;
	
	// tempo di vita di una sw
	private int time_to_refresh;

	// porta associata all' oggetto remoto che gestisce le notifiche
	private int notific_port;

	// metodo costruttore della classe WordleServer
	// settaggio dei parametri di configurazione presi da file di configurazione del
	// server

	public WordleServer(String multicastAddress, int multicastPort, int registry_port, int socket_tcp_port,
			int time_to_awayt, int registr_port, int notific_port, int time_to_refresh) {
		this.multicastAddress = multicastAddress;
		// sets the port number for multicast group
		this.multicastPort = multicastPort;
		this.registry_port = registry_port;
		this.socket_tcp_port = socket_tcp_port;
		this.time_to_awayt = time_to_awayt;
		this.registr_port = registr_port;
		this.notific_port = notific_port;
		this.time_to_refresh=time_to_refresh;
	}

	public class serverStatusSerializer implements Runnable {
		// database registrazioni
		private RegistrationService db;
		// tempo in ms che mi dice ogni quanto serializzare
		private int millis;

		// metodo costruttore
		public serverStatusSerializer (RegistrationService db, int millis) {
			// fa la cosa ovvia
			this.db = db;
			this.millis = millis;
		}

		// @overriride del metodo run
		public void run() {
			// entro in un ciclo in cui dormo, mi sveglio, serializzo e rivado a dormire
			while (true) {
				try {
					Thread.sleep(millis);
				}
				catch (InterruptedException ie) {
					ie.printStackTrace();
				}

				// path che indica la cartella di lavoro corrente (CURRENT WORKING DIRECTORY)
				String curDir = System.getProperty("user.dir");

				File logFile = new File(curDir, "config/RegistrationLog.txt");

				System.out.println("SERILIZZATION IN PROGRESS");
				try (
						FileOutputStream fos = new FileOutputStream(logFile);
						OutputStreamWriter ow = new OutputStreamWriter(fos);) { // wrapper
					// serializz
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					String jsonString = gson.toJson(db.getRegistration()); // converto il file gson in una stringa json
					// scrivo sull' OutputStream
					ow.write(jsonString);
					ow.flush();
					System.out.println("SERIALIZATION COMPLETED SUCCESSFULLY");
				}
				catch (RemoteException re) {
					re.printStackTrace();
				}
				catch (FileNotFoundException fnfe) {
					fnfe.printStackTrace();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}

			}
		}

	}

	public class shareSuggestions implements Runnable {
		private String username;
		private RegistrationServiceImpl RegistrationService;
		SelectionKey key;
		Game game;
		InetAddress multicastGroup;
		int multicastPort;

		public shareSuggestions(String username, RegistrationServiceImpl registrationService, SelectionKey key,
				Game game, InetAddress multicastGroup, int port) {
			// fa la cosa ovvia
			this.username = username;
			this.RegistrationService = registrationService;
			this.key = key;
			this.game = game;
			this.multicastGroup = multicastGroup;
			this.multicastPort = port;
		}

		// @override del metodo run
		public void run() throws NullPointerException {
			SocketChannel clientChannel = (SocketChannel) key.channel();
			String exit_code = "ok";
			String message = username + "|" + "share" + "|" + exit_code + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			int bytes_to_send = message.getBytes().length;
			int bytes_sent = 0;
			try {
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + message + "SENT TO: " + clientChannel.getRemoteAddress());
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// creo un DatagramSocket
			try (DatagramSocket datagramSocket = new DatagramSocket()) {
				Match m = this.game.getPlayerGame(RegistrationService.getUser(username));
				if (m == null)
					throw new NullPointerException("ERROR: MATCH NOT FOUND, UNABLE TO SEND SHARE");
				// preparo il DatagramPacket da inviare
				DatagramPacket datagramPacket = new DatagramPacket((username + "\n"+ m.getAttempts()).trim().getBytes("ASCII"),
						(username + "\n"+ m.getAttempts()).trim().length(), this.multicastGroup, this.multicastPort);
				// invio il datagramma sul socket
				datagramSocket.send(datagramPacket);
				System.out.printf("SENT ON MULTICAST SOCKET:\n%s\n", new String(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength()));
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public class submit implements Runnable {
		private String username;
		SelectionKey key;
		Game game;
		String gw;
		User u;

		//
		public submit(String username, SelectionKey key, Game game, String gw) {
			this.username = username;
			this.key = key;
			this.game = game;
			this.gw = gw;
			this.u = registrationService.getUser(username);
		}

		// @override del metodo run
		public void run() throws NoSuchElementException {
			if (u == null)
				throw new NoSuchElementException("ERROR: USER NOT FOUND " + username );
			String code; // contiene l'esito della richiesta
			String suggestions = "supercalifragilistichespiralidoso"; // valore di default quando non devo dare suggerimenti
			SocketChannel clientChannel = (SocketChannel) key.channel();
			Match m = game.getPlayerGame(u);
			// controllo che u stia giocando alla sessione corrente
			Boolean in_game = (m != null);
			if (in_game) { // se sto giocando
				// controllo che la partita non sia finita 
				Boolean partita_finita = m.isEnd();
				if (!partita_finita) {
					int in_dictionary = game.inVocaboulary(gw);
					if (in_dictionary >=0) { // se la gw è nel dizionario
						suggestions = game.suggestions(gw); // prendo la stringa con i suggerimenti
						boolean test = game.test(u, gw); // testo se ho indovinato la parola
						if (test)
							code = "ok"; // se ho vinto restituisco ok
						else if (game.getPlayerGame(u).isEnd())
							code = "end"; // se non ho vinto e ho esaurito i tentativi
						else
							code = "tryagain"; //non ho vinto ma ho ancora tentativi
					} else
						code = "resend"; // rinviare la parola non è nel dizionario
				} else
					code = "finished";
			} else
				code = "play"; // gioca per la sessione corrente
			// preparo il messaggio da inviare
			String message;
			if (code.equals("end") || code.equals("ok"))
				message = username + "|" + "submit" + "|" + code + "|" + suggestions + "|" + game.getTranslation()
						+ "\n";
			else
				message = username + "|" + "submit" + "|" + code + "|" + suggestions + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			int bytes_to_send = message.getBytes().length;
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + message + "SENT TO: " + clientChannel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	// task che serve per avviare una partita nella sessione corrente del gioco 
	public class play implements Runnable {
		// username dell' utente
		private String username;
		// selection key del channel che era Readable
		SelectionKey key;
		// gioco
		Game game;
		// Utente
		User u;

		// metodo costruttore
		public play(String username, SelectionKey key, Game game) {
			// fa la cosa ovvia
			this.username = username;
			this.key = key;
			this.game = game;
			this.u = registrationService.getUser(username);
		}

		// @override del metodo run
		public void run() {
			boolean exit_status = true;
			// controllo se u ha già giocato o sta già giocando alla sessione corrente del
			// gioco
			if (game.alreadyPlayed(u))
				exit_status = false;
			if (exit_status) {
				// creo una partita per l'utente e comunico al client che può iniziare a giocare
				game.addPlayers(u);
				System.out.println(username + " HAS STARTED A MATCH ");
			}
			String exit_code;
			if (exit_status)
				exit_code = "ok"; // partita iniziata con successo
			else
				exit_code = "no"; // non è stato possibile iniziare la partita

			// prendo il canale associato alla key
			SocketChannel clientChannel = (SocketChannel) key.channel();
			// preparo il messaggio
			String message = username + "|" + "play" + "|" + exit_code + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			// numero di byte da inviare
			int bytes_to_send = message.getBytes().length;
			// numero di byte inviati
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + message + "SENT TO: " + clientChannel.getRemoteAddress());
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}

	}

	// task che gestisce la richiesta di logout di un Utente
	public class logout implements Runnable {
		// username dell' utente
		private String username;
		// password dell' utente
		private String password;
		// database utenti registrati
		private RegistrationServiceImpl registrationService;
		// selection key del channel che era Readable
		SelectionKey key;
		// gioco di Wordle
		Game game;
		// Utente
		User u;

		// metodo costruttore
		public logout(String username, String password, RegistrationServiceImpl RegistrationService, SelectionKey key,
				Game game) {
			// fa la cosa ovvia
			this.username = username;
			this.password = password;
			this.registrationService = RegistrationService;
			this.key = key;
			this.game = game;
			this.u = RegistrationService.getUser(username);
		}

		// @override del metodo run che definisce il task
		public void run() {
			// flag dove vado a registrare il buon esito o meno dell' operazione di unlog
			boolean exit_status;
			// unloggo l'utente chiamando l'opportuno metodo fornito da RegistrationService
			exit_status = registrationService.setUnlogged(username, password);
			// prendo il canale associato alla key
			SocketChannel clientChannel = (SocketChannel) key.channel();
			String exit_code;
			if (exit_status == true)
				exit_code = "ok";
			else
				exit_code = "no";
			// preparo il msg
			String message = username + "|" + "logout" + "|" + exit_code + "\n";
			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			int bytes_to_send = message.getBytes().length;
			int bytes_sent = 0;
			// scrivo sul channel la sequenza di byte
			try {
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + message + "SENT TO: " + clientChannel.getRemoteAddress());
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// se l'utente stava giocando una partita non terminata il logout la termina
			if (exit_status)
				game.endMatch(u);
		}
	}

	// task che gestisce l' invio delle statistiche
	public class sendStatistics implements Runnable {
		String username;
		String password;
		RegistrationService db;
		SelectionKey key;

		public sendStatistics(String username, String password, RegistrationService db, SelectionKey key) {
			this.username = username;
			this.password = password;
			this.db = db;
			this.key = key;
		}

		// @override
		public void run() throws NoSuchElementException {
			try {
				User u = db.getUser(username);
				Statistics stat = u.getStatistics();
				//toString delle Statistiche
				String msg = stat.toString();
				SocketChannel clientChannel = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
				int bytes_to_send = msg.getBytes().length;
				int bytes_sent = 0;
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + msg + "SENT TO: " + clientChannel.getRemoteAddress());
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	// task che gestisce l'invio della classifica
	public class sendClassification implements Runnable {
		RegistrationService db;
		SelectionKey key;

		public sendClassification (RegistrationService db, SelectionKey key) {
			this.db = db;
			this.key = key;
		}

		public void run() {
			try {
				// prendo la classifica e faccio il toString
				String msg = db.getClassification().toString();
				// prima invio un intero che codifica la lunghezza della classifica 
				int msgLength = msg.length();
				SocketChannel clientChannel = (SocketChannel) key.channel();
				byte[] bytes = ByteBuffer.allocate(4).putInt(msgLength).array();
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				int bytes_to_send = bytes.length;
				int bytes_sent = 0;
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				//System.out.println("MESSAGE LENGHT " + msgLength + "\n" + "SENT TO: " + clientChannel.getRemoteAddress());
				System.out.flush();
				// poi invio la classifica 
				buffer = ByteBuffer.wrap(msg.getBytes());
				bytes_to_send = msg.getBytes().length;
				bytes_sent = 0;
				while ((bytes_sent += clientChannel.write(buffer)) != bytes_to_send) {
					;
				}
				System.out.println("MESSAGE: " + msg + "SENT TO: " + clientChannel.getRemoteAddress());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	// metodo che viene eseguito per avviare il server
	public void start() {
		try {
			int procs = Runtime.getRuntime().availableProcessors();
			//System.out.println("CORES AVALIABLE: " + procs);
			executorService = Executors.newFixedThreadPool(procs);
			//System.out.println("THREADPOOL CREATED");

			classification = new Classification();
			//System.out.println("CLASSIFICATION CREATED");

			registrationService = new RegistrationServiceImpl(classification);
			//System.out.println("REGISTARTION SERVICE CREATED");

			notificationService = new NotificationServiceImpl();
			//System.out.println("NOTIFICATION SERVICE CREATED");

			game = new Game(classification, notificationService, time_to_refresh);
			System.out.println("GAME CREATED, NEW WORDLE'S WORD = " + game.getSecretWord() + " ITALIAN TRASLATION : " + game.getTranslation());

			// Esportazione degli oggetti remoti
			RegistrationService skeleton = (RegistrationService) UnicastRemoteObject.exportObject(registrationService, registr_port);
			NotificationService skeleton2 = (NotificationService) UnicastRemoteObject.exportObject(notificationService, notific_port);

			LocateRegistry.createRegistry(registry_port);
			Registry reg = LocateRegistry.getRegistry(registry_port);

			// Pubblicazione degli skeleton nel registry
			reg.rebind("REGISTRATION-SERVICE", skeleton);
			reg.rebind("NOTIFICATION-SERVICE", skeleton2);

			this.multicastGroup = InetAddress.getByName(multicastAddress);
			if (!this.multicastGroup.isMulticastAddress())
				throw new IllegalArgumentException("ERROR: UNVALID MULTICAST ADDRESS");

			System.out.println("READY\n");
			// thread per la serializzazione
			Thread serializer = new Thread(new serverStatusSerializer(registrationService, time_to_awayt));
			serializer.start();
		}

		catch (RemoteException re) {
			System.out.println("ERROR: COMUNICATION " + re.toString());
		}
		catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		}
		//Listening socket per richieste di connessione
		ServerSocketChannel serverChannel;
		// dichiaro un selettore che servirà per il multiplexing dei canali
		Selector selector;

		try {
			// apro un channel per il listening socket
			serverChannel = ServerSocketChannel.open();
			// prendo il ServerSocket associato al ServerSocketChannel aperto sopra
			ServerSocket ss = serverChannel.socket();
			// metto in ascolto il listening socket in localhost sulla porta indicata
			// Creates a socket address where the IP address is the wildcard address and the
			// port number a specified value. The wildcard is a special local IP address
			InetSocketAddress address = new InetSocketAddress(socket_tcp_port);
			ss.bind(address);
			// confinguro il channel a non bloccante
			serverChannel.configureBlocking(false);
			// apro il selettore e registro il serversocketchannel su questo selettore per
			// le operazioni di ACCEPT
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}

		// ciclo infinito di  monitoraggio sui channel associati alla select
		while (true) {
			try {
				// Selects a set of keys whose corresponding channels are ready for
				// I/O operations.
				selector.select();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
				break;
			}

			// prendo il set dei channel "pronti" o meglio le chiavi/i token corrispondenti
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			// prendo un iteratore su questo set
			Iterator<SelectionKey> iterator = readyKeys.iterator();

			// ciclo finchè ci sono elem nell' iteratore
			while (iterator.hasNext()) {
				// prendo il next
				SelectionKey key = iterator.next();
				// rimuove la chiave dal Selected Set, ma non dal Registered Set
				iterator.remove();
				try {
					// Il canale è pronto per accettazione di connessioni
					if (key.isAcceptable()) {
						// prendo il server socket channel associato alla key in questione
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						// faccio un' accept su quel server socket che mi restituirà il socketchannel
						// per la comunicazione col client
						SocketChannel client = server.accept();
						System.out.println("ACCEPTED CONNECTION FROM " + client);
						// configuro il SocketChannel appena creato come non bloccante
						client.configureBlocking(false);
						// alloca un byte buffer con tanto spazio per contenere un intero
						ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
						// alloco un byte buffer con tanto spazio per contenere un msg
						ByteBuffer message = ByteBuffer.allocate(2048);
						// metto tutto in un array di ByteBuffer
						ByteBuffer[] bfs = { length, message };
						// registro il SocketChannel appena creato sul selettore per le operazioni di
						// lettura e aggiunge l'array di bytebuffer [lenght, message] come attachment
						client.register(selector, SelectionKey.OP_READ, bfs);
					}

					// Il canale è pronto in lettura
					if (key.isReadable()) {
						// recupero il SocketChannel associato alla chiave
						SocketChannel client = (SocketChannel) key.channel();
						// recupera l'array di bytebuffer (attachment)
						ByteBuffer[] bfs = (ByteBuffer[]) key.attachment();
						// nel protocollo stabilito il client invia messaggi al server del tipo
						// username/password/cmd[/valore] dove valore è opzionale , sarà presente solo
						// per determinati comandi (cmd)
						String username = null;
						String password = null;
						String cmd = null;
						String gw = null;

						// leggo dal socket channel nel buffer bfs in maniera non bloccante
						client.read(bfs);
						// se ho letto tutto l'intero nel primo ByteBuffer
						if (!bfs[0].hasRemaining()){
							// mi preparo a leggere dal buffer facendo una flip
							bfs[0].flip();
							// prendo l'intero che mi dice la length in byte del msg che segue
							int l = bfs[0].getInt();
							// System.out.println(l);
							if (bfs[1].position() == l) { // ho letto tutto quello che c'era da leggere
								bfs[1].flip();
								//traformo in stringa il bytebuffer[1]
								String message = new String(bfs[1].array()).trim();
								// System.out.printf(message + "\n");
								// rinizializzo il ByteBufferArray per successive comunicazioni
								ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
								ByteBuffer messagenew = ByteBuffer.allocate(2048);
								ByteBuffer[] bfsnew = { length, messagenew };
								key.attach(bfsnew);

								// tokenizzo la stringa contenente il msg
								StringTokenizer tokenizedLine = new StringTokenizer(message, "|");
								if (tokenizedLine.hasMoreTokens()) {
									username = tokenizedLine.nextToken();
								}
								else {
									System.err.println("ERROR: USER NOT FOUND");
									throw new NoSuchElementException();
								}
								if (tokenizedLine.hasMoreTokens()) {
									password = tokenizedLine.nextToken();
								}
								else {
									System.err.println("ERROR: PASSWORD NOT FOUND");
									throw new NoSuchElementException();
								}
								if (tokenizedLine.hasMoreTokens()) {
									cmd = tokenizedLine.nextToken();
								}
								else {
									System.err.println("ERROR: COMMAND NOT FOUND");
									throw new NoSuchElementException();
								}
								if (!registrationService.isRegistered(username)) {
									System.err.println("ERROR: USER NOT REGISTERED"+ username);
									break;
								}
								if (!registrationService.getUser(username).isLogged()) {
									System.err.println("ERROR: USER NOT LOGGED"+ username);
									break;
								}
								switch (cmd) {
									case ("logout"):
										executorService.execute(new logout(username, password, registrationService, key, game));
										break;
									case ("play"):
										executorService.execute(new play(username, key, game));
										break;
									case ("submit"):
										if (tokenizedLine.hasMoreTokens()) {
											gw = tokenizedLine.nextToken();
											executorService.execute(new submit(username, key, game, gw));
										}
										else {
											System.err.println("ERROR: GUESSED WORD NOT FOUND");
											throw new NoSuchElementException();
										}
										break;
									case ("statistics"):
										executorService.execute(new sendStatistics(username, password, registrationService, key));
										break;
									case ("classification"):
										executorService.execute(new sendClassification(registrationService, key));
										break;
									case ("share"):
										executorService.execute(new shareSuggestions(username, registrationService, key, game, multicastGroup, multicastPort));
									break;
								}
							}
						}
					}
				}
				catch (IOException ioe) {
					key.cancel();
					try {
						key.channel().close();
					}
					catch (IOException ioe2) {
					}
				}
			}
		}
	}
}
