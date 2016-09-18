package com.minion.api.models.dao;

import org.springframework.data.repository.CrudRepository;

import com.minion.api.models.Account;

public interface AccountDao extends  CrudRepository<Account, String> {
  
  public Account findByUsername(String username);

}