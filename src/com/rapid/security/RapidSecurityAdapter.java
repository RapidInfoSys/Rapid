/*

Copyright (C) 2018 - Gareth Edwards / Rapid Information Systems

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

package com.rapid.security;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rapid.core.Application;
import com.rapid.server.Rapid;
import com.rapid.server.RapidHttpServlet;
import com.rapid.server.RapidRequest;
import com.rapid.utils.Files;

public class RapidSecurityAdapter extends SecurityAdapter {

	// security object which is marshalled and unmarshalled

	@XmlRootElement
	public static class Security {

		private Roles _roles;
		private Users _users;

		// properties (these are marshalled in and out of the security.xml file)

		public Roles getRoles() { return _roles; }
		public void setRoles(Roles roles) { _roles = roles; }

		public Users getUsers() { return _users; }
		public void setUsers(Users users) { _users = users; }

		// constructors

		public Security() {
			_roles = new Roles();
			_users = new Users();
		}

	}

	// instance variables

	private static Logger _logger = LogManager.getLogger(RapidSecurityAdapter.class);
	private Marshaller _marshaller;
	private Unmarshaller _unmarshaller;
	private Security _security;
	private Map<String, Integer> failedPasswordCheckAttempts = new HashMap<>();

	// constructor

	public RapidSecurityAdapter(ServletContext servletContext, Application application) {
		// call the super method which retains the context and application
		super(servletContext, application);
		// init the marshaller and ununmarshaller
		try {
			JAXBContext jaxb = JAXBContext.newInstance(Security.class);
			_marshaller = jaxb.createMarshaller();
			_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			_marshaller.setAdapter(RapidHttpServlet.getEncryptedXmlAdapter());
			_unmarshaller = jaxb.createUnmarshaller();
			_unmarshaller.setAdapter(RapidHttpServlet.getEncryptedXmlAdapter());
		} catch (JAXBException ex) {
			_logger.error(ex);
		}
		// load the users and roles
		load();
	}

	// instance methods

	private void load() {

		try {

			// create a file object for the security.xml file
			File securityFile = new File(_application.getConfigFolder(_servletContext) + "/security.xml");

			// check the file exists
			if (securityFile.exists()) {

				// unmarshal the xml into an object
				_security = (Security) _unmarshaller.unmarshal(securityFile);

			} else {

				// initialise the security object
				_security = new Security();

			}

			// initialise roles if necessary
			if (_security.getRoles() == null) _security.setRoles(new Roles());

			// initialise users if necessary
			if (_security.getUsers() == null) _security.setUsers(new Users());

			// retain whether we added users or roles to the security object
			boolean modified = false;

			// some old versions of rapid did not have roles with separate names and descriptions check them here
			for (int i = 0; i < _security.getRoles().size(); i ++) {
				// we're using a for loop as we will later the collection
				Role role = _security.getRoles().get(i);
				// if the role has no name
				if (role.getName() == null) {
					// remove it
					_security.getRoles().remove(i);
					// record that we modified
					modified = true;
					// set i back one as we've shrunk the collection
					i--;
				}
			}

			// some old versions of rapid held the users in a hashtable since we are now using a list we need to remove invalid entries
			for (int i = 0; i < _security.getUsers().size(); i ++) {
				// we're using a for loop as we will later the collection
				User user = _security.getUsers().get(i);
				// if the user has no name
				if (user.getName() == null) {
					// remove it
					_security.getUsers().remove(i);
					// record that we modified
					modified = true;
					// set i back one as we've shrunk the collection
					i--;
				}
			}

			// assume we don't have the admin role
			boolean gotAdminRole = false;
			// assume we don't have the design role
			boolean gotDesignRole = false;
			// assume we don't have the users role
			boolean gotUsersRole = false;
			// loop all the roles
			for (Role role : _security.getRoles()) {
				if (Rapid.ADMIN_ROLE.equals(role.getName())) gotAdminRole = true;
				if (Rapid.DESIGN_ROLE.equals(role.getName())) gotDesignRole = true;
				if (Rapid.USERS_ROLE.equals(role.getName())) gotUsersRole = true;
			}
			// if no admin role
			if (!gotAdminRole) {
				// add it
				_security.getRoles().add(new Role(Rapid.ADMIN_ROLE, "Manage application in Rapid Admin"));
				// record that we modified
				modified = true;
			}
			// if no design role
			if (!gotDesignRole) {
				// add it
				_security.getRoles().add(new Role(Rapid.DESIGN_ROLE, "Design application in Rapid Designer"));
				// record that we modified
				modified = true;
			}
			// if no users role
			if (!gotUsersRole) {
				// add it
				_security.getRoles().add(new Role(Rapid.USERS_ROLE, "Only manage users in Rapid Admin"));
				// record that we modified
				modified = true;
			}

			// sort and save the files if we modified it
			if (modified) {
				_security.getRoles().sort();
				save();
			}

		} catch (Exception ex) {
			_logger.error(ex);
		}

	}

	private void save() {

		try {

			// create a file object for the security.xml file
			File securityFile = new File(_application.getConfigFolder(_servletContext) + "/security.xml");

			// create a temp file for saving the application to
			File tempFile = new File(_application.getConfigFolder(_servletContext) + "/security-saving.xml");

			// get a file output stream to write the data to
			FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());

			// marshal the security object to the temp file
			_marshaller.marshal(_security, fos);

			// close the stream
		    fos.close();

		    // copy / overwrite the app file with the temp file
		    Files.copyFile(tempFile, securityFile);

		    // delete the temp file
		    tempFile.delete();

		} catch (Exception ex) {
			_logger.error(ex);
		}

	}


	// overrides

	@Override
	public Roles getRoles(RapidRequest rapidRequest) throws SecurityAdapaterException {
		return _security.getRoles();
	}

	@Override
	public Users getUsers(RapidRequest rapidRequest) throws SecurityAdapaterException {
		return _security.getUsers();
	}

	@Override
	public Role getRole(RapidRequest rapidRequest, String roleName)	throws SecurityAdapaterException {
		for (Role role : _security.getRoles()) if (role.getName().toLowerCase().equals(roleName.toLowerCase())) return role;
		return null;
	}

	@Override
	public User getUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) {
			if (user.getName() != null && rapidRequest.getUserName() != null) {
				if (user.getName().toLowerCase().equalsIgnoreCase(rapidRequest.getUserName().toLowerCase())) return user;
			}
		}
		return null;
	}

	@Override
	public void addRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException {
		Roles roles = _security.getRoles();
		if (!roles.contains(role)) roles.add(role);
		roles.sort();
		save();
	}

	@Override
	public boolean deleteRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		Roles roles = _security.getRoles();
		boolean removed = roles.remove(roleName);
		if (removed) save();
		return removed;
	}

	@Override
	public void addUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		// get the list of users
		Users users = _security.getUsers();
		// get this user name
		String userName = user.getName();
		// users to remove - their may be more than one for legacy reasons
		List<User> removeUsers = new ArrayList<User>();
		// loop the users looking for anyone with the same name as the current user
		for (User u : users) if (u.getName().equalsIgnoreCase(userName)) {
			// remember to remove this user
			removeUsers.add(u);
		}
		// if we are removing any users
		if (removeUsers.size() > 0) {
			// loop remove users
			for (User u : removeUsers) users.remove(u);
		}
		users.add(user);
		users.sort();
		save();
	}

	@Override
	public boolean deleteUser(RapidRequest rapidRequest) throws SecurityAdapaterException {
		for (User user : _security.getUsers()) if (user.getName().equalsIgnoreCase(rapidRequest.getUserName())) {
			boolean removed = _security.getUsers().remove(user);
			if (removed) {
				_security.getUsers().sort();
				save();
			}
			return removed;
		}
		return false;
	}

	@Override
	public void addUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user == null) throw new SecurityAdapaterException("User " + rapidRequest.getUserName() + " cannot be found");
		user.getRoles().add(roleName);
		user.getRoles().sort();
		save();
	}

	@Override
	public boolean deleteUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user == null) throw new SecurityAdapaterException("User " + rapidRequest.getUserName() + " cannot be found");
		boolean removed = user.getRoles().remove(roleName);
		if (removed) {
			user.getRoles().sort();
			save();
		}
		return removed;
	}

	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, String roleName) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user != null) {
			return user.getRoles().contains(roleName);
		}
		return false;
	}

	@Override
	public boolean checkUserRole(RapidRequest rapidRequest, List<String> roleNames) throws SecurityAdapaterException {
		User user = getUser(rapidRequest);
		if (user != null) {
			for (String roleName : roleNames) {
				if (user.getRoles().contains(roleName)) return true;
			}
		}
		return false;
	}

	@Override
	public void updateRole(RapidRequest rapidRequest, Role role) throws SecurityAdapaterException {
		Role currentRole = getRole(rapidRequest, role.getName());
		if (currentRole == null) throw new SecurityAdapaterException("Role " + role.getName() + " cannot be found");
		currentRole.setDescription(role.getDescription());
		save();
	}

	@Override
	public void updateUser(RapidRequest rapidRequest, User user) throws SecurityAdapaterException {
		// get all users
		Users users = _security.getUsers();
		// assume the user was not removed
		boolean removed = false;
		// loop all old users, looking for this user
		for (User oldUser : users) if (oldUser.getName().equalsIgnoreCase(rapidRequest.getUserName())) {
			// retain removed
			removed = users.remove(oldUser);
			// we're done
			break;
		}
		// the form security adapter will return true to any password check so it looks like any user is in the app, only save if user was really in the app
		if (removed) {
			users.add(user);
			users.sort();
			save();
		}
	}

	@Override
	public boolean checkUserPassword(RapidRequest rapidRequest, String userName, String password) throws SecurityAdapaterException {

		// assume no user
		User user = null;
		// assume the password check has failed
		boolean pass = false;

		// change to lower case first to make all equality checks case insensitive
		userName = userName.toLowerCase();

		// loop users
		for (User u : _security.getUsers()) {
			if (u.getName().toLowerCase().equalsIgnoreCase(userName)) {
				user = u;
				break;
			}
		}

		// check we got a user
		if (user != null) {
			// if we were provided with a password and their're not locked
			if (password != null && !user.getIsLocked()) {
				// if the password matches
				if (password.equals(user.getPassword())) {
					// get the application
					Application application = rapidRequest.getApplication();
					// if there was one (soa authentication doesn't)
					if (application == null) {
						pass = true;
					} else {
						// if it has device security
						if (application.getDeviceSecurity()) {
							// check device security as well
							if (user.checkDevice(rapidRequest))
								pass = true;
						} else
							pass = true;
					} // application not null
				} // password != null && not locked
			} // user not null

			// if we passed
			if (pass)
				failedPasswordCheckAttempts.remove(userName);
			else {
				Integer failedCount = 0;
				if (failedPasswordCheckAttempts.containsKey(userName)) failedCount = failedPasswordCheckAttempts.get(userName) + 1;
				failedPasswordCheckAttempts.put(userName, failedCount);
				if (failedCount >= 5) {
					user.setIsLocked(true);
					updateUser(rapidRequest, user);
				}
			}

		} // user != null
		return pass;
	}

}
