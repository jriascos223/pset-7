package com.apcsa.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.apcsa.controller.Utils;
import com.apcsa.data.*;
import com.apcsa.model.User;

public class Student extends User {

	private int studentId;
	private int classRank;
	private int gradeLevel;
	private int graduationYear;
	private double gpa;
	private String firstName;
	private String lastName;
    
    public Student(User user, ResultSet rs) throws SQLException {
    	super(user);
    	
    	this.studentId = rs.getInt("student_id");
    	this.classRank = rs.getInt("class_rank");
    	this.gradeLevel = rs.getInt("grade_level");
    	this.graduationYear = rs.getInt("graduation");
    	this.gpa = rs.getDouble("gpa");
    	this.firstName = rs.getString("first_name");
    	this.lastName = rs.getString("last_name");
	}
	
	public Student(ResultSet rs) throws SQLException {
		//user id, account type, username, password, last login
		super(rs.getInt("user_id"), rs.getString("account_type"), rs.getString("username"), rs.getString("auth"), rs.getString("last_login"));

		this.studentId = rs.getInt("student_id");
    	this.classRank = rs.getInt("class_rank");
    	this.gradeLevel = rs.getInt("grade_level");
    	this.graduationYear = rs.getInt("graduation");
    	this.gpa = rs.getDouble("gpa");
    	this.firstName = rs.getString("first_name");
		this.lastName = rs.getString("last_name");
	}
    
    public String getFirstName() {
    	return firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}

	public int getGraduation() {
		return this.graduationYear;
	}

	public int getGradeLevel() {
		return this.gradeLevel;
	}

	public int getStudentId() {
		return this.studentId;
	}
    
    
    /**
     * Function that both changes the property of the object as well as the data in the database.
     * @param in the Scanner
     */

	public void changePassword(Scanner in) {
		System.out.println("\nEnter current password:");
        String currentPassword = in.nextLine();
        currentPassword = Utils.getHash(currentPassword);
    	
    	if (currentPassword.equals(this.password)) {
    		System.out.println("\nEnter a new password:");
    		String password = Utils.getHash((in.nextLine()));
    		this.setPassword(password);
        	try {
        		Connection conn = PowerSchool.getConnection();
        		PowerSchool.updatePassword(conn, this.getUsername(), password);
        	} catch (SQLException e){
        		System.out.println(e);
        	}
    	}else {
    		System.out.println("\nIncorrect current password.");
    	}
		
	}

	public void viewCourseGrades() {
		System.out.print("\n");
		try (Connection conn = PowerSchool.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(QueryUtils.GET_STUDENT_COURSES);
			stmt.setInt(1, this.getStudentId());
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					System.out.println(rs.getString("title") + " / " + rs.getInt("grade"));
				}
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	public void viewAssignmentGradesByCourse(Scanner in) {
		System.out.print("\n");
		ArrayList<String> course_nos = new ArrayList<String>();
		ArrayList<String> course_ids = new ArrayList<String>();
		
		int count = 1;
		int input = 0;
		int selection = 0;
		String selectionString = "";
		
		try (Connection conn = PowerSchool.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(QueryUtils.GET_STUDENT_COURSES);
			stmt.setInt(1, this.getStudentId());
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					System.out.println("[" + count + "] " + rs.getString("course_no"));
					count++;
					course_nos.add(rs.getString("course_no"));
					course_ids.add(rs.getString("course_id"));
				}
				System.out.print("\n::: ");
			} catch (SQLException e) {
				System.out.println(e);
			}
		} catch (SQLException e) {
			System.out.println(e);
		}

		try {
			input = in.nextInt();
		} catch (InputMismatchException e) {
			System.out.println("\nYour input was invalid. Please try again.");
		} finally {
			in.nextLine();
		}

		System.out.println("\n[1] MP1 Assignment.");
		System.out.println("[2] MP2 Assignment.");
		System.out.println("[3] MP3 Assignment.");
		System.out.println("[4] MP4 Assignment.");
		System.out.println("[5] Midterm Exam.");
		System.out.println("[6] Final Exam.");
		System.out.print("\n::: ");


		try {
			selection = in.nextInt();
		} catch (InputMismatchException e) {
			System.out.println(e);
		} finally {
			in.nextLine();
		}

		switch (selection) {
			case 1:
				selectionString = "mp1";
				break;
			case 2:
				selectionString = "mp2";
				break;
			case 3:
				selectionString = "mp3";
				break;
			case 4:
				selectionString = "mp4";
				break;
			case 5:
				selectionString = "midterm_exam";
				break;
			case 6:
				selectionString = "final_exam";
		}



