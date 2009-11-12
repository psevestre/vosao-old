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

package org.vosao.entity;

import java.io.Serializable;
import java.util.Date;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.vosao.enums.PageState;
import org.vosao.utils.DateUtil;
import org.vosao.utils.UrlUtil;


@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class PageEntity implements Serializable {

	private static final long serialVersionUID = 7L;

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    @Extension(vendorName="datanucleus", key="gae.encoded-pk", value="true")
    private String id;
	
	@Persistent
	private String title;
	
	@Persistent
	private String friendlyURL;
	
	@Persistent
	private String parentUrl;

	@Persistent
	private String template;
	
	@Persistent
	private Date publishDate;
	
	@Persistent
	private boolean commentsEnabled;

	@Persistent
	private Integer version;
	
	@Persistent
	private String versionTitle;

	@Persistent
	private PageState state;
	
	@Persistent
	private Long createUserId;
	
	@Persistent
	private Date createDate;
	
	@Persistent
	private Long modUserId;
	
	@Persistent
	private Date modDate;
	
	public PageEntity() {
		publishDate = new Date();
		state = PageState.EDIT;
		version = 1;
		versionTitle = "New page";
		createDate = new Date();
		modDate = createDate;
	}
	
	public PageEntity(String title, String friendlyURL, 
			String aTemplate, Date publish) {
		this(title, friendlyURL, aTemplate);
		publishDate = publish;
	}

	public PageEntity(String title, String friendlyURL,  
			String aTemplate) {
		this(title, friendlyURL);
		template = aTemplate;
	}

	public PageEntity(String aTitle, String aFriendlyURL) {
		this();
		title = aTitle;
		setFriendlyURL(aFriendlyURL);
	}
	
	public void copy(final PageEntity entity) {
		setTitle(entity.getTitle());
		setFriendlyURL(entity.getFriendlyURL());
		setTemplate(entity.getTemplate());
		setPublishDate(entity.getPublishDate());
		setCommentsEnabled(entity.isCommentsEnabled());
		setVersion(entity.getVersion());
		setVersionTitle(entity.getVersionTitle());
		setState(entity.getState());
		setCreateDate(entity.getCreateDate());
		setCreateUserId(entity.getCreateUserId());
		setModDate(entity.getModDate());
		setModUserId(entity.getModUserId());
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getFriendlyURL() {
		return friendlyURL;
	}
	
	public void setFriendlyURL(String aFriendlyURL) {
		friendlyURL = aFriendlyURL;
		parentUrl = getParentFriendlyURL();
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
	
	public String getParentFriendlyURL() {
		return UrlUtil.getParentFriendlyURL(getFriendlyURL());
	}

	public void setParentFriendlyURL(final String url) {
		if (getFriendlyURL() == null) {
			setFriendlyURL(url);
		}
		else {
			setFriendlyURL(url + "/" + getPageFriendlyURL());
		}
	}

	public String getPageFriendlyURL() {
		return UrlUtil.getPageFriendlyURL(getFriendlyURL());
	}

	public void setPageFriendlyURL(final String url) {
		if (getFriendlyURL() == null) {
			setFriendlyURL(url);
		}
		else {
			if (parentUrl.equals("/")) {
				friendlyURL = parentUrl + url;
			}
			else {
				friendlyURL = parentUrl + "/" + url;
			}
		}
	}

	public Date getPublishDate() {
		return publishDate;
	}

	public String getPublishDateString() {
		return DateUtil.toString(publishDate);
	}

	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	public void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}
	
	public boolean isRoot() {
		return friendlyURL.equals("/");
	}
	
	public boolean equals(Object object) {
		if (object instanceof PageEntity) {
			PageEntity entity = (PageEntity)object;
			if (getId() == null && entity.getId() == null) {
				return true;
			}
			if (getId() != null && getId().equals(entity.getId())) {
				return true;
			}
		}
		return false;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getVersionTitle() {
		return versionTitle;
	}

	public void setVersionTitle(String versionTitle) {
		this.versionTitle = versionTitle;
	}

	public PageState getState() {
		return state;
	}

	public String getStateString() {
		return state.name();
	}

	public void setState(PageState state) {
		this.state = state;
	}

	public Long getCreateUserId() {
		return createUserId;
	}

	public void setCreateUserId(Long createUserId) {
		this.createUserId = createUserId;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Long getModUserId() {
		return modUserId;
	}

	public void setModUserId(Long modUserId) {
		this.modUserId = modUserId;
	}

	public Date getModDate() {
		return modDate;
	}

	public String getModDateString() {
		return DateUtil.dateTimeToString(modDate);
	}

	public String getCreateDateString() {
		return DateUtil.dateTimeToString(createDate);
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	
	public boolean isApproved() {
		return state.equals(PageState.APPROVED);
	}

	public String getParentUrl() {
		return parentUrl;
	}

	public void setParentUrl(String parentUrl) {
		this.parentUrl = parentUrl;
	}
	
}
