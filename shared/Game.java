import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import org.json.JSONException;
import org.json.JSONObject;

public class Game {
	private String secretWord;
	// partite in corso
	private Map<User, Match> players;

	private List<String> words;
	private Classification c;
	private String translation;
	private NotificationServiceImpl ns;
	private int time_to_awayt;
	private class refreshGame implements Runnable {
		// tempo di validità della parola segreta
		int time_to_await;
		public refreshGame(int millis) {
			time_to_await = millis;
		}

		// @override del metodo run
		public synchronized void run() {
			while (true) {
				try {
					Thread.sleep(time_to_await); // dormo per tanto tempo quanto deve rimanere in vita la parole corrente
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("REFRESHING THE GAME");
				//chiudo la vecchia sessione di gioco, partite non finite sono automaticamnte perse
				endGame();
				resetPlayer();
				String new_sw = generateRandomWord();
				setSecretWord(new_sw);
				String translation = Translate(getSecretWord());
				setTranslation(translation);
				System.out.println("NEW GAME STARTED : \n" + "SECRET WORD: " + secretWord
						+ " ITALIAN TRASLATION: " + getTranslation());
			}
		}
	}

	public Game(Classification c, NotificationServiceImpl ns, int time_to_awayt) throws NullPointerException {

		if (c == null || ns == null)
			throw new NullPointerException();
		this.ns = ns;
		this.c = c;
		this.time_to_awayt = time_to_awayt;
		words = new LinkedList<>();


		// ConcurrentHashMap che avrà come elementi tuple <Utente,Partita>
		players = new ConcurrentHashMap<>();

		try (BufferedReader br = new BufferedReader(
				new FileReader(new File(System.getProperty("user.dir"), "config/WordsDB.txt")))) {
			String line;
			while ((line = br.readLine()) != null) { // prendo una linea alla volta sino a che non raggiungo
														// l'end-of-the-stream
				// aggiungo la parola presente in quella linea alla mia struttura
				words.add(line.trim());
			}
		}
		catch (FileNotFoundException exc) {
			exc.printStackTrace();
		}
		catch (IOException exc) {
			exc.printStackTrace();
		}
		this.secretWord = this.generateRandomWord();
		this.translation = this.Translate(secretWord);
		//il thread che si occuperà di fare il refresh del gioco
		Thread t = new Thread(new refreshGame(this.time_to_awayt));
		t.start();
	}
	public synchronized String getSecretWord() {

		return this.secretWord;
	}
	public synchronized String getTranslation() {

		return this.translation;
	}

	public synchronized Match getPlayerGame(User player) throws NullPointerException {
		if (player == null)
			throw new NullPointerException();
		return players.get(player);// ritorna il match associato ad un utente
	}

	public synchronized Classification getClassification() {
		return this.c;
	}
	public synchronized boolean alreadyPlayed(User u) throws NullPointerException {
		if (u == null)
			throw new NullPointerException();
		// Returns true if this map contains a mapping for the specifiedkey.
		return this.players.containsKey(u);
	}
	/*
	public boolean inVocaboulary(String word) throws NullPointerException {
		if (word == null)
			throw new NullPointerException();
		//return this.words.contains(word.trim());
	}
	*/
	public int inVocaboulary(String word) throws NullPointerException {
		if (word == null)
			throw new NullPointerException();
		int index = Collections.binarySearch(words,word.trim());
		return index;

	}

	public synchronized void addPlayers(User player) throws NullPointerException, IllegalArgumentException {
		if (player == null)
			throw new NullPointerException();
		if (this.alreadyPlayed(player))
			throw new IllegalArgumentException(player.getUsername() + " ERROR: THE PLAYER IS ALREADY IN A MATCH");
		{
		}
		// inserisco nella map il giocatore e inizializzo una nuovo oggetto partita a lui associato
		players.put(player, new Match());
	}
	public synchronized void resetPlayer() {
		// nuova HashMap
		this.players = new ConcurrentHashMap<>();
	}
	public synchronized void setSecretWord(String newSecretWord) {
		this.secretWord = newSecretWord;
	}

	public synchronized void setTranslation(String word) {
		this.translation = word;
	}
	public synchronized String suggestions(String guessedWord) {
		String suggestionString = "";

		for (int i = 0; i < guessedWord.length(); i++) {
			if (secretWord.contains("" + guessedWord.charAt(i)))
				// controllo se i caratteri i-esimi della gw e della sw sono uguali
				if (("" + guessedWord.charAt(i)).equals("" + secretWord.charAt(i)))
					suggestionString = suggestionString.concat("[T]"); // lettera indovinata e nella posizione giusta

				else {
					suggestionString = suggestionString.concat("[?]"); // lettera indovinata ma nella posizione sbagliata
				}
			else
				suggestionString = suggestionString.concat("[F]"); // lettera sbagliata
		}
		return suggestionString;
	}


