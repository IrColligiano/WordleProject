
import java.rmi.*;
import java.util.List;

/*
 * Interfaccia del servizio di notifica di variazioni nel podio della
 * classifica. Dichiara un metodo registerForCallback che permette ad un client
 * di registrarsi alle callback ed un metodo unregisterForCallback che permette
 * ad un client di deregistrarsi dal servizio di Callback, un metodo update che
 * notifica ai client registrati al servizio di notifica le variazioni nel podio
 * della classifica. I metodi dichiarati in questa interfaccia dovranno essere
 * definiti nelle sue implementazioni
 */
public interface NotificationService extends Remote {

	/* registrazione per la callback */
	public void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException;

	/* cancella registrazione per la callback */
	public void unregisterForCallback(NotifyEventInterface Client) throws RemoteException;

	/* aggiorna gli utenti iscritti al servizio di notifica */
	public void update(List<String> podium) throws RemoteException;

}
