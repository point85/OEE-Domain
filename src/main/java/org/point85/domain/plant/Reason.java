package org.point85.domain.plant;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.point85.domain.performance.TimeLoss;
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

@NamedQueries({
		@NamedQuery(name = Reason.REASON_BY_NAME, query = "SELECT reason FROM Reason reason WHERE reason.name = :name"),
		@NamedQuery(name = Reason.REASON_NAMES, query = "SELECT reason.name FROM Reason reason"),
		@NamedQuery(name = Reason.REASON_KEY_BY_NAME, query = "SELECT reason.primaryKey, reason.version FROM Reason reason WHERE reason.name = :name"),
		@NamedQuery(name = Reason.REASON_ROOTS, query = "SELECT reason FROM Reason reason WHERE reason.parent IS NULL"),
		@NamedQuery(name = Reason.REASON_ALL, query = "SELECT reason FROM Reason reason"), })
public class Reason extends NamedObject {
	// the one and only root reason in the hierarchy
	public static final String ROOT_REASON_NAME = "All Reasons";

	// named queries
	public static final String REASON_BY_NAME = "REASON.ByName";
	public static final String REASON_NAMES = "REASON.Names";
	public static final String REASON_KEY_BY_NAME = "REASON.KeyByName";
	public static final String REASON_ROOTS = "REASON.Roots";
	public static final String REASON_ALL = "REASON.All";

	// parent reason
	@ManyToOne
	@JoinColumn(name = "PARENT_KEY")
	private Reason parent;

	// child reasons
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<Reason> children = new HashSet<>();

	// transition loss category
	@Column(name = "LOSS")
	@Convert(converter = TimeLossConverter.class)
	private TimeLoss toCategory;

	public Reason() {
		super();
	}

	public Reason(String name, String description) {
		super(name, description);
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
		return this.toCategory;
	}

	public void setLossCategory(TimeLoss loss) {
		this.toCategory = loss;
	}
/*
	@Override
	public String getFetchQueryName() {
		return REASON_BY_NAME;
	}
*/
	@Override
	public String toString() {
		return super.toString() + ", Loss: " + getLossCategory();
	}

}
