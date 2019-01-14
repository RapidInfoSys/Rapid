/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. The terms require you
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.soa;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.server.RapidRequest;
import com.rapid.soa.SOAElementRestriction.EnumerationRestriction;
import com.rapid.soa.SOAElementRestriction.MaxOccursRestriction;
import com.rapid.soa.SOAElementRestriction.MinOccursRestriction;
import com.rapid.soa.SOASchema.SOASchemaElement;
import com.rapid.utils.Classes;

public class JavaWebservice extends Webservice {

	// all classes used by JavaWebservices are expected to implement this interface
	public interface Request {

		public Object getResponse(RapidRequest rapidRequest) throws WebserviceException;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
	public @interface XSDname { public String name(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD}) // make an object with the choices as it's child elements and put choice on parent reference to it
	public @interface XSDchoice {}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDtype { public String name(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDorder { public int value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminOccurs { public int value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxOccurs { public int value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDnillable { public boolean value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminLength { public int value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxLength { public int value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDpattern { public String value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDenumeration { public String value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminInclusive{ public String value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxInclusive { public String value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDminExclusive{ public String value(); }

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface XSDmaxExclusive { public String value(); }

	// schema element properties can come from fields or methods so we collect them before sorting
	public static class ElementProperty {

		// private final statics
		private static final int TYPE_SIMPLE = 0;
		private static final int TYPE_ARRAY = 1;
		private static final int TYPE_COMPLEX = 2;
		private static final int TYPE_METHOD = 3;
		private static final int TYPE_FIELD = 4;
		private static final int TYPE_CHOICE = 5;

		// private instance variables
		private String _name, _elementName;
		private Class _class;
		private Annotation[] _annotations;
		private int _type;

		// properties
		public String getName() { return _name; }
		public Class getElementClass() { return _class; }
		public String getElementName() { return _elementName; }
		public Annotation[] getAnnotations() { return _annotations; }
		public int getType() { return _type; }

		// constructor
		public ElementProperty(String name, Class propertyClass, Annotation[] annotations, int type) {
			_name = name;
			_class = propertyClass;
			_annotations = annotations;
			_type = type;
			// assume element name is from name
			_elementName = name;
			// if not in all caps, make first letter lower case
			if (!name.equals(name.toUpperCase())) _elementName = name.substring(0,1).toLowerCase() + name.substring(1); //_class.getSimpleName().substring(0,1).toLowerCase() + _class.getSimpleName().substring(1);
			// if there are annotations, look for name and update
			if (_annotations != null) {
				for (Annotation a : _annotations) {
					if (a instanceof XSDname) {
						XSDname n = (XSDname) a;
						_elementName = n.name();
						break;
					}
				}
			}
		}

		// methods
		public int getOrder() {
			if (_annotations != null) {
				for (Annotation a : _annotations) {
					if (a instanceof XSDorder) {
						XSDorder o = (XSDorder) a;
						return o.value();
					}
				}
			}
			return -1;
		}

	}

	// private variables
	private boolean _requestCached, _responseCached;
	private String _className;
	private Logger _logger;
	private Map<String, ElementProperty> _elementProperties;
	private Map<String,List<ElementProperty>> _childElementProperties;
	private SimpleDateFormat _dateFormat, _dateTimeFormat;

	// properties

	// this is the request object, it must implement the JavaWebserviceRequest interface
	public String getClassName() { return _className; }
	public void setClassName(String className) { _className = className; }

	// constructors

	// used by Jaxb
	public JavaWebservice() {
		_logger = LogManager.getLogger(this.getClass());
		_elementProperties = new HashMap<String,ElementProperty>();
		_childElementProperties = new HashMap<String,List<ElementProperty>>();
		_dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		_dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	}

	// used by Designer
	public JavaWebservice(String name) {
		this();
		setName(name);
	}

	// private methods

	// is this an arry type
	private static boolean isArray(Class c) {
		return c.isArray() || c == java.util.List.class || Classes.implementsClass(c, java.util.List.class) ? true : false;
	}

	// is this a java class or any of the primitives
	private static boolean isSimpleType(Class c) {
		return !isArray(c) && (
			c.getName().indexOf("java") == 0 ||
			c.getName().equals("int") ||
			c.getName().equals("boolean") ||
			c.getName().equals("float")
		) ? true : false;
	}

	// what is the simple type
	private static int getSimpleType(Class c) {
		String simpleName = c.getSimpleName();
		int type = SOASchema.STRING;
		if (simpleName.equalsIgnoreCase("boolean")) { type = SOASchema.BOOLEAN; }
		else if (simpleName.equalsIgnoreCase("date")) { type = SOASchema.DATE; }
		else if (simpleName.equalsIgnoreCase("timestamp")) { type = SOASchema.DATETIME; }
		else if (simpleName.equalsIgnoreCase("float")) { type = SOASchema.DECIMAL; }
		else if (simpleName.equalsIgnoreCase("int") || simpleName.equalsIgnoreCase("Integer")) { type = SOASchema.INTEGER; }
		return type;
	}

	private boolean isXSDAnnotation(Annotation a) {
		if (a instanceof XSDchoice ||
			a instanceof XSDname ||
			a instanceof XSDtype ||
			a instanceof XSDorder ||
			a instanceof XSDminOccurs ||
			a instanceof XSDmaxOccurs ||
			a instanceof XSDnillable ||
			a instanceof XSDminLength ||
			a instanceof XSDmaxLength||
			a instanceof XSDpattern ||
			a instanceof XSDenumeration ||
			a instanceof XSDminInclusive ||
			a instanceof XSDmaxInclusive ||
			a instanceof XSDminExclusive ||
			a instanceof XSDmaxExclusive ) {
			return true;
		} else {
			return false;
		}
	}

	// whether there is a set method for a corresponding get
	private boolean containsSetMethod(Method[] methods, Method method) {
		for (Method m : methods) {
			if (m.getName().equals("set" + method.getName().substring(3))) return true;
		}
		return false;
	}

	// get a list of element properties for a class
	private List<ElementProperty> getChildElements(Class c) {

		// a list of properties in the class
		List<ElementProperty> elementProperties = new ArrayList<ElementProperty>();

		// return them
		return elementProperties;

	}


	// get the child schema elements
	private List<SOASchemaElement> getChildClassSchemaElements(Class c, String parentId) {

		// a list of properties in the class
		List<ElementProperty> elementProperties = new ArrayList<ElementProperty>();

		// get all class methods
		Method[] methods = c.getMethods();
		// if we got some
		if (methods != null) {
			// loop all retained method names looking for get/set pairs
			for (Method m : methods) {
				// work with the get methods as we need the return type for our class
				if (m.getName().startsWith("get")) {
					// if we have a corresponding set method in the list
					if (containsSetMethod(methods, m)) {
						// get the return class
						Class rc = m.getReturnType();
						// check if array
						elementProperties.add(new ElementProperty(m.getName().substring(3), rc, m.getAnnotations(), ElementProperty.TYPE_METHOD));
					}
				}
			}
		}

		// get all fields
		Field[] fields = c.getFields();
		// if we got some
		if (fields != null) {
			// loop them
			for (Field f : fields) {
				// get the annotations
				Annotation[] annotations = f.getAnnotations();
				// if we got some
				if (annotations != null) {
					// loop the annotations
					for (Annotation a : annotations) {
						// if this is an XSD annotation
						if (isXSDAnnotation(a)) {
							// put our property collection
							elementProperties.add(new ElementProperty(f.getName(), f.getType(), f.getAnnotations(), ElementProperty.TYPE_FIELD));
							// we're done with the annotations
							break;
						}
					}
				}
			}
		}

		// sort the properties
		Collections.sort(elementProperties, new Comparator<ElementProperty>() {
			@Override
			public int compare(ElementProperty p1, ElementProperty p2) {
				return p1.getOrder() - p2.getOrder();
			}
		});

		// get the class name for looking up this class
		String className = c.getCanonicalName();

		// cache element properties for when we produce the output
		_childElementProperties.put(className, elementProperties);

		// the list we're making
		List<SOASchemaElement> elements = new ArrayList<SOASchemaElement>();
		// first id
		int id = 0;
		// loop the properties
		for (ElementProperty p : elementProperties) {
			// add a schema element for the property class
			elements.add(getClassSchemaElement(p.getName(), p.getElementClass(), p.getAnnotations(), parentId + "." + id, c));
			// increment the id
			id++;
		}
		// return
		return elements;
	}

	private Class getArrayClass(String name, Class c, Class parent) {

		// if it is a list, or implements it
		if (c == java.util.List.class || Classes.implementsClass(c, java.util.List.class)) {

			// get any generic super class type
			ParameterizedType pt = (ParameterizedType) c.getGenericSuperclass();
			// if we got one
			if (pt != null) {
				// get it's name
				Type t = pt.getActualTypeArguments()[0];
				// get the name, trimming out the prefix
				String n = t.toString().replace("class ", "");
				// get the class
				try {
					c = Class.forName(n);
				} catch (ClassNotFoundException ex) {
					_logger.error("Error creating schema element for class " + n, ex);
				}
				// we're done
				return c;

			} else {

				// start looking in fields as this seems faster
				if (pt == null) {
					// get fields
					Field[] fields = parent.getFields();
					// loop them
					for (Field field : fields) {
						// if this is our field
						if (field.getName().equals(name)) {
							// get the generic type
							pt = (ParameterizedType) field.getGenericType();
							// we're done
							break;
						}
					}
				}

				// if we didn't get the type from the fields
				if (pt == null) {
					// start by getting methods
					Method[] methods = parent.getMethods();
					// loop them
					for (Method method : methods) {
						// if it implements List and end with our name
						if (method.getName().endsWith(name) && Classes.implementsClass(method.getReturnType(), java.util.List.class)) {
							// get the type
							pt = (ParameterizedType) method.getGenericReturnType();
							// we're done
							break;
						}

					}
				}
			}

			// if we got a type, get the class
			if (pt == null) {
				return null;
			} else {
				return (Class) pt.getActualTypeArguments()[0];
			}

		} else {
			// get the array type
			return c.getComponentType();
		}

	}

	private SOASchemaElement getClassSchemaElement(String name, Class c, Annotation[] annotations, String id, Class parent) {
		// the schema element we're making
		SOASchemaElement e = new SOASchemaElement();
		// set it's name, preserving any full caps, otherwise lower the first letter and keep the rest
		e.setName(name.equals(name.toUpperCase()) ? name : name.substring(0,1).toLowerCase() + name.substring(1));
		// assume no name override
		boolean nameOverride = false;
		// if there are annotations, look for name and update
		if (annotations != null) {
			for (Annotation a : annotations) {
				if (a instanceof XSDname) {
					XSDname n = (XSDname) a;
					e.setName(n.name());
					nameOverride = true;
					break;
				}
			}
		}
		// set it's id
		e.setId(id);
		// simple type check
		if (isSimpleType(c)) {

			// set it's type
			e.setDataType(getSimpleType(c));
			// cache element properties them for when we produce the output
			_elementProperties.put(c.getCanonicalName(), new ElementProperty(c.getName(), c, c.getAnnotations(), ElementProperty.TYPE_SIMPLE));

		} else {

			// assume non enclosing
			Class complexClass = c;

			// if it's an array type
			if (isArray(c)) {

				// assume simple property name
				String propertyName = complexClass.getCanonicalName();

				// get array class
				complexClass = getArrayClass(name, c, parent);

				// if list update property name
				if (c == java.util.List.class || Classes.implementsClass(c, java.util.List.class)) propertyName = name + "_List";

				// base name on complex class
				String arrayName =  complexClass.getSimpleName().substring(0,1).toLowerCase() +  complexClass.getSimpleName().substring(1);

				// cache element properties them for when we produce the output
				_elementProperties.put(propertyName, new ElementProperty(arrayName, complexClass, complexClass.getAnnotations(), ElementProperty.TYPE_ARRAY));

				// set flag
				e.setIsArray(true);

				// set name after class if no override
				if (!nameOverride) e.setName(arrayName);

			}

			// get the key
			String className = complexClass.getCanonicalName();
			// get the element property
			ElementProperty property = new ElementProperty(name, complexClass, complexClass.getAnnotations(), ElementProperty.TYPE_COMPLEX);

			// cache element properties for when we produce the output
			_elementProperties.put(className, property);

			// add child elements
			e.setChildElements(getChildClassSchemaElements(complexClass, id));

		}

		// if there are some annotations
		if (annotations != null) {
			// loop them
			for (Annotation a : annotations) {
				// if this is an xsd annotation
				if (isXSDAnnotation(a)) {
					// add appropriate restriction
					if (a instanceof XSDchoice) {
						XSDchoice x = (XSDchoice) a;
						// set the choice indicator
						e.setIsChoice(true);
					} else if (a instanceof XSDminOccurs) {
						XSDminOccurs x = (XSDminOccurs) a;
						e.addRestriction(new MinOccursRestriction(x.value()));
					} else if (a instanceof XSDmaxOccurs) {
						XSDmaxOccurs x = (XSDmaxOccurs) a;
						e.addRestriction(new MaxOccursRestriction(x.value()));
					} else if (a instanceof XSDnillable) {
						XSDnillable x = (XSDnillable) a;
					} else if (a instanceof XSDminLength) {
						XSDminLength x = (XSDminLength) a;
					} else if (a instanceof XSDmaxLength) {
						XSDmaxLength x = (XSDmaxLength) a;
					} else if (a instanceof XSDpattern) {
						XSDpattern x = (XSDpattern) a;
					} else if (a instanceof XSDenumeration) {
						XSDenumeration x = (XSDenumeration) a;
						e.addRestriction(new EnumerationRestriction(x.value()));
					} else if (a instanceof XSDminInclusive) {
						XSDminInclusive x = (XSDminInclusive) a;
					} else if (a instanceof XSDmaxInclusive) {
						XSDmaxInclusive x = (XSDmaxInclusive) a;
					} else if (a instanceof XSDminExclusive) {
						XSDminExclusive x = (XSDminExclusive) a;
					} else if (a instanceof XSDmaxExclusive ) {
						XSDmaxExclusive x = (XSDmaxExclusive) a;
					}
				}
			}
		}
		// return it
		return e;
	}

	private SOASchema getClassSchema(Class c) {
		// the schema we're making
		SOASchema classSchema = new SOASchema();
		// get the element for the root class
		SOASchemaElement rootSchemaElement = getClassSchemaElement(c.getSimpleName(), c, c.getAnnotations(), "0", null);
		// add it to the schema
		classSchema.setRootElement(rootSchemaElement);
		// return it
		return classSchema;
	}

	// get the class of our named request object
	private Class getRequestClass() throws Exception {
		// the class we are hoping for
		Class c = null;
		// if we have a class name
		if (_className != null) {
			// get the class
			c = Class.forName(_className);
			// make sure it implements JavaWebservice.Response
			if (!Classes.implementsClass(c, com.rapid.soa.JavaWebservice.Request.class)) throw new WebserviceException("Webservice action class " + c.getCanonicalName() + " must implement com.rapid.soa.JavaWebservice.Response.");
		}
		// return
		return c;
	}

	// a recursive method for inflating child objects from a list of SOAElements
	public void setChildObjects(Object o, Class c, List<SOAElement> childElements) throws NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException, InstantiationException {

		// if we got what we need
		if (o != null && childElements != null) {
			if (childElements.size() > 0) {

				// get the class name for looking up this class
				String className = c.getCanonicalName();

				// get the element properties
				List<ElementProperty> childProperties = _childElementProperties.get(className);

				// check we got some
				if (childProperties != null) {

					// loop the properties
					for (ElementProperty childElementProperty : childProperties) {

						// look for a list property to hold the array
						ElementProperty listProperty = _elementProperties.get(childElementProperty.getName() + "_List");

						// check array
						if (listProperty != null) {

							// the list we want to retrieve
							List<Object> list = null;

							// retrieve any existing list
							if (childElementProperty.getType() == ElementProperty.TYPE_METHOD) {

								// get the method value
								Method method = c.getDeclaredMethod("get" + childElementProperty.getName(), childElementProperty.getElementClass());

								// get the list in the method
								list = (List<Object>) method.invoke(o);

								// check we got one
								if (list == null) {

									// make one
									list = new ArrayList<Object>();

									// get the set method
									Method setMethod = c.getDeclaredMethod("set" + childElementProperty.getName(), childElementProperty.getElementClass());

									// add it
									setMethod.invoke(o, list);

								}

							} else if (childElementProperty.getType() == ElementProperty.TYPE_FIELD) {

								// get the field
								Field field = c.getDeclaredField(childElementProperty.getName());

								// get the list in the field
								list = (List<Object>) field.get(o);

								// check we got one
								if (list == null) {

									// make one
									list = new ArrayList<Object>();

									// add it
									field.set(o, list);

								}

							} // list type check

							// loop the childElements
							for (SOAElement childElement : childElements) {

								// match?
								if ((listProperty.getName() + "Array").equalsIgnoreCase(childElement.getName())) {

									// get the class
									Class listItemClass = listProperty.getElementClass();

									// get the parameterless constructor
									Constructor constructor = listItemClass.getConstructor();

									// create an object
									Object listItemObject = constructor.newInstance();

									// get the child nodes
									List<SOAElement> listChildElements = childElement.getChildElements();

									// recursively set child objects
									setChildObjects(listItemObject, listItemClass, listChildElements);

									// add it
									list.add(listItemObject);

								}

							} // child element loop

						} else {

							// loop the childElements
							for (SOAElement childElement : childElements) {

								if (childElementProperty.getName().equalsIgnoreCase(childElement.getName())) {

									// get the string value
									String stringValue =  childElement.getValue();

									// place holder for the final value
									Object value = null;

									// get the child class
									Class childClass = childElementProperty.getElementClass();

									// check is simple type
									if (isSimpleType(childClass)) {

										// get the child object class type
										int simpleType = getSimpleType(childClass);

										// get the string value based on type
										switch (simpleType) {
										case SOASchema.STRING :
											value = stringValue;
											break;
										case SOASchema.BOOLEAN :
											value = Boolean.parseBoolean(stringValue);
											break;
										case SOASchema.DATE :
											java.util.Date d = _dateFormat.parse(stringValue);
											value = new java.sql.Date(d.getTime());
											break;
										case SOASchema.DATETIME :
											java.util.Date dt = _dateTimeFormat.parse(stringValue);
											value = new Timestamp(dt.getTime());
											break;
										case SOASchema.DECIMAL :
											value = Float.parseFloat(stringValue);
											break;
										case SOASchema.INTEGER :
											value = Integer.parseInt(stringValue);
											break;
										}

									} else {

										// get the parameterless constructor
										Constructor constructor = childClass.getConstructor();

										// create an object for the value
										value = constructor.newInstance();

										// populate this object
										setChildObjects(value, childClass, childElement.getChildElements());

									}

									if (childElementProperty.getType() == ElementProperty.TYPE_METHOD) {

										// get the method value
										Method method = c.getDeclaredMethod("set" + childElementProperty.getName(), childElementProperty.getElementClass());

										// set the object in the method
										method.invoke(o, value);

									} else if (childElementProperty.getType() == ElementProperty.TYPE_FIELD) {

										// get the field
										Field field = c.getDeclaredField(childElement.getName());

										// get the object in the field
										field.set(o, value);

									}

									// we're done with the childElement
									break;

								} // name match

							} // array check

						} // childElements loop

					} // ElementProperty loop

				} // properties check

			} // childElements size check

		} // childElements check


	}

	// a public method for inflating any object from soaData
	public Object getObject(SOAData soaData, Class c) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, ParseException {

		// the object we want to return
		Object o = null;

		// get the root element
		SOAElement root = soaData.getRootElement();

		// if we got one
		if (root != null) {

			// get the parameterless constructor
			Constructor constructor = c.getConstructor();

			// create an object
			o = c.newInstance();

			// get the child nodes
			List<SOAElement> childElements =  root.getChildElements();

			// recursively set child objects
			setChildObjects(o, c, childElements);

		} // root check

		// return
		return o;

	}

	// turn the request data into the request object!
	private Request getRequestObject(SOAData soaData, Class c) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, ParseException {

		// make sure the request schema and all element properties have been cached
		getRequestSchema();

		// the request we want to return
		Request r = null;

		// get the object we are making
		Object o = getObject(soaData, c);

		// if we got one, cast to request
		if (o != null) r = (Request) o;

		// return
		return r;

	}

	// get an SOA element from an object (used to generate the response)
	private SOAElement getResponseSOAElement(Object object, SOAElement parentElement, String propertyName) throws NoSuchMethodException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		// make sure the schema and all element properties has been cached
		getResponseSchema();

		// get the class
		Class c = object.getClass();

		// get the class SOA properties
		ElementProperty properties  = _elementProperties.get(propertyName);

		// place holder for the element
		SOAElement element = null;

		if (properties == null) {

			throw new NoSuchFieldException("Properties for " + propertyName + " not found!");

		} else {

			if (properties.getType() == ElementProperty.TYPE_ARRAY) {

				// create the array SOA element
				element = new SOAElement(properties.getElementName(), true);

				// check implements List, or classic array
				if (c == java.util.List.class || Classes.implementsClass(c, java.util.List.class)) {

					// get the list of children
					List<Object> childObjects = (List<Object>) object;

					// loop them
					for (Object childObject : childObjects) {

						// get the child element into the array
						getResponseSOAElement(childObject, element, properties.getElementClass().getCanonicalName());

						// close the array so the next addition goes onto a new child
						element.closeArray();

					}

				} else {

					// get the array of children
					Object[] childObjects = (Object[]) object;

					// loop them
					for (Object childObject : childObjects) {

						// get the child element into the array
						getResponseSOAElement(childObject, element, properties.getElementClass().getCanonicalName());

						// close the array so the next addition goes onto a new child
						element.closeArray();

					}

				}

			} else {

				// check if we are adding to a parent element
				if (parentElement == null) {
					// create the SOA element
					element = new SOAElement(properties.getElementName());
				} else {
					// use the given parent element
					element = parentElement;
				}

				// get the child element properties
				List<ElementProperty> childElementProperties = _childElementProperties.get(c.getCanonicalName());

				// if we got some
				if (childElementProperties != null) {

					// loop them
					for (ElementProperty childElementProperty : childElementProperties) {

						// place holder for the child object we're about to retrieve
						Object childObject = null;

						// check the type
						if (childElementProperty.getType() == ElementProperty.TYPE_METHOD) {

							// get the method value
							Method method = c.getDeclaredMethod("get" + childElementProperty.getName());

							// get the object in the method
							childObject = method.invoke(object);

						} else if (childElementProperty.getType() == ElementProperty.TYPE_FIELD) {

							// get the field
							Field field = c.getDeclaredField(childElementProperty.getName());

							// get the object in the field
							childObject = field.get(object);

						} else {

							childObject = "Unknown type";

						}

						// if we got a child object
						if (childObject != null) {

							// check it's type
							if (isSimpleType(childObject.getClass())) {

								// placeholder for the value
								String value = null;

								// get the child object object type
								int simpleType = getSimpleType(childObject.getClass());

								// get the string value based on type
								switch (simpleType) {
								case SOASchema.STRING :
									value = (String) childObject;
									break;
								case SOASchema.BOOLEAN :
									if ((Boolean) childObject) {
										value = "true";
									} else {
										value = "false";
									}
									break;
								case SOASchema.DATE :
									value = _dateFormat.format(childObject);
									break;
								case SOASchema.DATETIME :
									value = _dateTimeFormat.format(childObject);
									break;
								case SOASchema.DECIMAL :
									value = Float.toString((Float) childObject);
									break;
								case SOASchema.INTEGER :
									value = Integer.toString((Integer) childObject);
									break;
								}

								// add the child with its value
								element.addChildElement(new SOAElement(childElementProperty.getElementName(), value));

							} else {

								// assume simple child property name
								String childPropertyName = childObject.getClass().getCanonicalName();

								// get child object class
								Class childClass = childObject.getClass();

								// if it implements list set property name accordingly - not sure we need this...
								if (childClass == java.util.List.class || Classes.implementsClass(childClass, java.util.List.class)) childPropertyName = childElementProperty.getName() + "_List";

								// add the child the more complex way
								element.addChildElement(getResponseSOAElement(childObject, null, childPropertyName));

							} // simple type check

						} // childObject check

					} // child element loop

				} // child element check

			} // property type check

		} // got properties check

		// return it
		return element ;
	}

	@Override
	public SOASchema getRequestSchema() {
		// useful for debugging
		//_requestSchema = null;
		if (_requestSchema == null || !_requestCached) {
			try {
				// get the request class
				Class requestClass = getRequestClass();
				// if we have one make a schema from it
				if (requestClass != null) _requestSchema = getClassSchema(requestClass);
				// update that it is now cached - this allows anything passed in from JAXB to be overrwritten
				_requestCached = true;
			} catch (Exception ex) {
				_logger.error("Error creating request schema for Java webservice", ex);
			}
		}
		return _requestSchema;
	}

	@Override
	public SOASchema getResponseSchema() {
		// useful for debugging
		//_responseSchema = null;
		if (_responseSchema == null || !_responseCached) {
			try {
				// get the request class
				Class requestClass = getRequestClass();
				// if we have one
				if (requestClass != null) {
					// get the response method
					Method responseMethod = requestClass.getMethod("getResponse", RapidRequest.class);
					// get the response class
					Class responseClass = responseMethod.getReturnType();
					// now make a schema from it
					_responseSchema = getClassSchema(responseClass);
					// update that it is now cached - this allows anything passed in from JAXB to be overrwritten
					_responseCached = true;
				}
			} catch (Exception ex) {
				_logger.error("Error creating response schema for Java webservice", ex);
			}
		}
		return _responseSchema;
	}

	@Override
	public SOAData getResponseData(RapidRequest rapidRequest, SOAData requestData)	throws WebserviceException {

		// instantiate an instance of _className
		// use requestData to inflate it's properties (or public instance variables)
		// invoke it's JavaWebservice.Response.getResponse() method
		// marshal the response object to an SOAData object
		// return the SOAData object (and the printer will take care of the rest)

		try {

			// get the request class
			Class requestClass = getRequestClass();

			// get an instance of the request object
			JavaWebservice.Request requestObject = getRequestObject(requestData, requestClass);

			// get the response method
			Method responseMethod = requestClass.getMethod("getResponse", RapidRequest.class);

			// invoke the response method
			Object responseObject = responseMethod.invoke(requestObject, rapidRequest);

			// get the response Class
			Class c = responseObject.getClass();

			// Assume property name is canonical name
			String propertyName = responseObject.getClass().getCanonicalName();

			// if the class is a list change property name
			if (c == java.util.List.class || Classes.implementsClass(c, java.util.List.class)) propertyName = c.getSimpleName() + "_List";

			// get an SOA element from the object
			SOAElement responseElement = getResponseSOAElement(responseObject, null, propertyName);

			// get the response data
			SOAData responseData = new SOAData(responseElement, _responseSchema);

			// return the response data
			return responseData;

		} catch (Exception ex) {

			throw new WebserviceException(ex);

		}

	}

}