package com.hsjawanda.firestorerepository;

import com.hsjawanda.firestorerepository.annotation.Collection;
import com.hsjawanda.firestorerepository.annotation.Id;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Collection("everythings")
public class EverythingOkModel implements Firestorable {

	/**
	 * 29/05/2019
	 */
	private static final long serialVersionUID = 1L;

	@Id
	private String id;

}
