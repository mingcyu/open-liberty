package test.jakarta.data.jpa.hibernate.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link test.jakarta.data.jpa.hibernate.web.City}
 **/
@StaticMetamodel(City.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _City {

	
	/**
	 * @see #name
	 **/
	String NAME = "name";
	
	/**
	 * @see #stateName
	 **/
	String STATE_NAME = "stateName";
	
	/**
	 * @see #population
	 **/
	String POPULATION = "population";

	
	/**
	 * Static metamodel for attribute {@link City#name}
	 **/
	TextAttribute<City> name = new TextAttributeRecord<>(NAME);
	
	/**
	 * Static metamodel for attribute {@link City#stateName}
	 **/
	TextAttribute<City> stateName = new TextAttributeRecord<>(STATE_NAME);
	
	/**
	 * Static metamodel for attribute {@link City#population}
	 **/
	SortableAttribute<City> population = new SortableAttributeRecord<>(POPULATION);

}

