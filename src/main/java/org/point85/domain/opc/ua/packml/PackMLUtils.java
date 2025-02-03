package org.point85.domain.opc.ua.packml;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeEventType;

/**
 * Utilities for PackML
 */
public final class PackMLUtils {
	/**
	 * Compute the difference in the accumulated good production count from this
	 * call and the last one. This method is intended to be called from the event
	 * script for this equipment.
	 * 
	 * @param value    Value of {@link PackMLCountDataType} from the OPC UA node
	 *                 subscription
	 * @param resolver {@link EventResolver} script resolver
	 * @return Difference in accumulated count
	 * @throws Exception Exception
	 */
	public static Integer getDeltaAccumCount(PackMLCountDataType value, EventResolver resolver) throws Exception {
		Integer deltaCount = 0;

		if (value == null || resolver == null) {
			return deltaCount;
		}

		// last accumulated count
		Integer lastAccumulated = (Integer) resolver.getLastValue();

		if (lastAccumulated == null) {
			// fetch the last good production OEE event for this equipment
			OeeEvent lastEvent = PersistenceService.instance().fetchLastEvent(resolver.getEquipment(),
					OeeEventType.PROD_GOOD);

			// get the input (total good count) value
			if (lastEvent != null) {
				lastAccumulated = Integer.valueOf((String) lastEvent.getInputValue());
			}
		}

		// get accumulated count from PackMLProdCount
		if (lastAccumulated != null) {
			deltaCount = lastAccumulated - value.getAccCount();
		}

		// save it for next time
		resolver.setLastValue(value.getAccCount());

		return deltaCount;
	}

	public static String localizedTextToString(LocalizedText lt) {
		StringBuilder sb = new StringBuilder();
		sb.append(lt.getText()).append(", ").append(lt.getLocale());
		return sb.toString();
	}

	public static String euInformationToString(EUInformation eu) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\t").append("Namespace URI\t").append(eu.getNamespaceUri());
		sb.append("\n\t").append("Unit Id\t\t").append(eu.getUnitId());
		sb.append("\n\t").append("Display Name\t").append(localizedTextToString(eu.getDisplayName()));
		sb.append("\n\t").append("Description\t").append(localizedTextToString(eu.getDescription()));
		return sb.toString();
	}

	public static String parametersToString(PackMLDescriptorDataType[] parameters) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			sb.append("\n\t").append('[').append(i).append("] ").append(parameters[i].toString());
		}
		return sb.toString();
	}
}