		try (Connection conn = PowerSchool.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM assignment_grades INNER JOIN assignments ON assignment_grades.assignment_id = assignments.assignment_id WHERE student_id = ? AND assignment_grades.course_id = ?");
			stmt.setInt(1, this.getStudentId());
			stmt.setString(2, course_ids.get(input - 1));
			try (ResultSet rs = stmt.executeQuery()) {
				System.out.print("\n");
				int assignmentCount = 1;
				while (rs.next()) {
					System.out.printf("%d. %s / %d (out of %d pts)\n", assignmentCount, rs.getString("title"), rs.getInt("points_earned"), rs.getInt("points_possible"));
					assignmentCount++;
				}
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	public void updateMPGrade(int course_id, int mp) {
		double pointsEarned = 0;
		double pointsPossible = 0;
		int grade = 0;
		String columnLabel = "mp" + Integer.toString(mp);
		if (mp < 5 && mp > 0) {
			String statement = "SELECT * FROM assignment_grades INNER JOIN assignments ON assignments.assignment_id = assignment_grades.assignment_id WHERE student_id = ? AND assignments.course_id = ? AND marking_period = ?";
			try (Connection conn = PowerSchool.getConnection()) {
				PreparedStatement stmt = conn.prepareStatement(statement);
				stmt.setInt(1, this.getStudentId());
				stmt.setInt(2, course_id);
				stmt.setInt(3, mp);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						pointsEarned += rs.getInt("points_earned");
						pointsPossible += rs.getInt("points_possible");
					}
				}
			}catch (SQLException e) {
				System.out.println(e);
			}

			grade = (int) Math.round((pointsEarned / pointsPossible) * 100);

			try (Connection conn = PowerSchool.getConnection()) {
				String updateStatement = "UPDATE course_grades SET " + columnLabel + " = ? WHERE course_id = ? AND student_id = ?";
				PreparedStatement stmt = conn.prepareStatement(updateStatement);
				stmt.setInt(1, grade);
				stmt.setInt(2, course_id);
				stmt.setInt(3, this.studentId);
				stmt.executeUpdate();
			}catch (SQLException e) {
				System.out.println(e);
			}
		}else if (mp == 5) {
			String statement = "SELECT * FROM assignment_grades INNER JOIN assignments ON assignments.assignment_id = assignment_grades.assignment_id WHERE student_id = ? AND assignments.course_id = ? AND is_midterm = 1";
			try (Connection conn = PowerSchool.getConnection()) {
				PreparedStatement stmt = conn.prepareStatement(statement);
				stmt.setInt(1, this.studentId);
				stmt.setInt(2, course_id);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						pointsEarned = rs.getInt("points_earned");
						pointsPossible = rs.getInt("points_possible");
					}
				}
			}catch (SQLException e) {
				System.out.println(e);
			}

			grade = (int) Math.round((pointsEarned / pointsPossible) * 100);
			try (Connection conn = PowerSchool.getConnection()) {
				PreparedStatement stmt = conn.prepareStatement("UPDATE course_grades SET midterm_exam = ? WHERE course_id = ? AND student_id = ?");
				stmt.setInt(1, grade);
				stmt.setInt(2, course_id);
				stmt.setInt(3, this.studentId);
				stmt.executeUpdate();
			}catch (SQLException e) {
				System.out.println(e);
			}
		}else if (mp == 6) {
			String statement = "SELECT * FROM assignment_grades INNER JOIN assignments ON assignments.assignment_id = assignment_grades.assignment_id WHERE student_id = ? AND assignments.course_id = ? AND is_final = 1";
			try (Connection conn = PowerSchool.getConnection()) {
				PreparedStatement stmt = conn.prepareStatement(statement);
				stmt.setInt(1, this.studentId);
				stmt.setInt(2, course_id);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						pointsEarned = rs.getInt("points_earned");
						pointsPossible = rs.getInt("points_possible");
					}
				}
			}catch (SQLException e) {
				System.out.println(e);
			}

			grade = (int) Math.round((pointsEarned / pointsPossible) * 100);
			try (Connection conn = PowerSchool.getConnection()) {
				PreparedStatement stmt = conn.prepareStatement("UPDATE course_grades SET midterm_exam = ? WHERE course_id = ? AND student_id = ?");
				stmt.setInt(1, grade);
				stmt.setInt(2, course_id);
				stmt.setInt(3, this.studentId);
				stmt.executeUpdate();
			}catch (SQLException e) {
				System.out.println(e);
			}
		}

		this.updateGradeInCourse(course_id);
	}

	private void updateGradeInCourse(int course_id){
		Double[] grades = new Double[6]; 
		try (Connection conn = PowerSchool.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM course_grades WHERE student_id = ? AND course_id = ?");
			stmt.setInt(1, this.studentId);
			stmt.setInt(2, course_id);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					grades[0] = (double) rs.getInt("mp1");
					grades[1] = (double) rs.getInt("mp2");
					grades[2] = (double) rs.getInt("mp3");
					grades[3] = (double) rs.getInt("mp4");
					grades[4] = (double) rs.getInt("midterm_exam");
					grades[5] = (double) rs.getInt("final_exam");
				}
			}
		}catch (SQLException e) {
			System.out.println(e);
		}

		int course_grade = (int) Math.round(Utils.getGrade(grades));

		try (Connection conn = PowerSchool.getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("UPDATE course_grades SET grade = ? WHERE course_id = ? AND student_id = ?");
			stmt.setInt(1, course_grade);
			stmt.setInt(2, course_id);
			stmt.setInt(3, this.studentId);
			stmt.executeUpdate();
		}catch (SQLException e) {
			System.out.println(e);
		}


	}

}