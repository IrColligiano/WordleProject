
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.List;

@SuppressWarnings("serial")
public class NotificationServiceImpl extends RemoteObject implements NotificationService {
	/* lista dei client registrati */
	// realizzata come lista degli stub della classe NotifyEventInterface
	private List<NotifyEventInterface> clientsToNotify;

	/* crea un nuovo servente */
	public NotificationServiceImpl() throws RemoteException {
		super();
		clientsToNotify = new ArrayList<NotifyEventInterface>();
	};

	// registrazione per la callback
	public synchronized void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
		if (!clientsToNotify.contains(ClientInterface)) {
			clientsToNotify.add(ClientInterface);
			System.out.println("NEW CLIENT REGISTERED FOR CALLBACK");
		}
	}

	// annulla registrazione per il callback
	public synchronized void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
		if (clientsToNotify.remove(ClientInterface)) {
			System.out.println("CLIENT UNREGISTERED FOR CALLBACK");
		}
		else {
			System.out.println("ERROR: UNABLE TO UNREGISTER CLIENT");
		}
	}

	// notifica una variazione nelle prime 3 posizioni della classifica, quando
	// viene richiamato, fa le callback a tutti i client registrati
	public void update(List<String> podium) throws RemoteException {
		doCallbacks(podium);
	};

	// esegue le callback agli iscritti al servizio di notifica
	private synchronized void doCallbacks(List<String> podium) throws RemoteException {
		System.out.println("STARTING CALLBACK");
		// open an iterator sugli iscritti al servizio di notifica
		Iterator<NotifyEventInterface> i = clientsToNotify.iterator();
		while (i.hasNext()) {
			// prendo lo stub dell' oggetto remoto
			NotifyEventInterface clientsToNotify = (NotifyEventInterface) i.next();
			// invoco il metodo remoto
			clientsToNotify.notifyEvent(podium);
		}
		System.out.println("CALLBACK COMPLETE");
	}
}