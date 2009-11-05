package org.vosao.update;

import java.util.ArrayList;
import java.util.List;

import org.vosao.dao.Dao;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

public class UpdateManager {

	private List<UpdateTask> tasks;
	private DatastoreService datastore;
	private Dao dao;
	
	public UpdateManager(Dao aDao) {
		dao = aDao;
		datastore = DatastoreServiceFactory.getDatastoreService(); 
		tasks = new ArrayList<UpdateTask>();
		tasks.add(new UpdateTask003());
		tasks.add(new UpdateTask004(dao));
	}
	
	public void update() throws UpdateException {
		Entity config = getConfig();
		if (config.getProperty("version") == null) {
			addConfigVersion();
		}
		config = getConfig();
		for (UpdateTask task : tasks) {
			if (config.getProperty("version").equals(task.getFromVersion())) {
				task.update();
				config.setProperty("version", task.getToVersion());
				datastore.put(config);
			}
		}
	}
	
	private Entity getConfig() {
		Query query = new Query("ConfigEntity");
		return datastore.prepare(query).asIterator().next();
	}
	
	private void addConfigVersion() {
		Entity config = getConfig();
		config.setProperty("version", "0.0.2");
		config.setProperty("enableRecaptcha", false);
		datastore.put(config);
	}
	
}
