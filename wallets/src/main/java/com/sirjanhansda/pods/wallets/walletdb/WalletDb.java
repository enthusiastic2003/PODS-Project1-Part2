package com.sirjanhansda.pods.wallets.walletdb;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sirjanhansda.pods.wallets.model.UsrWallet;

import java.util.List;

@Repository
public interface WalletDb extends JpaRepository<UsrWallet, Integer>{

    @Query("SELECT uw FROM UsrWallet uw WHERE uw.user_id = :userid")
    List<UsrWallet> findUsrWalletByUser_id(@Param("userid") Integer userid);

    
}
