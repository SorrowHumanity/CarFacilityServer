package remote.dao.shipment;

import java.rmi.Naming;
import java.rmi.RemoteException;

import remote.dao.part.PartDAOLocator;

public final class ShipmentDAOLocator {

	public static final String SHIPMENT_DAO_ID = "RemoteShipmentDAO";

	private ShipmentDAOLocator() {}

	/**
	 * Returns a reference to a remote shipment data access object from the registry 
	 * 
	 * @return a remote shipment dao
	 * @throws RemoteException
	 **/
	public static IShipmentDAO lookupDAO() throws RemoteException {
		try {
			return (IShipmentDAO) Naming.lookup(SHIPMENT_DAO_ID);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

	/**
	 * Binds a remote shipment data access object to the registry 
	 *
	 * @throws RemoteException 
	 **/
	public static void bindDAO() throws RemoteException {
		try {
			Naming.rebind(SHIPMENT_DAO_ID, new RemoteShipmentDAOServer(PartDAOLocator.lookupDAO()));
		} catch (Exception e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

}
