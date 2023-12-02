package com.mind.smg.smppas.queue.dao;

import java.io.Serializable;

import javax.persistence.Entity;

import lombok.Data;

@Data
public class UnitDto implements Serializable {

	private Long unitId;
	
	private Long userId;
	
	private String action;
	
}
