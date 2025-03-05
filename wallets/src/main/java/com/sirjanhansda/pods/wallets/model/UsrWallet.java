package com.sirjanhansda.pods.wallets.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Entity
@Data
@Getter
@Setter
public class UsrWallet {

    @Id
    private Integer user_id;

    private Integer balance;

    
}
