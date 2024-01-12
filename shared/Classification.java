import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;

public class Classification {
	List<User> classification;
	public Classification() {
		classification = new LinkedList<User>();
	}

	public synchronized boolean addUserClassification(User u) throws NullPointerException {
		if (u == null)
			throw new NullPointerException();
		// se u non è già presente in classifica
		if (!classification.contains(u)) {
			classification.add(u); // lo aggiungo
			Collections.sort(classification);
			return true;
		}
		return false;
	}

	public synchronized boolean sort() {
		// prendo il podio prima di ordinare
		List<String> old_podium = this.getPodium();
		Collections.sort(classification);
		// prendo il podio dopo aver ordinato
		List<String> new_podium = this.getPodium();

		return !old_podium.equals(new_podium);
	}

	// restituisce la lista di stringhe di username degli utenti del podio della
	// classifica, se la classifica contiene 1 utente o 2 utenti il podio sarà
	// composto rispettivamente da 1 o 2 stringhe di username, se non ci sono utenti
	// in classifica restituisce null
	public synchronized List<String> getPodium() {
		List<String> podium = new ArrayList<>();
		if (classification.size() >= 3) {
			podium.add(0, this.classification.get(0).getUsername());
			podium.add(1, this.classification.get(1).getUsername());
			podium.add(2, this.classification.get(2).getUsername());
			return podium;
		} else if (classification.size() == 2) {
			podium.add(0, this.classification.get(0).getUsername());
			podium.add(1, this.classification.get(1).getUsername());
			return podium;
		} else if (classification.size() == 1) {
			podium.add(0, this.classification.get(0).getUsername());
			return podium;
		}
		return podium;

	}

//@override 
	public synchronized String toString() {
		String result = "";
		int i = 1;
		for (User u : classification) {
			if (!result.isEmpty())
				result += "\n";
			result += i + "°: " + u.toString();
			i++;
		}
		return result;
	}

	public synchronized List<User> getClassification() {

		return this.classification;
	}
}
