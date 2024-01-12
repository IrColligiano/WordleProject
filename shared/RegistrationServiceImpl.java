
import java.rmi.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.IOException;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RegistrationServiceImpl implements RegistrationService {

	/* Store data in a hashtable */
	// userò una hashtable di tuple <username,Utente>
	private ConcurrentHashMap<String, User> RegistrationDB;
	// classifica
	private Classification c;

	/* Constructor - set up database */
	public RegistrationServiceImpl(Classification classification) throws RemoteException {
		RegistrationDB = new ConcurrentHashMap<String, User>();
		this.c = classification;
		// se esiste il file con le registrazioni effettuate in passato le carico sulla
		// struttura del mio programma che implementa il database
		File file = new File(System.getProperty("user.dir"), "config/RegistrationLog.txt");
		if (file.exists()) {
			Reader r = null;
			try {
				r = Files.newBufferedReader(Paths.get(System.getProperty("user.dir"), "config/RegistrationLog.txt"));
			}
			catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// deserializzazione
			Type mapOfMyClassObject = new TypeToken<ConcurrentHashMap<String, User>>() {// definisce il tipo per il json
			}.getType();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			RegistrationDB = gson.fromJson(r, mapOfMyClassObject);
			for (Map.Entry<String, User> entry : this.RegistrationDB.entrySet()) { // scorro con un for generalizzato tutte le entry della map
				c.addUserClassification(entry.getValue());
			}
		}
	}

	// metodo per aggiungere la registrazione dell' utente, avente come credenziali
	// la coppia (username, password) al db delle registrazioni, restituisce un
	// booleano che mi dice se la registrazione è andata a buon fine.
	public synchronized boolean addRegistration(String username, String password) {
		if (password.equals("")) {
			System.err.println("ERROR: EMPTY PASSWORD");
			return false;
		}
		if (RegistrationDB.containsKey(username)) {
			System.err.println("ERROR: THIS USERNAME IS ALREADY EXISTS");
			return false;
		}
		RegistrationDB.put(username, new User(username, password));
		// prendo l'utente appena creato dal database
		User u = RegistrationDB.get(username);
		//lo metto in classifica
		c.addUserClassification(u);
		System.out.printf("NEW USER REGISTERED:\n"+"USERNAME: %s  PASSWORD: %s\n",
				RegistrationDB.get(username).getUsername(), RegistrationDB.get(username).getPassword());
		return true;
	}


	public synchronized boolean setLogged(String username, String password) {

		// controllo che effettivamente sia registrato un utente con quell'username
		if (!this.isRegistered(username))
			return false;

		// reperisco l'utente registrato con quell'username
		User u = this.getUser(username);

		// controllo che l'utente non sia già loggato
		if (u.isLogged())
			return false;

		// controllo la validità delle credenziali con cui voglio fare login
		if (!u.getPassword().equals(password))
			return false;

		RegistrationDB.get(username).log();

		if (RegistrationDB.get(username).isLogged()) {// entro nel corpo dell'if solo se il flag logged è stato settato a true
			System.out.println("USER LOGGED : "+ RegistrationDB.get(username).getUsername());
			return true;
		}
		return false;
	}

	// (realizza il logout) restituisce true se e solo se l'operazione è andata a
	// buon fine
	public synchronized boolean setUnlogged(String username, String password) {
		// controllo che effettivamente sia presente un utente con quell'username
		if (!this.isRegistered(username))
			return false;

		// reperisco l'utente registrato con quell'username
		User u = this.getUser(username);

		// controllo la validità delle credenziali con cui voglio fare login
		if (!u.getPassword().equals(password))
			return false;

		// controllo che l'utente non sia già unlogged
		if (!u.isLogged())
			return false;

		// reperisco l'oggetto utente associato ad username con una get e chiamo il
		// metodo unlog
		RegistrationDB.get(username).unlog();
		if (!RegistrationDB.get(username).isLogged()) {// entro nel corpo dell'if solo se il flag logged è stato settato
														// a false
			System.out.println("USER: UNLOGGED " + RegistrationDB.get(username).getUsername());
			return true;
		}
		return false;

	}

	public synchronized User getUser(String username) {
		// se è registrato un utente con username
		if (this.isRegistered(username))
			return RegistrationDB.get(username);
		else {
			System.err.println("ERROR: USER NOT EXIST IN DATABASE "+ username);
			return null;
		}
	}

	public synchronized String getPassword(String username) {

		User u = this.getUser(username);
		if (u != null)
			return u.getPassword();

		else {
			System.err.println("ERROR: USER NOT EXIST IN DATABASE "+ username);
			return null;
		}
	}

	public synchronized boolean isRegistered(String username) {

		return RegistrationDB.containsKey(username);
	}

	public synchronized Classification getClassification() throws RemoteException {
		return this.c;
	}

	public synchronized ConcurrentHashMap<String, User> getRegistration() throws RemoteException {
		return this.RegistrationDB;
	}

}