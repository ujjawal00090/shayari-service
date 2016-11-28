package com.sjosan.utility.service.business;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sjosan.utility.service.entity.Category;
import com.sjosan.utility.service.entity.Content;
import com.sjosan.utility.service.entity.Likes;
import com.sjosan.utility.service.entity.ResponsePayload;

public class UtilityServiceBusiness {


	@Autowired
	private SessionFactory sessionFactory;

	Logger logger = LoggerFactory.getLogger(UtilityServiceBusiness.class);

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}


	@SuppressWarnings("unchecked")
	public ResponsePayload getCategory() {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();

		Query query = session.createQuery("from Category where display != 0");

		List<Category> list = query.list();

		for (Category c : list) {
			Query query2 = session.createQuery("from Content where categoryID=? and approved != 0");
			query2.setParameter(0, (int) c.getId());
			List<Content> contents = query2.list();
			c.setContentCount(contents.size());
			//c.setContents(contents);
		}

		payload.setCategories(list);

		return payload;
	}

	

	@SuppressWarnings("unchecked")
	public ResponsePayload getContent(int categoryID) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();

		Query query = session.createQuery("from Content where categoryID=? and approved = 1");
		query.setParameter(0, categoryID);

		List<Content> list = query.list();
		
		Query query2 = session.createQuery("from Likes where categoryID=?");
		query2.setParameter(0, categoryID);
		
		List<Likes> list2 = query2.list();
		
		for(Content c:list){
			
			List<Likes> newLikes=new ArrayList<Likes>();
			
			for(Likes l:list2){
				if(l.getContentID()==c.getId()){
					newLikes.add(l);
				}
			}
			c.setLikes(newLikes);
		}

		payload.setContents(list);

		return payload;
	}
	
	@SuppressWarnings("unchecked")
	public ResponsePayload getMyContent(String accountID) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();

		Query query = session.createQuery("from Content as c where c.submittedBy like '%"+accountID+"%'");

		List<Content> list = query.list();
		
		
		
		for(Content c:list){
			
			Query query2 = session.createQuery("from Likes where contentID=?");
			query2.setParameter(0, (int)c.getId());
			
			List<Likes> list2 = query2.list();
			c.setLikes(list2);
		}

		payload.setContents(list);

		return payload;
	}

	public ResponsePayload saveData() {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();
		Transaction tx = null;

		ClassLoader classLoader = getClass().getClassLoader();
		File files = new File(classLoader.getResource("assets").getFile());

		File folder = new File(files.getAbsolutePath());
		File[] listOfFiles = folder.listFiles();

		tx = session.beginTransaction();

		try {

			for (File file : listOfFiles) {
				if (file.getName().contains("txt")) {
					String catName = file.getName().replace(".txt", "");
					Category cat = new Category();
					cat.setName(catName);
					cat.setDisplay(1);
					session.save(cat);

					try {
						String fileContent;

						fileContent = FileUtils.readFileToString(file, "UTF-8");

						fileContent = fileContent.replace("\u2060\u2060\u2060\u2060\u2060", "");
						fileContent = fileContent.replace("\"", "");

						String[] singleContents = fileContent.split(";;;");

						for (String singleContent : singleContents) {
							Content content = new Content();

							content.setContentText(StringEscapeUtils.escapeSql(singleContent));
							content.setCategoryID((int) cat.getId());
							content.setSubmittedBy("ADMIN");
							content.setApproved(1);

							session.save(content);

						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			tx.commit();

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();

			logger.error("saveData", e);
			payload.setResponseStatus(false);
			payload.setResponseMessage("OOPS! Something went wrong."+e.getMessage());
		} finally {
			session.close();
		}

		return payload;

	}

	public ResponsePayload addContent(Content content) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();
		Transaction tx = null;

		tx = session.beginTransaction();

		try {
			content.setApproved(0);

			session.save(content);

			tx.commit();

			payload.setResponseMessage("Content Added. Pending Admin approval.");

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();

			logger.error("addContent", e);
			payload.setResponseStatus(false);
			payload.setResponseMessage("OOPS! Something went wrong.");
		} finally {
			session.close();
		}

		return payload;

	}

	public ResponsePayload approveContent(Content content) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();
		Transaction tx = null;

		tx = session.beginTransaction();

		try {

			if (content.getApproved() == 1) {

				Query query2 = session.createQuery("UPDATE Content set approved = " + content.getApproved()
						+ ", categoryID = " + content.getCategoryID() + " where id = " + content.getId());
				query2.executeUpdate();
				
				payload.setResponseMessage("Content Approved.");
				
			} else {
				session.saveOrUpdate(content);
				
				payload.setResponseMessage("Content Deleted.");
			}

			tx.commit();

			

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();

			logger.error("addContent", e);
			payload.setResponseStatus(false);
			payload.setResponseMessage("OOPS! Something went wrong.");
		} finally {
			session.close();
		}

		return payload;

	}

	@SuppressWarnings("unchecked")
	public ResponsePayload getContentToApprove() {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();

		Query query = session.createQuery("from Content where approved = 0");

		List<Content> list = query.list();

		payload.setContents(list);

		return payload;
	}
	
	public ResponsePayload like(Likes like) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();
		Transaction tx = null;

		tx = session.beginTransaction();

		try {

			session.save(like);

			tx.commit();
			
			payload=getContent(like.getCategoryID());

			payload.setResponseMessage("Content liked.");

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();

			logger.error("like", e);
			payload.setResponseStatus(false);
			payload.setResponseMessage("OOPS! Something went wrong.");
		} finally {
			session.close();
		}

		return payload;

	}
	
	public ResponsePayload unlike(Likes like) {

		ResponsePayload payload = new ResponsePayload();

		Session session = sessionFactory.openSession();
		Transaction tx = null;

		tx = session.beginTransaction();

		try {

			session.delete(like);
				
			tx.commit();
			
			payload=getContent(like.getCategoryID());
			
			payload.setResponseMessage("Content unliked.");


		} catch (Exception e) {
			if (tx != null)
				tx.rollback();

			logger.error("unlike", e);
			payload.setResponseStatus(false);
			payload.setResponseMessage("OOPS! Something went wrong.");
		} finally {
			session.close();
		}

		return payload;

	}
}
