package org.point85.domain.persistence;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.point85.domain.plant.NamedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -247415339337530473L;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AuditInterceptor() {
		super();
	}

	private String buildLoggingText(Object entity, Object[] state) {
		String text = "TODO";

		if (entity instanceof NamedObject) {
			NamedObject po = (NamedObject) entity;
			text = "Name = " + po.getName() + ", Key = " + po.getKey() + ", #States = " + state.length
					+ ", Transient?: " + isTransient(entity);
		}
		return text;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		logger.info("onSave: " + buildLoggingText(entity, state));
		return super.onSave(entity, id, state, propertyNames, types);
	}

	@Override
	public String onPrepareStatement(String sql) {
		logger.info(sql);
		return super.onPrepareStatement(sql);
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		logger.info("onDelete: " + buildLoggingText(entity, state));
		super.onDelete(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		logger.info("onFlushDirty: " + buildLoggingText(entity, currentState) + ", Previous State = " + previousState);
		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		logger.info("onLoad: " + buildLoggingText(entity, state));
		return super.onLoad(entity, id, state, propertyNames, types);
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {
		logger.info("afterTransactionBegin: " + tx.getStatus());
		super.afterTransactionBegin(tx);
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		logger.info("beforeTransactionCompletion: " + tx.getStatus());
		super.beforeTransactionCompletion(tx);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		logger.info("afterTransactionCompletion: " + tx.getStatus());
		super.afterTransactionCompletion(tx);
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) {
		logger.info("onCollectionRecreate: ");
		super.onCollectionRecreate(collection, key);
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) {
		logger.info("onCollectionRemove: ");
		super.onCollectionRemove(collection, key);
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) {
		logger.info("onCollectionUpdate: ");
		super.onCollectionUpdate(collection, key);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void preFlush(Iterator entities) {
		logger.info("preFlush: ");
		super.preFlush(entities);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void postFlush(Iterator entities) {
		logger.info("postFlush: ");
		super.postFlush(entities);
	}
}
