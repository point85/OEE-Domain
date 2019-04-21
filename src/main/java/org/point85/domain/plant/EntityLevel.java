package org.point85.domain.plant;

import org.point85.domain.i18n.DomainLocalizer;

public enum EntityLevel {
	ENTERPRISE, SITE, AREA, PRODUCTION_LINE, WORK_CELL, EQUIPMENT;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case AREA:
			key = "area.level";
			break;
		case ENTERPRISE:
			key = "enterprise.level";
			break;
		case EQUIPMENT:
			key = "equipment.level";
			break;
		case PRODUCTION_LINE:
			key = "line.level";
			break;
		case SITE:
			key = "site.level";
			break;
		case WORK_CELL:
			key = "cell.level";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