	@SuppressWarnings("deprecation")
	public synchronized String Translate(String word) throws NullPointerException {
		// controllo che word non sia null
		if (word == null)
			throw new NullPointerException();
		String translation = null;
		try {
			// inserisco la parola da tradurre (word) nella query della URL http
			URL url = new URL("https://api.mymemory.translated.net/get?q=" + word + "!&langpair=en|it");
			// apro uno stream verso quella url,lo avvolgo prima in un InputStreamReader e
			// poi in un BufferedReader
			//apertura Inputstream verso url,
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

			char[] array = new char[2048];
			// leggo i caratteri nell'array fino alla fine dello stream
			// -1 rappresenta l'end-of-the-stream
			while (in.read(array) != -1) {
				;
			}
			// converto l'array di caratteri in una stringa
			String response_json = new String(array);
			// converto la stringa in un oggetto JSON
			JSONObject obj = new JSONObject(response_json);
			// prende la traduzione cercando nei campi giusti della risposta
			translation = obj.getJSONObject("responseData").getString("translatedText");
		}
		catch (MalformedURLException murle) {
			murle.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		catch (JSONException jsone) {
			jsone.printStackTrace();
		}
		return translation;
	}

	// genera e retituisce una parola random tra quelle presenti nel dizionario
	public synchronized String generateRandomWord() {
		int randIndex = (int) (Math.random() * (double) (this.words.size() - 1));
		// restituisco la parola del dizionario che sta in posizione randIndex
		return this.words.get(randIndex);
	}

	// metodo che termina abrupt una sessione di gioco
	// termina forzatamente le partite ancora in corso
	// considerandole perse
	public synchronized void endGame() {
		// tutte le partite non terminate vengono considerate perse
		for (Map.Entry<User, Match> entry : this.players.entrySet()) { // scorro con un for generalizzato tutte le
			// entry della map
			if (!entry.getValue().isEnd()) { // se la partita non è finita
				User u = entry.getKey();
				Match m = entry.getValue();
				m.setEnd(true); // segno che è terminata
				m.setWin(false);// segno la partita come persa
				u.getStatistics().update(false, m.getNumAttempts()); // aggiorno le statistiche
				u.updateScore();// aggiorno lo score
			}
		}
		// la classifica potrebbe essere cambiata, quindi la ordino
		if (this.c.sort()) // se è variato il podio invio le notifiche
			try {
				ns.update(c.getPodium());
			} catch (RemoteException re) {

				re.printStackTrace();
			}
	}

	// metodo che termina il match dell' utente u se è ancora in corso nell'attuale
	// sessione del gioco
	// throws NoSuchElementException se u non sta giocando alla sessione corrente
	// del gioco
	public synchronized void endMatch(User u) throws NullPointerException, NoSuchElementException {
		if (u == null)
			throw new NullPointerException();
		if (this.players.containsKey(u)) {
			Match m = this.players.get(u);
			if (!m.isEnd()) { // partita non  terminata
				m.setEnd(true);
				m.setWin(false);
				u.getStatistics().update(false, m.getNumAttempts());
				u.updateScore();
				if (this.c.sort())
					try {
						ns.update(c.getPodium());
					}
				catch (RemoteException re) {
						re.printStackTrace();
					}
			}
		}
		else
			throw new NoSuchElementException(u.getUsername() + " ERROR: HE IS NOT YET ATTENDING THE MATCH");
	}

	/*
	 * metodo che dato un Utente u e una String guessedWord testa o meno la vittoria
	 * del gioco, ovvero se la guessedWord proposta dall' Utente u corrisponde alla
	 * secret word
	 *
	 * @param u           Utente who sends the guessedWord, bisogna controllare a
	 *                    priori che l' utente passato in input abbia chiesto di
	 *                    giocare al gioco corrente e, se questo è avvenuto, che non
	 *                    abbia raggiunto il limite massimo di tentativi disponibili
	 *                    per indovinare la parola segreta
	 *
	 * @param guessedWord sent by user u, ricordarsi di controllare a priori che la
	 *                    parola sia della lunghezza giusta (10 caratteri )e che sia
	 *                    presenti nel dizionario
	 *
	 * @return true <--> guessed , false <--> not guessed
	 */
	public synchronized boolean test(User u, String guessedWord)
			throws NullPointerException, NoSuchElementException, IllegalArgumentException {
		if (u == null || guessedWord == null)
			throw new NullPointerException();
		if (!players.containsKey(u))
			throw new NoSuchElementException(u.getUsername() + " ERROR: HE IS NOT YET ATTENDING THE MATCH");
		if (guessedWord.length() != 10)
			throw new IllegalArgumentException(guessedWord + " ERROR: WORD LENGTH");
		if (this.inVocaboulary(guessedWord) < 0)
			throw new IllegalArgumentException(guessedWord + " ERROR: NOT IN THE WORDLE'S DICTIONARY");
		Match m = this.players.get(u);
		// se la partita non è finita
		if (!m.isEnd()) {
			// aggiungo il tentativo alla lista dei tentativi della partita
			// memorizzando la stringa dei suggerimenti associata alla gw inviata
			m.addAttempt(this.suggestions(guessedWord));
			// controllo se ho vinto
			if (guessedWord.equals(secretWord)) { // se la gw e la sw sono uguali
				m.setEnd(true); // la partita è finita
				m.setWin(true); // ho vinto
			} else if (m.getNumAttempts() == 12) { // non ho indovinato controllo se ho esaurito i tentativi a disposizione
				m.setEnd(true); // la partita è finita
				m.setWin(false); // ho perso
			}
			if (m.isEnd()) { // se la partita è finita, vinta o persa
				// aggiorno le statistiche dell'utente
				u.getStatistics().update(m.isWin(), m.getNumAttempts());
				// aggiorno il punteggio dell' utente
				u.updateScore();
				if (this.c.sort()) // aggiorno la classifica e se varia il podio
					try {
						ns.update(c.getPodium()); // faccio inviare una notifica dal servizio di notifica
					}
					catch (RemoteException re) {
						re.printStackTrace();
					}
			}
		}
		return m.isWin();
	}
}