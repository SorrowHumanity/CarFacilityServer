package remote.model.part;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import dto.part.PartDTO;

public class RemotePart extends UnicastRemoteObject implements IPart {

	private static final long serialVersionUID = 1L;

	private int id;
	private String carChassisNumber, name;
	private double weightKg;

	public RemotePart(int id, String carChassisNumber, String name, double weightKg) throws RemoteException {
		this.id = id;
		this.carChassisNumber = carChassisNumber;
		this.name = name;
		this.weightKg = weightKg;
	}

	public RemotePart(PartDTO partDTO) throws RemoteException {
		this(partDTO.getId(), partDTO.getCarChassisNumber(), partDTO.getName(), partDTO.getWeightKg());
	}

	@Override
	public int getId() throws RemoteException {
		return id;
	}

	@Override
	public String getName() throws RemoteException {
		return name;
	}

	@Override
	public double getWeightKg() throws RemoteException {
		return weightKg;
	}

	@Override
	public String getCarChassisNumber() throws RemoteException {
		return carChassisNumber;
	}

	@Override
	public String toString() {
		return "RemotePart [id=" + id + ", name=" + name + ", weight=" + weightKg + "]";
	}

}