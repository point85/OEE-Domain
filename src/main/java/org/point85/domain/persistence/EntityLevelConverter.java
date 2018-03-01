package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;

@Converter
public class EntityLevelConverter implements AttributeConverter<EntityLevel, String> {

	@Override
	public String convertToDatabaseColumn(EntityLevel attribute) {
		String value = null;

		switch (attribute) {
		case AREA:
			value = Area.AREA_VALUE;
			break;

		case ENTERPRISE:
			value = Enterprise.ENTER_VALUE;
			break;

		case EQUIPMENT:
			value = Equipment.EQUIP_VALUE;
			break;

		case PRODUCTION_LINE:
			value = ProductionLine.LINE_VALUE;
			break;

		case SITE:
			value = Site.SITE_VALUE;
			break;

		case WORK_CELL:
			value = WorkCell.CELL_VALUE;
			break;

		default:
			break;
		}
		return value;
	}

	@Override
	public EntityLevel convertToEntityAttribute(String value) {
		EntityLevel level = null;

		switch (value) {
		case Enterprise.ENTER_VALUE:
			level = EntityLevel.ENTERPRISE;
			break;

		case Site.SITE_VALUE:
			level = EntityLevel.SITE;
			break;

		case Area.AREA_VALUE:
			level = EntityLevel.AREA;
			break;

		case ProductionLine.LINE_VALUE:
			level = EntityLevel.PRODUCTION_LINE;
			break;

		case WorkCell.CELL_VALUE:
			level = EntityLevel.WORK_CELL;
			break;

		case Equipment.EQUIP_VALUE:
			level = EntityLevel.EQUIPMENT;
			break;

		default:
			break;
		}
		return level;
	}
}