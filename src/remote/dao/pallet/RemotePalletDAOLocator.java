package remote.dao.pallet;

import java.rmi.Naming;
import java.rmi.RemoteException;

import remote.dao.part.RemotePartDAOLocator;

public final class RemotePalletDAOLocator {

	public static final String PALLET_DAO_ID = "RemotePalletDAO";

	private RemotePalletDAOLocator() {}

	/**
	 * Returns a reference to a remote pallet data access object
	 * 
	 * @return a remote pallet data access object
	 * @throws RemoteException
	 **/
	public static IPalletDAO lookupDAO() throws RemoteException {
		try {
			return (IPalletDAO) Naming.lookup(PALLET_DAO_ID);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

	/**
	 * Binds a reference to a remote pallet data access object
	 * 
	 * @throws RemoteException
	 **/
	public static void bindDAO() throws RemoteException {
		try {
			Naming.rebind(PALLET_DAO_ID, new RemotePalletDAOServer(RemotePartDAOLocator.lookupDAO()));
		} catch (Exception e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

}