package com.apcsa.controller;

import java.util.Scanner;
import com.apcsa.data.PowerSchool;
import com.apcsa.data.QueryUtils;
import com.apcsa.model.Administrator;
import com.apcsa.model.User;
import com.apcsa.model.Student;
import com.apcsa.model.Teacher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.InputMismatchException;

public class Application {

    private Scanner in;
    private User activeUser;
    
    enum RootAction { PASSWORD, DATABASE, LOGOUT, SHUTDOWN }
    enum StudentAction { GRADES, GRADESBYCOURSE, PASSWORD, LOGOUT }
    enum AdminAction { FACULTY, FACULTYBYDEPT, STUDENT, STUDENTBYGRADE, STUDENTBYCOURSE, PASSWORD, LOGOUT}

    /**
     * Creates an instance of the Application class, which is responsible for interacting
     * with the user via the command line interface.
     */

    public Application() {
        this.in = new Scanner(System.in);

        try {
            PowerSchool.initialize(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the PowerSchool application.
     */

    public void startup() {
        System.out.println("PowerSchool -- now for students, teachers, and school administrators!");
        boolean login = false;

        // continuously prompt for login credentials and attempt to login

        while (true) {
            System.out.print("\nUsername: ");
            String username = in.next();

            System.out.print("Password: ");
            String password = in.next();
            // if login is successful, update generic user to administrator, teacher, or student

            if (login(username, password)) {
            	login = true;
                activeUser = activeUser.isAdministrator()
                    ? PowerSchool.getAdministrator(activeUser) : activeUser.isTeacher()
                    ? PowerSchool.getTeacher(activeUser) : activeUser.isStudent()
                    ? PowerSchool.getStudent(activeUser) : activeUser.isRoot()
                    ? activeUser : null;
                
                if (isFirstLogin() && !activeUser.isRoot()) {
                    firstTimePassword();
                }
                
                System.out.printf("\nHello again, %s!\n", activeUser.getFirstName());

                while (login) {
                	login = this.requestSelectionLoop(activeUser);
                }
            } else {
                System.out.println("\nInvalid username and/or password.");
            }
        }
    }
    
    /*
     * First time password reset.
     */
    
    private void firstTimePassword() {
    	System.out.print("\nAs a new user, you must change your password. \n\nEnter your new password: ");
        String tempPassword = in.next();
        activeUser.setPassword(tempPassword);
        String hashedPassword = Utils.getHash(tempPassword);
        
        try {
			Connection conn = PowerSchool.getConnection();
			int success = PowerSchool.updatePassword(conn, activeUser.getUsername(), hashedPassword);
			if (success == 1) {
				System.out.println("\nSuccess!");
			}else if (success == -1) {
				System.out.println("Something went wrong.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    /*
     * Request selection loop.
     */
    public boolean requestSelectionLoop(User user) {
    	if (user.isAdministrator()) {
    		switch(getAdminSelection()) {
    			case FACULTY:
    				Administrator.viewFaculty();
    				return true;
    			case FACULTYBYDEPT:
    				Administrator.viewFacultyByDept();
    				return true;
    			case STUDENT: 
    				Administrator.viewStudentEnrollment();
    				return true;
    			case STUDENTBYGRADE:
    				Administrator.viewStudentEnrollmentByGrade();
    				return true;
    			case STUDENTBYCOURSE:
    				Administrator.viewStudentEnrollmentByCourse();
    				return true;
    			case PASSWORD:
    				System.out.println("\nEnter a new password:");
    				String tempPassword = Utils.getHash((in.nextLine()));
    				((Administrator) activeUser).changePassword(tempPassword);
    				return true;
    			case LOGOUT:
    				return false;
    		}
    	}else if (user.isTeacher()) {
    		
    	}else if (user.isStudent()) {
    		switch(getStudentSelection()) {
    			case GRADES:
    				//Student.viewCourseGrades();
    				return true;
    			case GRADESBYCOURSE:
    				//Student.viewAssignmentGradesByCourse();
    				return true;
    			case PASSWORD:
    				((Student) activeUser).changePassword(in);
    				return true;
    			case LOGOUT:
    				return false;
    				
    		}
    	}else if (user.isRoot()) {
    		
    	}
    	
    	return true;
    }
    
    /*
     * Requests selection from any administrator accounts.
     */
    
    public AdminAction getAdminSelection() {
    	int output = 0;
		do {
			System.out.println("\n[1] View faculty.");
			System.out.println("[2] View faculty by department.");
			System.out.println("[3] View student enrollment.");
			System.out.println("[4] View student enrollment by grade.");
			System.out.println("[5] View student enrollment by course.");
			System.out.println("[6] Change password.");
			System.out.println("[7] Logout.");
			try {
				output = in.nextInt();
			} catch (InputMismatchException e) {
				System.out.println("\nYour input was invalid. Please try again.\n");
			}
			in.nextLine(); // clears the buffer
		} while (output < 1 || output > 7);
		
		switch(output) {
		case 1:
			return AdminAction.FACULTY;
		case 2:
			return AdminAction.FACULTYBYDEPT;
		case 3:
			return AdminAction.STUDENT;
		case 4:
			return AdminAction.STUDENTBYGRADE;
		case 5:
			return AdminAction.STUDENTBYCOURSE;
		case 6:
			return AdminAction.PASSWORD;
		case 7:
			return AdminAction.LOGOUT;
		default:
			return null;
	}
		
    }
    
    /*
     * Requests selection from any student accounts.
     */
    
    public StudentAction getStudentSelection() {
    	int output = 0;
    	do {
    		System.out.println("\n[1] View course grades.");
			System.out.println("[2] View assignment grades by course.");
			System.out.println("[3] Change password.");
			System.out.println("[4] Logout.");
			try {
				output = in.nextInt();
			} catch (InputMismatchException e) {
				System.out.println("\nYour input was invalid. Please try again.\n");
			}
			in.nextLine();
    	} while (output < 1 || output > 4);
    	
    	switch(output) {
    		case 1:
    			return StudentAction.GRADES;
    		case 2:
    			return StudentAction.GRADESBYCOURSE;
    		case 3:
    			return StudentAction.PASSWORD;
    		case 4:
    			return StudentAction.LOGOUT;
    		default:
    			return null;
    	}
    }

    /**
     * Logs in with the provided credentials.
     *
     * @param username the username for the requested account
     * @param password the password for the requested account
     * @return true if the credentials were valid; false otherwise
     */

    public boolean login(String username, String password) {
        activeUser = PowerSchool.login(username, password);

        return activeUser != null;
    }

    /**
     * Determines whether or not the user has logged in before.
     *
     * @return true if the user has never logged in; false otherwise
     */

    public boolean isFirstLogin() {
        return activeUser.getLastLogin().equals("0000-00-00 00:00:00.000");
    }

    /////// MAIN METHOD ///////////////////////////////////////////////////////////////////

    /*
     * Starts the PowerSchool application.
     *
     * @param args unused command line argument list
     */

    public static void main(String[] args) {
        Application app = new Application();

        app.startup();
    }
}