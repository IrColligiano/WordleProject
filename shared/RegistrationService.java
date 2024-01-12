
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

/*
 * RegistrationService è l'interfaccia del servizio di registrazione. Le
 * implementazioni di questa interfaccia dovranno definire i metodi da essa
 * esposti. RegistrationService estende Remote perchè le implementazioni di
 * questa interfaccia saranno oggetti i cui metodi potranno essere invocati da
 * remoto attraverso il meccanismo di Java RMI.
 */

public interface RegistrationService extends Remote {

	/*
	 * Metodo che aggiunge al database delle registrazioni una registrazione di un
	 * utente con credenziali (username, password), restituisce true se la
	 * registrazione è andata a buon fine, false altrimenti.
	 */
	boolean addRegistration(String username, String password) throws RemoteException;

	/*
	 * L' utente con username e password passati in input viene registrato come
	 * loggato. Restituisce true se il login è andato a buon fine, false altrimenti
	 */
	boolean setLogged(String username, String password) throws RemoteException;

	/*
	 * Se esiste un utente registrato con l' username passato in input questo metodo
	 * restituisce true, false altrimenti
	 */
	boolean isRegistered(String username) throws RemoteException;

	/*
	 * L'utente con username e password passati in input viene registrato come
	 * unlogged. Restituisce true se il logout è andato a buon fine, false
	 * altrimenti
	 */
	boolean setUnlogged(String username, String password) throws RemoteException;

	/*
	 * viene restituito l'oggetto utente presente nel database delle registrazioni,
	 * se esiste, registrato con l'username fornito in input
	 */
	public User getUser(String username) throws RemoteException;

	/*
	 * viene restituita la password, associata all' utente con username fornito in
	 * input, presente nel database delle registrazioni, se è registrato un utente
	 * con quell' username, null altrimenti
	 */
	public String getPassword(String username) throws RemoteException;

	/*
	 * restituisce la classifica
	 */
	public Classification getClassification() throws RemoteException;

	/*
	 * restituisce la struttura che implementa il db delle regiostrazioni
	 */
	public ConcurrentHashMap<String, User> getRegistration() throws RemoteException;
}
