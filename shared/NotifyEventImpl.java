
import java.rmi.*;
import java.rmi.server.*;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
	// crea un nuovo callback client
	// struttura che memorizza il podio della classifica 
	List<String> podium;

	public NotifyEventImpl() throws RemoteException {
		super();
		podium = new ArrayList<>();
	}

	/*
	 * metodo che può essere richiamato dal servente per notificare un client di una
	 * variazione nel podio della classifica
	 */
	public void notifyEvent(List<String> podium) throws RemoteException {

		String returnMessage = "UPDATE EVENT: PODIUM HAS CHANGED";
		System.out.println(returnMessage);
		// aggiorno il podio 
		this.podium = podium;
		// devo stampare il podio aggiornato 
		System.out.println(podium);
	}
}