package com.qanairy.models.dao;

import com.qanairy.persistence.TestUser;

public interface TestUserDao {
	public TestUser save(TestUser user);
	public TestUser find(String key);
	public TestUser findByUsername(String username);
}
