
import java.util.List;
import java.util.ArrayList;
public class Match {
	//lista di tutti i suggerimenti associati ad ogni tentativo
	private List<String> attemptsList;
	// numero di tentativi effettuato nella partita
	private int attempts;
	// flag che mi indica se la partita è terminata
	private boolean end;
	// flag che mi indica se ho vinto
	private boolean win;

	// metodo costruttore
	public Match() {
		// tentativi è un ArrayList
		attemptsList = new ArrayList<>();
		win = false; // quando viene creata la partita non è vinta
		end = false; // quando viene creata la partita non è terminata
		attempts = 0; // inizialmente 0 tentativi fatti
	}

// METODI GETTER 
// dato che la risorsa Partita potrebbe essere condivisa 
// tra più threads i metodi sono dichiarati synchronized

	// metodo che restituisce il numero di tentativi effettuati in this partita
	public synchronized int getNumAttempts() {

		return this.attempts;
	}

	public synchronized String getAttempts() {
		String result = "";
		int i = 1;
		for (String s1 : attemptsList) {
			result += i + "/12: " + s1 + "\n";
			i++;
		}
		return result;
	}
	public synchronized boolean isEnd() {

		return this.end;
	}
	public synchronized void setEnd(boolean end) {

		this.end = end;
	}

	public synchronized void setWin(boolean win) {

		this.win = win;
	}
	// metodo che restituisce il valore del flag win
	public synchronized boolean isWin() {

		return this.win;
	}

	public synchronized void addAttempt(String suggerimento) {
		// se ho già effettuato 12 tentativi non posso più aggiungere tentativi alla
		// partita
		if (this.attempts >= 12) {
			return;
		}
		// aggiungo suggerimento alla lista coi suggerimenti
		this.attemptsList.add(suggerimento);
		// incremento la variabile che conta i tentativi
		attempts++;
	}
}
