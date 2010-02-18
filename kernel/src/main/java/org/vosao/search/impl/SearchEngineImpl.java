/**
 * Vosao CMS. Simple CMS for Google App Engine.
 * Copyright (C) 2009 Vosao development team
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * email: vosao.dev@gmail.com
 */

package org.vosao.search.impl;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.business.Business;
import org.vosao.business.CurrentUser;
import org.vosao.common.AccessDeniedException;
import org.vosao.entity.ContentEntity;
import org.vosao.entity.FileEntity;
import org.vosao.entity.PageEntity;
import org.vosao.search.Hit;
import org.vosao.search.SearchEngine;
import org.vosao.search.SearchResult;
import org.vosao.servlet.IndexTaskServlet;
import org.vosao.utils.StrUtil;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.labs.taskqueue.Queue;

public class SearchEngineImpl implements SearchEngine {

	private static final Log logger = LogFactory.getLog(
			SearchEngineImpl.class);

	
	private static final String INDEX_MOD_DATE = "IndexModDate";
	
	private Business business;
	private HashMap<String, ArrayList<Long>> index;
	private Date indexModDate;

	@Override
	public SearchResult search(String query, int start, int count,
			String language, int textSize) {
		try {
		
		checkIndex();
		SearchResult result = new SearchResult();
		List<String> urls = getContentUrls(getContentIds(query));
		int startIndex = start < urls.size() ? start : urls.size();
		int endIndex = startIndex + count;
		if (count == -1) {
			endIndex = urls.size();
		}
		if (endIndex > urls.size()) {
			endIndex = urls.size();
		}
		for (int i = startIndex; i < endIndex; i++) {
			String url = urls.get(i);
			PageEntity page = getBusiness().getDao().getPageDao()
				.getByUrl(url);
			if (page != null) {
				String content = getBusiness().getDao().getPageDao().getContent(
								page.getId(), language);
				if (content == null) {
					content = getBusiness().getDao().getPageDao().getContent(
							page.getId(), "en");					
				}
				content = StrUtil.extractTextFromHTML(content);
				if (content.length() > textSize) {
					content = content.substring(0, textSize);
				}
				Hit hit = new Hit(page, content);
				result.getHits().add(hit);
			}
		}	
		result.setCount(urls.size());
		return result;
		
		}
		catch (Exception e) {
			e.printStackTrace();
			return new SearchResult();
		}
	}

	private List<String> getContentIds(String query) {
		List<String> ids = new ArrayList<String>();
		if (getIndex().containsKey(query)) {
			for (Long key : getIndex().get(query)) {
				String id = KeyFactory.createKeyString("ContentEntity", key);
				ContentEntity contentEntity = getBusiness().getDao().getContentDao()
						.getById(id);
				if (contentEntity == null) {
					continue;
				}
				ids.add(contentEntity.getParentKey());
			}
		}
		return ids;
	}

	private List<String> getContentUrls(List<String> contentIds) {
		List<String> urls = new ArrayList<String>();
		for (String id : contentIds) {
			PageEntity page = getBusiness().getDao().getPageDao()
				.getById(id);
			if (page != null) {
				if (!page.isSearchable()) {
					continue;
				}
		    	if (getBusiness().getContentPermissionBusiness().getPermission(
		    			page.getFriendlyURL(), CurrentUser.getInstance()).isDenied()) {
		    		continue;
		    	}
		    	PageEntity approvedPage = getBusiness().getDao().getPageDao()
		    			.getByUrl(page.getFriendlyURL());
				if (approvedPage != null && !urls.contains(page.getFriendlyURL())) {
					urls.add(page.getFriendlyURL());
				}
			}
		}
		return urls;
	}
	
	private void checkIndex() {
		if (index == null) {
			loadIndex();
			return;
		}
		Date date = (Date) getBusiness().getSystemService().getCache()
				.getMemcache().get(INDEX_MOD_DATE);
		if (date == null) {
			getBusiness().getSystemService().getCache()
					.getMemcache().put(INDEX_MOD_DATE, indexModDate);
			return;
		}
		if (!date.equals(indexModDate)) {
			loadIndex();
		}
	}
	
	@Override
	public void updateIndex(PageEntity page) {
		List<ContentEntity> contents = getBusiness().getDao().getPageDao()
				.getContents(page.getId());
		for (ContentEntity content : contents) {
			updateIndex(content);
		}
	}

	@Override
	public void updateIndex(ContentEntity content) {
		String data = "";
		data = StrUtil.extractTextFromHTML(content.getContent().toLowerCase());
		logger.info(data);
		String[] words = data.split("\\W+");
		Long key = KeyFactory.stringToKey(content.getId()).getId();
		for (String word : words) {
			if (word.length() < 3) {
				continue;
			}
			if (!getIndex().containsKey(word)) {
				getIndex().put(word, new ArrayList<Long>());
			}
			if (getIndex().get(word).indexOf(key) == -1) {
				getIndex().get(word).add(key);
			}
		}
	}

	public void saveIndex() {
		try {
			byte[] indexContent = StrUtil.zipStringToBytes(indexToString());
			FileEntity file = getBusiness().getFileBusiness().saveFile("/tmp/index.bin", 
					indexContent);
			indexModDate = file.getLastModifiedTime();
			getBusiness().getSystemService().getCache().getMemcache().put(
					INDEX_MOD_DATE, file.getLastModifiedTime());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String indexToString() {
		StringBuffer buf = new StringBuffer();
		int i = 0;
		for (String word : getIndex().keySet()) {
			buf.append(i++ == 0 ? "" : ":").append(word).append("=");
			int j = 0;
			for (Long id : getIndex().get(word)) {
				buf.append(j++ == 0 ? "" : ",").append(id);
			}
		}
		return buf.toString();
	}
	
	private void indexFromString(String data) {
		for (String wordBuf : data.split(":")) {
			String[] wordStruc = wordBuf.split("=");
			index.put(wordStruc[0], new ArrayList<Long>());
			for (String key : wordStruc[1].split(",")) {
				index.get(wordStruc[0]).add(Long.valueOf(key));
			}
		}
	}
	
	public Business getBusiness() {
		return business;
	}

	public void setBusiness(Business business) {
		this.business = business;
	}

	public HashMap<String, ArrayList<Long>> getIndex() {
		if (index == null) {
			loadIndex();
		}
		return index;
	}
	
	private void loadIndex() {
		try {
			index = new HashMap<String, ArrayList<Long>>();
			FileEntity file = getBusiness().getFileBusiness().findFile("/tmp/index.bin");
			if (file == null) {
				return;
			}
			byte[] data = getBusiness().getDao().getFileDao().getFileContent(file);
			if (data != null) {
				String strIndex = StrUtil.unzipStringFromBytes(data);
				indexFromString(strIndex);
				indexModDate = file.getLastModifiedTime();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reindex() {
		/*List<PageEntity> pages = getBusiness().getDao().getPageDao().select();
		Queue queue = getBusiness().getSystemService().getQueue("reindex");
		for (PageEntity page : pages) {
			queue.add(url(IndexTaskServlet.TASK_URL)
					.param("pageId", page.getId()));
		}*/
		reindexInRequest();
	}

	@Override
	public void reindexInRequest() {
		List<PageEntity> pages = getBusiness().getDao().getPageDao().select();
		Queue queue = getBusiness().getSystemService().getQueue("reindex");
		for (PageEntity page : pages) {
			getBusiness().getSearchEngine().updateIndex(page);
		}
		getBusiness().getSearchEngine().saveIndex();
	}
	
}