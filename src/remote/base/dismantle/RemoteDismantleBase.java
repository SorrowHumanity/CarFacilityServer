package remote.base.dismantle;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import dto.pallet.PalletDTO;
import dto.part.PartDTO;
import remote.dao.pallet.IPalletDAO;
import remote.dao.part.IPartDAO;
import remote.domain.car.ICar;
import remote.domain.pallet.IPallet;
import remote.domain.pallet.RemotePallet;
import remote.domain.part.IPart;
import remote.domain.part.RemotePart;
import util.CollectionUtils;

public class RemoteDismantleBase extends UnicastRemoteObject implements IDismantleBase {
	// the methods here are not synchronized, because some of them are using eachother
	
	private static final long serialVersionUID = 1L;

	private Map<Integer, IPart> partCache;
	private Map<Integer, IPallet> palletCache;
	private IPartDAO partDao;
	private IPalletDAO palletDao;

	public RemoteDismantleBase(IPartDAO partDao, IPalletDAO palletDao) throws RemoteException {
		this.partDao = partDao;
		this.palletDao = palletDao;
		this.partCache = new HashMap<>();
		this.palletCache = new HashMap<>();
	}

	@Override
	public IPart registerPart(String carChassisNumber, String name, double weightKg) throws RemoteException {
		// create part in the database
		PartDTO partDto = partDao.create(carChassisNumber, name, weightKg);

		IPart newPart = new RemotePart(partDto);
		
		// get all pallets, in case there is an existing pallet that fits the part
		getAllPallets();
		
		// distribute part to a pallet
		addToPallet(newPart);
		
		// cache & return
		partCache.put(newPart.getId(), newPart);
		
		return newPart;
	}

	@Override
	public List<IPart> getParts(String carChassisNumber) throws RemoteException {
		// read all parts from the database
		Collection<PartDTO> allParts = partDao.readCarParts(carChassisNumber);

		// create output collection
		int size = allParts.size();
		List<IPart> matchingParts = new ArrayList<>(size); // avoid arraylist resizing

		for (PartDTO part : allParts) {
			// cache, if part is not already cached
			if (!partCache.containsKey(part.getId())) 
				partCache.put(part.getId(), new RemotePart(part));

			matchingParts.add(partCache.get(part.getId()));
		}

		return matchingParts;
	}

	@Override
	public List<IPart> getParts(int palletId) throws RemoteException {
		// read all parts from the database
		Collection<PartDTO> allParts = partDao.readPalletParts(palletId);

		// create output collection
		int size = allParts.size();
		List<IPart> matchingParts = new ArrayList<>(size); // avoid arraylist resizing

		for (PartDTO part : allParts) {
			// cache, if part is not already cached
			if (!partCache.containsKey(part.getId())) 
				partCache.put(part.getId(), new RemotePart(part));

			matchingParts.add(partCache.get(part.getId()));
		}

		return matchingParts;
	}

	@Override
	public List<IPart> getAllParts() throws RemoteException {
		// read all parts from the database
		Collection<PartDTO> allParts = partDao.readAll();
		
		// cache, if it is not already cached
		for (PartDTO part : allParts) 
			if (!partCache.containsKey(part.getId()))
				partCache.put(part.getId(), new RemotePart(part));

		return new ArrayList<>(partCache.values());
	}

	@Override
	public IPallet registerPallet(String palletType, List<IPart> parts) throws RemoteException {
		PalletDTO palletDto = palletDao.create(palletType, CollectionUtils.toPartDTOList(parts));
		
		IPallet newPallet = new RemotePallet(palletDto);
		
		palletCache.put(palletDto.getId(), newPallet);

		return newPallet;
	}

	@Override
	public IPallet getPallet(int palletId) throws RemoteException {
		if (!palletCache.containsKey(palletId)) {
			
			PalletDTO palletDto = palletDao.read(palletId);

			if (palletDto == null) return null;
			
			IPallet remotePallet = new RemotePallet(palletDto);
			
			palletCache.put(palletDto.getId(), remotePallet);
			
			return remotePallet;
		}

		return palletCache.get(palletId);
	}

	@Override
	public List<IPallet> getAllPallets() throws RemoteException {
		// read all pallets from the database
		Collection<PalletDTO> allPallets = palletDao.readAll();

		// cache all pallets that have not been already
		for (PalletDTO pallet : allPallets) 
			if (!palletCache.containsKey(pallet.getId()))
				palletCache.put(pallet.getId(), new RemotePallet(pallet));

		return new ArrayList<>(palletCache.values());
	}

	@Override
	public List<IPart> dismantleCar(ICar car) throws RemoteException {
		// create output collection
		int size = car.getParts().size();
		List<IPart> carParts = new ArrayList<>(size);
		
		// register all parts & add them to output collection
		List<IPart> parts = car.getParts();
		for (IPart part : parts) {
			IPart remotePart = registerPart(car.getChassisNumber(),
					part.getName(), part.getWeightKg());
			carParts.add(remotePart);
		}

		return carParts;
	}
	
	@Override
	public int removeFromPallet(IPart part) throws RemoteException {
		// get all available pallets
		getAllPallets();
		
		// find the pallet that contains the part
		Set<Entry<Integer, IPallet>> palletCacheEntries = palletCache.entrySet();
		for (Map.Entry<Integer, IPallet> entry : palletCacheEntries) {
			IPallet pallet = entry.getValue();
			
			if (pallet != null && pallet.containsPart(part)) {
				// remove part from pallet
				pallet.removePart(part);
				
				// update database
				palletDao.update(new PalletDTO(pallet));
				palletDao.removePartAssociations(pallet.getId(), new PartDTO(part));
				
				// update cache
				palletCache.put(pallet.getId(), pallet);
				partCache.remove(part.getId(), part);
				
				// return pallet id
				return pallet.getId();
			}
		}
		
		throw new NoSuchElementException("No pallet contains part " + part);
	}

	/**
	 * Adds a part to a pallet. If there is no existing pallet that fits the part,
	 * a new pallet is created and registered for the part
	 * 
	 *  @param part
	 *  		the part
	 *  @return true, if the part is successfully added to a pallet. Otherwise, the part is 
	 *  			too heavy (above 1000 kg) for the standard pallets
	 * @throws RemoteException
	 **/
	private boolean addToPallet(IPart part) throws RemoteException {
		// part is too heavy for standard pallets
		if (isOverweight(part)) return false;
		
		// attempt to add to existing pallet
		Set<Entry<Integer, IPallet>> palletCacheEntries = palletCache.entrySet();
		for (Map.Entry<Integer, IPallet> entry : palletCacheEntries) {
			IPallet pallet = entry.getValue();
			
			if (pallet != null && pallet.fits(part)) {
				// add part to pallet
				pallet.addPart(part);
				
				// update database
				palletDao.update(new PalletDTO(pallet));
		
				// update cache
				palletCache.put(pallet.getId(), pallet);
				
				return true;
			}
		}
	
		// if there is no existing pallet that fits, create a new one
		registerPallet(part.getType(), Arrays.asList(part));	
		return true;
	}
	
	/**
	 * Verifies that the part passed as a parameter fits the weight limits of the standard pallets
	 * 
	 * @param part 
	 * 			the part
	 * @return true, if the part fits the weight standards. Otherwise, false 
	 * @throws RemoteException
	 **/
	private boolean isOverweight(IPart part) throws RemoteException {
		boolean isOverweight = Double.compare(part.getWeightKg(),
				RemotePallet.MAX_PALLET_WEIGHT_CAPACITY) >= 0;
		return isOverweight;
	}

}
