
import java.rmi.*;
import java.util.List;

/*
 * NotifyEventInterface è un' interfaccia che contiene la dichiarazione di un
 * metodo notifyEvent invocabile da remoto che serve per notificare al client il
 * verificarsi un particolare evento
 */
public interface NotifyEventInterface extends Remote {
	/*
	 * Metodo invocato dal server per notificare un evento ad un client remoto.
	 */
	public void notifyEvent(List<String> podium) throws RemoteException;
}
