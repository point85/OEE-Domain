package org.point85.domain.proficy;

import java.time.Instant;

import com.google.gson.annotations.SerializedName;

/**
 * Serialized properties describing a tag
 *
 */
public class TagDetail {
	@SerializedName(value = "Tagid")
	private String tagId;

	@SerializedName(value = "Tagname")
	private String tagName;

	@SerializedName(value = "Description")
	private String description;

	@SerializedName(value = "DataType")
	private Integer dataType;

	@SerializedName(value = "CollectorName")
	private String collectorName;

	@SerializedName(value = "CollectorType")
	private Integer collectorType;

	@SerializedName(value = "DataStoreName")
	private String dataStoreName;

	@SerializedName(value = "EngineeringUnits")
	private String engineeringUnits;

	@SerializedName(value = "Comment")
	private String comment;

	@SerializedName(value = "SourceAddress")
	private String sourceAddress;

	@SerializedName(value = "CollectionInterval")
	private Integer collectionInterval;

	@SerializedName(value = "CollectorCompression")
	private Boolean collectorCompression;

	@SerializedName(value = "LastModifiedUser")
	private String lastModifiedUser;

	@SerializedName(value = "EnumeratedSetName")
	private String enumeratedSetName;

	@SerializedName(value = "UserDefinedTypeName")
	private String userDefinedTypeName;

	@SerializedName(value = "CalcType")
	private Integer calcType;

	@SerializedName(value = "IsStale")
	private Boolean isStale;

	@SerializedName(value = "HasAlias")
	private Boolean hasAlias;

	@SerializedName(value = "NumberOfElements")
	private Integer numberOfElements;

	@SerializedName(value = "CollectionDisabled")
	private Boolean collectionDisabled;

	@SerializedName(value = "LastModified")
	private Long lastModified;

	@SerializedName(value = "LastModifiedString")
	private String lastModifiedString;

	public String getTagId() {
		return tagId;
	}

	public String getTagName() {
		return tagName;
	}

	public String getDescription() {
		return description;
	}

	public Integer getDataType() {
		return dataType;
	}

	public String getCollectorName() {
		return collectorName;
	}

	public Integer getCollectorType() {
		return collectorType;
	}

	public String getDataStoreName() {
		return dataStoreName;
	}

	public String getEngineeringUnits() {
		return engineeringUnits;
	}

	public String getComment() {
		return comment;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public Integer getCollectionInterval() {
		return collectionInterval;
	}

	public Boolean isCollectorCompression() {
		return collectorCompression;
	}

	public String getLastModifiedUser() {
		return lastModifiedUser;
	}

	public String getEnumeratedSetName() {
		return enumeratedSetName;
	}

	public String getUserDefinedTypeName() {
		return userDefinedTypeName;
	}

	public Integer getCalcType() {
		return calcType;
	}

	public Boolean isHasAlias() {
		return hasAlias;
	}

	public Integer getNumberOfElements() {
		return numberOfElements;
	}

	public Boolean isCollectionDisabled() {
		return collectionDisabled;
	}

	public Long getLastModified() {
		return lastModified;
	}

	public TagDataType getEnumeratedType() {
		return TagDataType.fromInt(dataType);
	}

	public TagCalcType getTagCalculationType() {
		return TagCalcType.fromInt(calcType);
	}

	public String getLastModifiedString() {
		return lastModifiedString;
	}

	public Instant getLastModifiedTime() {
		return (lastModifiedString != null) ? Instant.parse(lastModifiedString) : null;
	}
}
