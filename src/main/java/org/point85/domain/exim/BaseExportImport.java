package org.point85.domain.exim;

import com.google.gson.Gson;

abstract class BaseExportImport {
	// serializer
	protected Gson gson = new Gson();
	
	// content to export or import
	protected ExportImportContent content = new ExportImportContent();
	
	public ExportImportContent getContent() {
		return content;
	}
}
