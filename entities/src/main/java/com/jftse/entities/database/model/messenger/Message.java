package com.jftse.entities.database.model.messenger;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Getter
@Setter
@Audited
@Entity
public class Message extends AbstractMessage {
}