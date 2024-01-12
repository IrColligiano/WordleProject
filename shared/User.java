
public class User implements Comparable<User> {
	private String username;
	private String password;
	private boolean logged;
	private float score;
	private Statistics stats;

	public User(String username, String password) throws IllegalArgumentException, NullPointerException {

		// controllo che nessuno dei due parametri sia null
		if (username == null || password == null)
			throw new NullPointerException();

		if (password.isEmpty())
			throw new IllegalArgumentException("ERROR: PASSWORD CAN NOT BE EMPTY");

		// username e password sono quelli passati al costruttore
		this.username = username;
		this.password = password;

		// un utente quando viene registrato non è inizialmente loggato
		this.logged = false;

		// un utente quando viene registrato inizialmente ha punteggio 0
		this.score = (float) 0;

		// inizializzo le statistiche chiamando l'opportuno costruttore
		this.stats = new Statistics();
	}

	public synchronized String getPassword() {

		return this.password;
	}

	public synchronized String getUsername() {

		return this.username;
	}
	public synchronized boolean isLogged() {

		return this.logged;
	}


	public synchronized float getScore() {

		return this.score;
	}

	public synchronized Statistics getStatistics() {

		return this.stats;
	}

	public synchronized void log() {

		this.logged = true;
	}

	public synchronized void unlog() {
		this.logged = false;
	}

	public synchronized void setScore(float score) {

		this.score = score;
	}

	public synchronized void updateScore() {
		// numero di vittorie, chiamo l'opportuno metodo getter di Statistiche
		int num_won = this.stats.getVictories();

		// numero complessivo dei tentativi impiegati nelle partite vittoriose
		// chiamo l'opportuno metodo getter di Statistiche
		int num_attempts = this.stats.getAttempts();

		if (num_won != 0) { // ho vinto almeno una partita, posso fare tranquillamente la divisione di cui
									// sotto, senza correre il rischio dividere per 0
			// numero medio dei tentavi impiegati nelle vittorie
			float avg_attempts = (float) num_attempts / (float) num_won;
			// fattore di penalità calcolato come il reciproco della media_tentativi
			// media_tentativi alta --> penalty_factor piccolo (tendente a 0) --> grande
			// penalità sul punteggio
			// media_tentativi bassa --> penalty_factor tendente a 1 --> bassa penalità sul
			// punteggio
			float penalty_factor = (float) 1 / avg_attempts;
			// chiamo la setScore col punteggio appena calcolato
			this.setScore(num_won * penalty_factor);
		} else
			// non ho vinto partite --> punteggio 0
			this.setScore((float) 0);

	}

	// @override : riscrivo il metodo equals per la classe Utente
	public  boolean equals(Object obj) {
		// controllo se l'oggetto passato in input obj è un'istanza della classe Utente
		if (obj instanceof User)
			return ((User) obj).getUsername().equals(this.username);
		// se non entro nell' if significa che obj non è un oggetto della classe Utente
		// restituisco perciò false
		return false;
	}

	// @override : riscrivo il metodo toString per la classe Utente
	public  String toString() {
		return "\n" +"username: " + this.username /*+ "\n" + "password: " + "*".repeat(this.password.length()) + "\n" + "logged: " + this.logged*/
				+ "\n" + "score: " + this.score + "\n" + "statistics: " /*+ "\n"*/ + this.stats.toString() + "\n";
	}

	public  int compareTo(User u) throws NullPointerException {
		// controllo che u non sia null
		if (u == null)
			throw new NullPointerException();

		// se due utenti hanno lo stesso username sono lo stesso utente
		if (this.username.equals(u.getUsername()))
			return 0;

		// confronto i due punteggi
		float result = u.getScore() - this.score;
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		// se i due punteggi sono uguali, confronto gli username dei due utenti
		// ovvero a parità di punteggio ordino lessicograficamente
		else
			// the value 0 if the argument string is equal to this string; a value less than
			// 0 if this string is lexicographically less than the string argument; and
			// a value greater than 0 if this string is lexicographically greater than the
			// string argument.
			return this.username.compareTo(u.getUsername());

	}

}
