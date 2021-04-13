package org.point85.domain.proficy;

/**
 * Proficy error codes
 *
 */
public enum ErrorCode {
	Success(0), Failed(-1), Timeout(-2), NotConnected(-3), CollectorNotFound(-4), NotSupported(-5), DuplicateData(-6),
	InvalidUsername(-7), AccessDenied(-8), WriteInFuture(-9), WriteArchiveOffline(-10), WriteArchiveReadonly(-11),
	WriteOutsideActiveRange(-12), WriteNoArchiveAvailable(-13), InvalidTagname(-14), LicensedTagCountExceeded(-15),
	LicensedConnectionCountExceeded(-16), InternalLicenseError(-17), NoValue(-18), DuplicateCollector(-19),
	NotLicensed(-20), CircularReference(-21), BackupInsufficientSpace(-22), InvalidServerVersion(-23),
	QueryResultSizeExceeded(-24), DeleteOutsideActiveRange(-25), AlarmArchiverUnavailable(-26), ArgumentException(-27),
	ArgumentNullException(-28), ArgumentOutOfRangeException(-29), InvalidEnumeratedSet(-30), InvalidDataStore(-31),
	NotPermitted(-32), InvalidCustomDataType(-33), ihSTATUS_EXISTING_USERDEF_REFERENCES(-34),
	ihSTATUS_INVALID_TAGNAME_DELETEDTAG(-35), ihSTATUS_INVALID_DHS_NODENAME(-36), ihSTATUS_DHS_SERVICE_IN_USE(-37),
	ihSTATUS_DHS_STORAGE_IN_USE(-38), ihSTATUS_DHS_TOO_MANY_NODES_IN_MIRROR(-39), ihSTATUS_ARCHIVE_IN_SYNC(-40),
	InvalidArchiveName(-41), InvalidSession(1), SessionExpired(2), UnknownError(3), NoValidClientBufferManager(4),
	NoValueInDataSet(5), TagNotExisting(6), ClientBufferManagerCommunicationError(7), TagTypeNotSupported(8),
	ValueTypeNotMatchTagDataType(9), InvalidParameter(10), TagSearchResultIsHuge(11), InvalidHistorianServer(12);

	// code
	private int code;

	private ErrorCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	/**
	 * Return ErrorCode from its integer value
	 * 
	 * @param value integer value
	 * @return ErrorCode
	 */
	public static ErrorCode fromInt(int value) {
		ErrorCode code = null;

		switch (value) {
		case 0:
			code = Success;
			break;
		case -1:
			code = Failed;
			break;
		case -2:
			code = Timeout;
			break;
		case -3:
			code = NotConnected;
			break;
		case -4:
			code = CollectorNotFound;
			break;
		case -5:
			code = NotSupported;
			break;
		case -6:
			code = DuplicateData;
			break;
		case -7:
			code = InvalidUsername;
			break;
		case -8:
			code = AccessDenied;
			break;
		case -9:
			code = WriteInFuture;
			break;
		case -10:
			code = WriteArchiveOffline;
			break;
		case -11:
			code = WriteArchiveReadonly;
			break;
		case -12:
			code = WriteOutsideActiveRange;
			break;
		case -13:
			code = WriteNoArchiveAvailable;
			break;
		case -14:
			code = InvalidTagname;
			break;
		case -15:
			code = LicensedTagCountExceeded;
			break;
		case -16:
			code = LicensedConnectionCountExceeded;
			break;
		case -17:
			code = InternalLicenseError;
			break;
		case -18:
			code = NoValue;
			break;
		case -19:
			code = DuplicateCollector;
			break;
		case -20:
			code = NotLicensed;
			break;
		case -21:
			code = CircularReference;
			break;
		case -22:
			code = BackupInsufficientSpace;
			break;
		case -23:
			code = InvalidServerVersion;
			break;
		case -24:
			code = QueryResultSizeExceeded;
			break;
		case -25:
			code = DeleteOutsideActiveRange;
			break;
		case -26:
			code = AlarmArchiverUnavailable;
			break;
		case -27:
			code = ArgumentException;
			break;
		case -28:
			code = ArgumentNullException;
			break;
		case -29:
			code = ArgumentOutOfRangeException;
			break;
		case -30:
			code = InvalidEnumeratedSet;
			break;
		case -31:
			code = InvalidDataStore;
			break;
		case -32:
			code = NotPermitted;
			break;
		case -33:
			code = InvalidCustomDataType;
			break;
		case -34:
			code = ihSTATUS_EXISTING_USERDEF_REFERENCES;
			break;
		case -35:
			code = ihSTATUS_INVALID_TAGNAME_DELETEDTAG;
			break;
		case -36:
			code = ihSTATUS_INVALID_DHS_NODENAME;
			break;
		case -37:
			code = ihSTATUS_DHS_SERVICE_IN_USE;
			break;
		case -38:
			code = ihSTATUS_DHS_STORAGE_IN_USE;
			break;
		case -39:
			code = ihSTATUS_DHS_TOO_MANY_NODES_IN_MIRROR;
			break;
		case -40:
			code = ihSTATUS_ARCHIVE_IN_SYNC;
			break;
		case -41:
			code = InvalidArchiveName;
			break;
		case 1:
			code = InvalidSession;
			break;
		case 2:
			code = SessionExpired;
			break;
		case 3:
			code = UnknownError;
			break;
		case 4:
			code = NoValidClientBufferManager;
			break;
		case 5:
			code = NoValueInDataSet;
			break;
		case 6:
			code = TagNotExisting;
			break;
		case 7:
			code = ClientBufferManagerCommunicationError;
			break;
		case 8:
			code = TagTypeNotSupported;
			break;
		case 9:
			code = ValueTypeNotMatchTagDataType;
			break;
		case 10:
			code = InvalidParameter;
			break;
		case 11:
			code = TagSearchResultIsHuge;
			break;
		case 12:
			code = InvalidHistorianServer;
			break;
		default:
			break;
		}
		return code;
	}

	@Override
	public String toString() {
		return name();
	}
}
