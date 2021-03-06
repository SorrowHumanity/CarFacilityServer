package remote.dao.shipment;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dto.part.PartDTO;
import dto.shipment.ShipmentDTO;
import persistence.DatabaseHelper;
import remote.dao.part.IPartDAO;
import util.CollectionUtils;

public class RemoteShipmentDAOServer extends UnicastRemoteObject implements IShipmentDAO {

	private static final long serialVersionUID = 1L;

	private IPartDAO partDao;
	private DatabaseHelper<ShipmentDTO> shipmentsEntity;

	public RemoteShipmentDAOServer(IPartDAO partDao) throws RemoteException {
		this.shipmentsEntity = new DatabaseHelper<>(DatabaseHelper.CAR_FACILITY_DB_URL, DatabaseHelper.POSTGRES_USERNAME,
				DatabaseHelper.POSTGRES_PASSWORD);
		this.partDao = partDao;
	}

	@Override
	public ShipmentDTO create(String receiverFirstName, String receiverLastName, List<PartDTO> parts,
			Map<Integer, Integer> partAssociations) throws RemoteException {
		int id = shipmentsEntity.executeUpdateReturningId(
				"INSERT INTO car_facility_schema.shipments (receiver_first_name, receiver_last_name)"
						+ " VALUES (?, ?) RETURNING id;",
				receiverFirstName, receiverLastName);

		// convert to DTO
		PartDTO[] partsArray = CollectionUtils.toPartDTOArray(parts);

		// associate all parts to the shipment, and include reference to the pallet
		// source
		associateParts(id, partsArray, partAssociations);

		return new ShipmentDTO(id, partsArray, receiverFirstName, receiverLastName);
	}

	@Override
	public ShipmentDTO read(int shipmentId) throws RemoteException {
		return shipmentsEntity.mapSingle((rs) -> createShipment(rs),
				"SELECT * FROM car_facility_schema.shipments WHERE shipments.id = ?;", shipmentId);
	}

	@Override
	public Collection<ShipmentDTO> readAll() throws RemoteException {
		return shipmentsEntity.map((rs) -> createShipment(rs), "SELECT * FROM car_facility_schema.shipments;");
	}

	@Override
	public boolean update(ShipmentDTO shipmentDto) throws RemoteException {
		// update database
		int rowsAffected = shipmentsEntity.executeUpdate(
				"UPDATE car_facility_schema.shipments SET receiver_first_name = ?, receiver_last_name = ? WHERE id = ?;",
				shipmentDto.getReceiverFirstName(), shipmentDto.getReceiverLastName(), shipmentDto.getId());

		return rowsAffected != 0;
	}

	@Override
	public boolean delete(ShipmentDTO shipmentDto) throws RemoteException {
		// remove all part associations to the shipment
		shipmentsEntity.executeUpdate("DELETE FROM car_facility_schema.requests WHERE shipments_id = ?;",
				shipmentDto.getId());

		// remove shipment
		int rowsAffected = shipmentsEntity.executeUpdate("DELETE FROM car_facility_schema.shipments WHERE id = ?;",
				shipmentDto.getId());

		return rowsAffected != 0;
	}

	/**
	 * Associates the parts to a shipment, including a reference to the pallet they
	 * came from
	 * 
	 * @param shipmentId
	 *            the id of the shipment
	 * @param allParts
	 *            the parts
	 * @param partAssociations
	 *            a map of the pallet id of the pallet in which a part was stored
	 * @throws RemoteException
	 **/
	private void associateParts(int shipmentId, PartDTO[] allParts, Map<Integer, Integer> partAssociations)
			throws RemoteException {
		for (PartDTO part : allParts) {
			int palletId = partAssociations.get(part.getId());
			shipmentsEntity.executeUpdate(
					"INSERT INTO car_facility_schema.requests"
							+ " (part_id, shipment_id, pallet_id) SELECT ?, ?, ? WHERE NOT EXISTS(SELECT *"
							+ " FROM car_facility_schema.requests WHERE requests.part_id = "
							+ "? AND requests.shipment_id = ? AND requests.pallet_id = ?);",
					part.getId(), shipmentId, palletId, part.getId(), shipmentId, palletId);
		}
	}

	/**
	 * Creates a shipment data transfer object from a database result set
	 * 
	 * @param rs
	 *            the result set
	 * @return the shipment data transfer object
	 * @throws SQLException
	 **/
	private ShipmentDTO createShipment(ResultSet rs) {
		try {
			int shipmentId = rs.getInt(ShipmentEntityConstants.ID_COLUMN);
			String receiverFirstName = rs.getString(ShipmentEntityConstants.FIRST_NAME_COLUMN);
			String receiverLastName = rs.getString(ShipmentEntityConstants.LAST_NAME_COLUMN);

			// read the pallet's parts
			Collection<PartDTO> parts = partDao.readShipmentParts(shipmentId);

			// convert to DTOs
			PartDTO[] partDTOs = CollectionUtils.toPartDTOArray(parts);

			return new ShipmentDTO(shipmentId, partDTOs, receiverFirstName, receiverLastName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
