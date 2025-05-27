package test.jakarta.data.jpa.hibernate.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link test.jakarta.data.jpa.hibernate.web.City}
 **/
@StaticMetamodel(City.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _City {

	
	/**
	 * @see #id
	 **/
	String ID = "id";
	
	/**
	 * @see #population
	 **/
	String POPULATION = "population";

	
	/**
	 * Static metamodel for attribute {@link City#id}
	 **/
	SortableAttribute<City> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * Static metamodel for attribute {@link City#population}
	 **/
	SortableAttribute<City> population = new SortableAttributeRecord<>(POPULATION);

}

