package remote.base.registration;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dto.car.CarDTO;
import dto.part.PartDTO;
import remote.model.part.IPart;
import remote.dao.car.ICarDAO;
import remote.model.car.ICar;
import remote.model.car.RemoteCar;
import util.CollectionUtils;

public class RemoteRegistrationBase extends UnicastRemoteObject implements IRegistrationBase {

	private static final long serialVersionUID = 1L;

	private Map<String, ICar> carCache = new HashMap<>();
	private ICarDAO carDao;

	public RemoteRegistrationBase(ICarDAO carDao) throws RemoteException {
		this.carDao = carDao;
	}

	@Override
	public synchronized ICar registerCar(String chassisNumber, String model, List<IPart> parts) throws RemoteException {
		List<PartDTO> partDtos = CollectionUtils.toDTOList(parts);
		
		// create database entry
		CarDTO carDto = carDao.create(chassisNumber, model, partDtos);

		// cache and return
		carCache.put(chassisNumber, new RemoteCar(carDto));
		
		return carCache.get(chassisNumber);
	}

	@Override
	public synchronized ICar getCar(String chassisNumber) throws RemoteException {
		// cache if car is not already cached
		if (!carCache.containsKey(chassisNumber)) {
			// read car from the database
			CarDTO carDto = carDao.read(chassisNumber);

			// avoid caching null values
			if (carDto == null) 
				throw new IllegalArgumentException
								("Car with chassis number " + chassisNumber + " does not exist!");
			
			// cache car
			carCache.put(chassisNumber, new RemoteCar(carDto));
		}

		return carCache.get(chassisNumber);
	}

	@Override
	public synchronized List<ICar> getAllCars() throws RemoteException {
		// read all cars from the database
		Collection<CarDTO> allCars = carDao.readAll();

		// create output collection
		int size = allCars.size();
		ArrayList<ICar> carList = new ArrayList<>(size); // avoid arraylist expansion

		for (CarDTO car : allCars) {
			// cache if car is not already cached
			if (!carCache.containsKey(car.getChassisNumber()))
				carCache.put(car.getChassisNumber(), new RemoteCar(car));

			// add to output collection
			carList.add(carCache.get(car.getChassisNumber()));
		}

		return carList;
	}

}
