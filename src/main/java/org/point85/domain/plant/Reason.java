package org.point85.domain.plant;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.point85.domain.dto.ReasonDto;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.TimeLossConverter;

/**
 * The Reason class is the reason for an OEE time loss.
 * 
 * @author Kent Randall
 *
 */

@Entity
@Table(name = "REASON")
@AttributeOverride(name = "primaryKey", column = @Column(name = "REASON_KEY"))

public class Reason extends NamedObject {
	// the one and only root reason in the hierarchy
	public static final String ROOT_REASON_NAME = "All Reasons";

	// parent reason
	@ManyToOne
	@JoinColumn(name = "PARENT_KEY")
	private Reason parent;

	// child reasons
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private final Set<Reason> children = new HashSet<>();

	// loss category
	@Column(name = "LOSS")
	@Convert(converter = TimeLossConverter.class)
	private TimeLoss timeLoss;

	public Reason() {
		super();
	}

	public Reason(String name, String description) {
		super(name, description);
	}

	public Reason(ReasonDto dto) {
		setAttributes(dto);
	}
	
	public void setAttributes(ReasonDto dto) {
		super.setAttributes(dto);
		
		if (dto.getLossCategory() != null) {
			this.timeLoss =  TimeLoss.valueOf(dto.getLossCategory());
		}
	}

	public Reason getParent() {
		return this.parent;
	}

	public void setParent(Reason parent) {
		this.parent = parent;
	}

	public Set<Reason> getChildren() {
		return this.children;
	}

	public void addChild(Reason child) {
		if (!children.contains(child)) {
			children.add(child);
			child.setParent(this);
		}
	}

	public void removeChild(Reason child) {
		if (children.contains(child)) {
			children.remove(child);
			child.setParent(null);
		}
	}

	public TimeLoss getLossCategory() {
		return this.timeLoss;
	}

	public void setLossCategory(TimeLoss loss) {
		this.timeLoss = loss;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Reason) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getLossCategory());
	}

	@Override
	public String toString() {
		return super.toString() + ", Loss: " + getLossCategory();
	}

}
