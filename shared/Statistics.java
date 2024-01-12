
/*
 * La classe Statistiche racchiude le statistiche di interesse per un utente.
 * Le statistiche di interesse sono: partite ( numero di partite giocate
 * complessivamente da un utente), vittorie (numero di parole segrete
 * indovinate), win_rate(tasso di vittorie sul totale delle partite giocate),
 * current_win_streak (attuale striscia di vittorie consecutive), max_win_streak
 * (massima striscia di vittorie consecutive), guess_distribution (distribuzione
 * dei tentativi impiegati per arrivare ad una soluzione vittoriosa del gioco),
 * attempts (somma complessiva dei tentativi impiegati per arrivare alla
 * soluzione nei giochi vincenti).
 */

public class Statistics {

	// numero di partite giocate
	private int matches;

	// numero di partite vinte
	private int victories;

	// percentuale di partite vinte sul totale delle partite giocate
	private float win_rate;

	// lunghezza dell' attuale sequenza continua di vincite
	private int current_win_streak;

	// lunghezza della massima sequenza continua di vincite
	private int max_win_streak;

	// vettore di supporto
	// nella posizione i-1 memorizza il numero di partite vinte con i tentativi
	private int[] attempts_to_win;

	// distribuzione dei tentativi impiegati per arrivare alla soluzione del gioco
	// nelle partite vinte dal giocatore
	// l'"indice dell'array +1" (i+1) rappresenta il numero di tentativi
	// il valore presente alla posizione "indice" contiene la percentuale di partite
	// vinte con (indice+1) tentativi sul totale di partite vinte
	private float[] guess_distribution;

	// somma complessiva dei tentativi totali impiegati nelle partite vittoriose per
	// arrivare alla soluzione
	private int attempts;

	// metodo costruttore
	public Statistics() {

		// inizialmente un utente non ha giocato nessuna partita
		// inizializzo tutto a 0
		matches = 0;
		victories = 0;
		win_rate = (float) 0;
		current_win_streak = 0;
		max_win_streak = 0;
		attempts = 0;

		attempts_to_win = new int[12]; // alloco un array di 12 interi tanti quanti sono i tentativi a disposizione
		guess_distribution = new float[12]; // alloco un array di 12 float (tanti quanti sono i tentativi a
											// disposizione)

		// inizializzo i due vettori di cui sopra a 0
		for (int i = 0; i < 12; i++) {
			attempts_to_win[i] = 0;
			guess_distribution[i] = (float) 0;
		}

	}

	public synchronized int getVictories() {

		return this.victories;
	}
	public synchronized int getAttempts() {

		return this.attempts;
	}


	public synchronized Float getGuessDistribution(int i) {

		return this.guess_distribution[i];
	}

	public synchronized int getMatches() {

		return this.matches;
	}


	public synchronized float getWinRate() {

		return this.win_rate;
	}
	public synchronized float getCurrentWinStreak() {

		return this.current_win_streak;
	}

	public synchronized float getMaxWinStreak() {
		return this.max_win_streak;
	}

	public synchronized void update(boolean win, int att) {
		this.matches++;
		if (win) {
			this.attempts += att; // tengo traccia del numero di tentativi impiegati per vincere
			victories = victories + 1; // incremento di uno il numero di vittorie
			current_win_streak++; // incremento di uno la serie consecutiva di vittorie
			// incremento di 1 il numero di partite vinte con il numero di tentativi
			// passato in input, ricordarsi che l'array è shiftato a sx di 1
			// (pos 0 <--> #(vittorie_con_1_tentativo),pos 1
			// <-->#(vittorie_con_2_tentativi),...,pos 11 <-->#(vittorie_con_12_tentativi) )
			for (int i = 0; i < 12; i++) {
				if (i == att - 1)
					attempts_to_win[i] = attempts_to_win[i] + 1;
				// aggiorno il vettore con la guess distribution
				guess_distribution[i] = ((float) attempts_to_win[i] / (float) victories) * (float)100;
			}

			if (current_win_streak > max_win_streak)
				max_win_streak = current_win_streak;
		} else { // se entro nel ramo else significa che ho perso
			current_win_streak = 0; /
		}
		win_rate = ((float) victories / (float) matches) * (float) 100;

	}

	// METODI DI SUPPORTO

	// @override : riscrivo il metodo to string per la classe Statistiche
	public String toString() {
		return "\n"+ "match : " + this.matches + "\n" + "win rate : " + this.win_rate + "\n" + "current winning streak : "
				+ this.current_win_streak + "\n" + "max winning streak : " + this.max_win_streak + "\n"
				+ "distribution of attempt to win the game : " + "\n+" + "1 attempt : "
				+ String.valueOf(getGuessDistribution(0)) + "\n+" + "2 attempt : "
				+ String.valueOf(getGuessDistribution(1)) + "\n+" + "3 attempt : "
				+ String.valueOf(getGuessDistribution(2)) + "\n+" + "4 attempt : "
				+ String.valueOf(getGuessDistribution(3)) + "\n+" + "5 attempt : "
				+ String.valueOf(getGuessDistribution(4)) + "\n+" + "6 attempt : "
				+ String.valueOf(getGuessDistribution(5)) + "\n+" + "7 attempt : "
				+ String.valueOf(getGuessDistribution(6)) + "\n+" + "8 attempt : "
				+ String.valueOf(getGuessDistribution(7)) + "\n+" + "9 attempt : "
				+ String.valueOf(getGuessDistribution(8)) + "\n+" + "10 attempt : "
				+ String.valueOf(getGuessDistribution(9)) + "\n+" + "11 attempt : "
				+ String.valueOf(getGuessDistribution(10)) + "\n+" + "12 attempt : "
				+ String.valueOf(getGuessDistribution(11)) + "\n";

	}

}
