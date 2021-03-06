package util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dto.car.CarDTO;
import dto.pallet.PalletDTO;
import dto.part.PartDTO;
import dto.shipment.ShipmentDTO;
import remote.domain.car.ICar;
import remote.domain.pallet.IPallet;
import remote.domain.part.IPart;
import remote.domain.part.RemotePart;
import remote.domain.shipment.IShipment;

public final class CollectionUtils {

	private CollectionUtils() {}

	/**
	 * Converts an array of DTO parts to a list of remote parts
	 **/
	public static List<IPart> toRemotePartList(PartDTO[] allParts) throws RemoteException {
		int size = allParts.length;
		List<IPart> remoteParts = new ArrayList<>(size);

		for (PartDTO part : allParts)
			remoteParts.add(new RemotePart(part));

		return remoteParts;
	}

	/**
	 * Converts a list of remote cars to an array of DTO cars
	 **/
	public static CarDTO[] toCarDTOArray(List<ICar> allCars) throws RemoteException {
		int size = allCars.size();
		CarDTO[] carDtos = new CarDTO[size];

		for (int i = 0; i < size; ++i)
			carDtos[i] = new CarDTO(allCars.get(i));

		return carDtos;
	}

	/**
	 * Converts a list of remote pallets to an array of DTO pallets
	 **/
	public static PalletDTO[] toPalletDTOArray(List<IPallet> allPallets) throws RemoteException {
		int size = allPallets.size();
		PalletDTO[] palletDtos = new PalletDTO[size];
		
		for (int i = 0; i < size; ++i)
			palletDtos[i] = new PalletDTO(allPallets.get(i));

		return palletDtos;
	}

	/**
	 * Converts a collection of DTO parts to an array of DTO parts
	 **/
	public static PartDTO[] toPartDTOArray(Collection<PartDTO> allParts) {
		PartDTO[] partArray = allParts.toArray(new PartDTO[0]);
		return partArray;
	}

	/**
	 * Converts a list of remote parts to an array of DTO parts
	 **/
	public static PartDTO[] toPartDTOArray(List<IPart> allParts) throws RemoteException {
		int size = allParts.size();
		PartDTO[] partDtos = new PartDTO[size];

		for (int i = 0; i < size; ++i)
			partDtos[i] = new PartDTO(allParts.get(i));

		return partDtos;
	}

	/**
	 * Converts a list of remote shipments to an an array of DTO shipments
	 **/
	public static ShipmentDTO[] toShipmentDTOArray(List<IShipment> allShipments) throws RemoteException {
		int size = allShipments.size();
		ShipmentDTO[] shipmentDtos = new ShipmentDTO[size];

		for (int i = 0; i < size; ++i)
			shipmentDtos[i] = new ShipmentDTO(allShipments.get(i));

		return shipmentDtos;
	}
	
	/**
	 * Converts a list of remote parts to a list of DTO parts 
	 **/
	public static List<PartDTO> toPartDTOList(List<IPart> parts) throws RemoteException {
		int size = parts.size();
		List<PartDTO> partDtos = new ArrayList<>(size);
		
		for (IPart part : parts) 
			partDtos.add(new PartDTO(part));
		
		return partDtos;
	}

}
