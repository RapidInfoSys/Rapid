/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.rapid.core.Application;
import com.rapid.data.ConnectionAdapter;
import com.rapid.data.DataFactory;
import com.rapid.data.DataFactory.Parameters;
import com.rapid.data.DatabaseConnection;
import com.rapid.server.RapidRequest;
import com.rapid.soa.JavaWebservice.Request;
import com.rapid.soa.JavaWebservice.XSDenumeration;
import com.rapid.soa.JavaWebservice.XSDmaxLength;
import com.rapid.soa.JavaWebservice.XSDminLength;
import com.rapid.soa.JavaWebservice.XSDminOccurs;
import com.rapid.soa.JavaWebservice.XSDnillable;
import com.rapid.soa.JavaWebservice.XSDorder;
import com.rapid.soa.Webservice.WebserviceException;

// this class performs the same task as the sql webservice in the demo application
public class PersonSearchRequest implements Request {

	// private instance variables
	private String _forename, _surname;

	// properties
	@XSDorder(1)
	@XSDnillable(false)
	@XSDminOccurs(0)
	@XSDminLength(1)
	@XSDmaxLength(30)
	public String getForename() { return _forename; }
	public void setForename(String forename) { _forename = forename; }

	@XSDorder(2)
	@XSDnillable(false)
	@XSDminLength(1)
	@XSDmaxLength(30)
	public String getSurname() { return _surname; }
	public void setSurname(String surname) { _surname = surname; }

	// a pojo, made even simpler by the public instance variables
	public class Person {

		// public instance variables

		@XSDorder(1)
		public int id;

		@XSDorder(2)
		@XSDminOccurs(0)
		@XSDminLength(1)
		@XSDmaxLength(30)
		public String forename;

		@XSDorder(3)
		@XSDminLength(1)
		@XSDmaxLength(30)
		public String surname;

		@XSDorder(4)
		@XSDminOccurs(0)
		public Date birthday;

		@XSDorder(5)
		@XSDminOccurs(0)
		@XSDenumeration("M,F,U")
		public String gender;

	}

	// an intermediate collection class, could be a traditional simple class too
	public class PersonSearchResponse extends ArrayList<Person> {}

	@Override
	public PersonSearchResponse getResponse(RapidRequest rapidRequest) throws WebserviceException {

		try {

			PersonSearchResponse response = new PersonSearchResponse();

			Application application = rapidRequest.getApplication();

			DatabaseConnection databaseConnection = application.getDatabaseConnections().get(0);

			ConnectionAdapter ca = databaseConnection.getConnectionAdapter(rapidRequest.getRapidServlet().getServletContext(), application);

			DataFactory df = new DataFactory(ca);

			Parameters parameters = new Parameters();
			parameters.add(_forename);
			parameters.add(_surname);

			String sql = "select id, forename, surname, strftime('%s', birthday)*1000 birthday, gender from people where lower(forename) like ifnull(lower(?),'%')||'%' and lower(surname) like lower(?)||'%' order by surname, birthday limit 10";

			ResultSet rs = df.getPreparedResultSet(rapidRequest, sql, parameters);

			while (rs.next()) {

				Person person = new Person();
				person.id = rs.getInt("id");
				person.forename = rs.getString("forename");
				person.surname =  rs.getString("surname");
				person.birthday =  rs.getDate("birthday");
				person.gender =  rs.getString("gender");
				response.add(person);

			}

			df.close();

			return response;

		} catch (Exception ex) {

			throw new WebserviceException(ex);

		}

	}

}
