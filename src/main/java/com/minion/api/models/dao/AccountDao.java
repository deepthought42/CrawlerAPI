package com.minion.api.models.dao;


import com.minion.api.models.Account;

public interface AccountDao{
  
  public Account findByUsername(String username);

}