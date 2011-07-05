package org.sagebionetworks.repo.model.jdo;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.persistence.JDOBlobAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDONodeType;
import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.RevisionId;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.orm.jdo.JdoCallback;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a basic JDO implementation of the NodeDAO.
 * 
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeDAOImpl implements NodeDAO, InitializingBean {
	
	static private Log log = LogFactory.getLog(NodeDAOImpl.class);
	
	@Autowired
	private JdoTemplate jdoTemplate;
	
	private static boolean isHypersonicDB = true;
	
	private static String BIND_ID_KEY = "bindId";
	private static String SQL_ETAG_WITHOUT_LOCK = "SELECT "+SqlConstants.COL_NODE_ETAG+" FROM "+SqlConstants.TABLE_NODE+" WHERE ID = :"+BIND_ID_KEY;
	private static String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	
	private static String SQL_GET_ALL_VERSION_NUMBERS = "SELECT "+SqlConstants.COL_REVISION_NUMBER+" FROM "+SqlConstants.TABLE_REVISION+" WHERE "+SqlConstants.COL_REVISION_OWNER_NODE +" = :"+BIND_ID_KEY+" ORDER BY "+SqlConstants.COL_REVISION_NUMBER+" DESC";

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNew(Node dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("Node cannot be null");
		JDORevision rev = new JDORevision();
		// Set the default label
		rev.setLabel("0.0.0");
		rev.setRevisionNumber(new Long(1));
		JDONode node = new JDONode();
		node.setCurrentRevNumber(rev.getRevisionNumber());
		JDONodeUtils.updateFromDto(dto, node, rev);
		// Look up this type
		if(dto.getNodeType() == null) throw new IllegalArgumentException("Node type cannot be null");
		JDONodeType type = getNodeType(ObjectType.valueOf(dto.getNodeType()));
		node.setNodeType(type);
		// Make sure the nodes does not come in with an id
		node.setId(null);
		// Start it with an eTag of zero
		node.seteTag(new Long(0));
		// Make sure it has annotations
		node.setStringAnnotations(new HashSet<JDOStringAnnotation>());
		node.setDateAnnotations(new HashSet<JDODateAnnotation>());
		node.setLongAnnotations(new HashSet<JDOLongAnnotation>());
		node.setDoubleAnnotations(new HashSet<JDODoubleAnnotation>());
		node.setBlobAnnotations(new HashSet<JDOBlobAnnotation>());
		
		// Set the parent and benefactor
		if(dto.getParentId() != null){
			// Get the parent
			JDONode parent = getNodeById(Long.parseLong(dto.getParentId()));
			node.setParent(parent);
			// By default a node should inherit from the same 
			// benefactor as its parent
			node.setPermissionsBenefactor(parent.getPermissionsBenefactor());
		}
		// Create the first revision for this node
		// We can now create the node.
		node = jdoTemplate.makePersistent(node);
		if(node.getPermissionsBenefactor() == null){
			// For nodes that have no parent, they are
			// their own benefactor. We have to wait until
			// after the makePersistent() call to set a node to point 
			// to itself.
			node.setPermissionsBenefactor(node);
		}
		// Now create the revision
		rev.setOwner(node);
		jdoTemplate.makePersistent(rev);
		return node.getId().toString();
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException {
		if(newVersion == null) throw new IllegalArgumentException("New version node cannot be null");
		if(newVersion.getId() == null) throw new IllegalArgumentException("New version node ID cannot be null");
		if(newVersion.getVersionLabel() == null) throw new IllegalArgumentException("Cannot create a new version with a null version label");
		// Get the Node
		JDONode jdo = getNodeById(KeyFactory.stringToKey(newVersion.getId()));
		// Look up the current version
		JDORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		// Make a copy of the current revision with an incremented the version number
		JDORevision newRev = JDORevisionUtils.makeCopyForNewVersion(rev);
		// Now update the new revision and node
		JDONodeUtils.updateFromDto(newVersion, jdo, newRev);
		// Now save the new revision
		try{
			jdoTemplate.makePersistent(newRev);
		}catch (DuplicateKeyException e){
			throw new IllegalArgumentException("Must provide a unique version label. Label: "+newRev.getLabel()+" has alredy be used for this entity");
		}

		// The new revision becomes the current version
		jdo.setCurrentRevNumber(newRev.getRevisionNumber());
		return newRev.getRevisionNumber();
	}

	@Transactional(readOnly = true)
	@Override
	public Node getNode(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		JDORevision rev  = getNodeRevisionById(jdo.getId(), jdo.getCurrentRevNumber());
		return JDONodeUtils.copyFromJDO(jdo, rev);
	}
	
	@Override
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(versionNumber == null) throw new IllegalArgumentException("Version number cannot be null");
		Long nodeID = KeyFactory.stringToKey(id);
		JDONode jdo =  getNodeById(nodeID);
		JDORevision rev = getNodeRevisionById(nodeID, versionNumber);
		return JDONodeUtils.copyFromJDO(jdo, rev);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws NotFoundException {
		JDONode toDelete = getNodeById(Long.parseLong(id));
		if(toDelete != null){
			jdoTemplate.deletePersistent(toDelete);
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(String nodeId, Long versionNumber) throws NotFoundException, DatastoreException {
		// Get the version in question
		Long id = KeyFactory.stringToKey(nodeId);
		JDORevision rev = getNodeRevisionById(id, versionNumber);
		if(rev != null){
			jdoTemplate.deletePersistent(rev);
			// Make sure the node is still pointing the the current version
			List<Long> versions = getVersionNumbers(nodeId);
			if(versions == null || versions.size() < 1){
				throw new IllegalArgumentException("Cannot delete the last version of a node");
			}
			JDONode node = getNodeById(id);
			// Make sure the node is still pointing the the current version
			node.setCurrentRevNumber(versions.get(0));
		}
	}
	
	/**
	 * Try to get a node, and throw a NotFoundException if it fails.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	private JDONode getNodeById(Long id) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		try{
			return jdoTemplate.getObjectById(JDONode.class, id);
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}
	
	private JDORevision getCurrentRevision(JDONode node) throws NotFoundException{
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		return getNodeRevisionById(node.getId(),  node.getCurrentRevNumber());
	}
	
	private JDORevision getNodeRevisionById(Long id, Long revNumber) throws NotFoundException{
		if(id == null) throw new IllegalArgumentException("Node ID cannot be null");
		try{
			return (JDORevision) jdoTemplate.getObjectById(new RevisionId(id, revNumber));
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}
	
	private JDONodeType getNodeType(ObjectType type) throws NotFoundException{
		if(type == null) throw new IllegalArgumentException("Node Type cannot be null");
		try{
			return jdoTemplate.getObjectById(JDONodeType.class, type.getId());
		}catch (JDOObjectNotFoundException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}catch (JdoObjectRetrievalFailureException e){
			// Convert to a not found exception
			throw new NotFoundException(e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(String id) throws NotFoundException, DatastoreException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode jdo =  getNodeById(Long.parseLong(id));
		JDORevision rev = getCurrentRevision(jdo);
		return getAnnotations(jdo, rev);
	}

	/**
	 * Helper method to create the annotations from a given a given revision.
	 * @param jdo
	 * @param rev
	 * @return
	 * @throws DatastoreException
	 */
	private Annotations getAnnotations(JDONode jdo, JDORevision rev) throws DatastoreException {
		// Get the annotations and make a copy
		Annotations annos;
		try {
			annos = JDOAnnotationsUtils.createFromJDO(rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		annos.setEtag(jdo.geteTag().toString());
		annos.setId(KeyFactory.keyToString(jdo.getId()));
		annos.setCreationDate(new Date(jdo.getCreatedOn()));
		return annos;
	}
	
	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException {
		Long nodeId = KeyFactory.stringToKey(id);
		JDONode jdo =  getNodeById(nodeId);
		// Get a particular version.
		JDORevision rev = getNodeRevisionById(nodeId, versionNumber);
		return getAnnotations(jdo, rev);
	}

	@Transactional(readOnly = true)
	@Override
	public Set<Node> getChildren(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode parent = getNodeById(Long.parseLong(id));
		if(parent != null){
			Set<JDONode> childrenSet = parent.getChildren();
			return extractNodeSet(childrenSet);
		}
		return null;
	}
	
	@Override
	public Set<String> getChildrenIds(String id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDONode parent = getNodeById(Long.parseLong(id));
		if(parent != null){
			Set<JDONode> childrenSet = parent.getChildren();
			return extractNodeIdSet(childrenSet);
		}
		return null;
	}

	private Set<Node> extractNodeSet(Set<JDONode> childrenSet) throws NotFoundException {
		if(childrenSet == null)return null;
		HashSet<Node> children = new HashSet<Node>();
		Iterator<JDONode> it = childrenSet.iterator();
		while(it.hasNext()){
			JDONode node = it.next();
			JDORevision rev = getCurrentRevision(node);
			children.add(JDONodeUtils.copyFromJDO(node, rev));
		}
		return children;
	}
	
	private Set<String> extractNodeIdSet(Set<JDONode> childrenSet) {
		if(childrenSet == null)return null;
		HashSet<String> children = new HashSet<String>();
		Iterator<JDONode> it = childrenSet.iterator();
		while(it.hasNext()){
			JDONode child = it.next();
			children.add(child.getId().toString());
		}
		return children;
	}
	
	@Transactional(readOnly = true)
	@Override
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException {
		JDONode node = getNodeById(KeyFactory.stringToKey(id));
		return KeyFactory.keyToString(node.geteTag());
	}

	/**
	 * Note: You cannot call this method outside of a transaction.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public String lockNodeAndIncrementEtag(String id, String eTag)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		// Create a Select for update query
		final Long longId = KeyFactory.stringToKey(id);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("bindId", longId);
		String sql = null;
		if(isSelectForUpdateSupported()){
			sql = SQL_ETAG_FOR_UPDATE;
		}else{
			sql = SQL_ETAG_WITHOUT_LOCK;
		}
		List<Long> result = executeQuery(sql, map);
		if(result == null ||result.size() < 1 ) throw new JDOObjectNotFoundException("Cannot find a node with id: "+longId);
		if(result.size() > 1 ) throw new IllegalStateException("More than one node found with id: "+longId);
		// Check the eTags
		long passedTag = KeyFactory.stringToKey(eTag);
		long currentTag =  result.get(0);
		if(passedTag != currentTag){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Increment the eTag
		currentTag++;
		JDONode node = getNodeById(longId);
		node.seteTag(currentTag);
		// Return the new tag
		return KeyFactory.keyToString(currentTag);
	}
	
	public List executeQuery(final String sql, final Map<String, Object> parameters){
		return this.jdoTemplate.execute(new JdoCallback<List>() {
			@SuppressWarnings("unchecked")
			@Override
			public List doInJdo(PersistenceManager pm) throws JDOException {
				if(log.isDebugEnabled()){
					log.debug("Runing SQL query:\n"+sql);
					if(parameters != null){
						log.debug("Using Parameters:\n"+parameters.toString());
					}
				}
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				return (List) query.executeWithMap(parameters);
			}
		});
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateNode(Node updatedNode) throws NotFoundException {
		if(updatedNode == null) throw new IllegalArgumentException("Node to update cannot be null");
		if(updatedNode.getId() == null) throw new IllegalArgumentException("Node to update cannot have a null ID");
		JDONode jdoToUpdate = getNodeById(Long.parseLong(updatedNode.getId()));
		JDORevision revToUpdate = getCurrentRevision(jdoToUpdate);
		// Update is as simple as copying the values from the passed node.
		JDONodeUtils.updateFromDto(updatedNode, jdoToUpdate, revToUpdate);		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String nodeId, Annotations updatedAnnotations) throws NotFoundException, DatastoreException {
		if(updatedAnnotations == null) throw new IllegalArgumentException("Updateded Annotations cannot be null");
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Node ID cannot be null");
		if(updatedAnnotations.getEtag() == null) throw new IllegalArgumentException("Annotations must have a valid eTag");
		JDONode jdo =  getNodeById(Long.parseLong(nodeId));
		JDORevision rev = getCurrentRevision(jdo);
		// now update the annotations from the passed values.
		try {
			JDOAnnotationsUtils.updateFromJdoFromDto(updatedAnnotations, jdo, rev);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	@Transactional(readOnly = true)
	@Override
	public List<Long> getVersionNumbers(String id) throws NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(BIND_ID_KEY, id);
		return executeQuery(SQL_GET_ALL_VERSION_NUMBERS, parameters);
	}
	
	/**
	 * Does the current database support 'select for update'
	 * @return
	 */
	private boolean isSelectForUpdateSupported(){
		return !isHypersonicDB;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void afterPropertiesSet() throws Exception {
		// Make sure all of the known types are there
		ObjectType[] types = ObjectType.values();
		for(ObjectType type: types){
			try{
				// Try to get the type.
				// If the type does not already exist then an exception will be thrown
				@SuppressWarnings("unused")
				JDONodeType jdo = getNodeType(type);
			}catch(NotFoundException e){
				// The type does not exist so create it.
				JDONodeType jdo = new JDONodeType();
				jdo.setId(type.getId());
				jdo.setName(type.name());
				this.jdoTemplate.makePersistent(jdo);
			}
		}
		String driver = this.jdoTemplate.getPersistenceManagerFactory().getConnectionDriverName();
		log.info("Driver: "+driver);
		isHypersonicDB = driver.startsWith("org.hsqldb");
	}

	
}
